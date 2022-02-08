/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.marker.jvm

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.plugins.groovy.GroovyFileType
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.jvm.psi.EndpointDetector
import spp.jetbrains.marker.source.JVMMarkerUtils
import spp.protocol.artifact.ArtifactNameUtils
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import java.util.*

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ArtifactSearch {

    private val log = LoggerFactory.getLogger(ArtifactSearch::class.java)

    @JvmStatic
    suspend fun detectRootPackage(project: Project): String? {
        var basePackages = withContext(Dispatchers.Default) {
            ApplicationManager.getApplication().runReadAction(Computable<Array<PsiPackage>> {
                val foundPackage = JavaPsiFacade.getInstance(project).findPackage("")
                if (foundPackage != null) {
                    foundPackage.getSubPackages(ProjectScope.getProjectScope(project))
                } else {
                    emptyArray()
                }
            })
        }

        //remove non-code packages
        basePackages = basePackages.filter {
            val dirs = it.directories
            dirs.isNotEmpty() && !dirs[0].virtualFile.path.contains("/src/main/resources/")
        }.toTypedArray()
        basePackages = basePackages.filter {
            it.qualifiedName != "asciidoc" && it.qualifiedName != "lib" && it.qualifiedName != "META-INF"
        }.toTypedArray() //todo: probably shouldn't be necessary

        //determine deepest common source package
        if (basePackages.isNotEmpty()) {
            var rootPackage: String? = null
            while (basePackages.size == 1) {
                rootPackage = basePackages[0].qualifiedName
                basePackages = withContext(Dispatchers.Default) {
                    ApplicationManager.getApplication().runReadAction(Computable<Array<PsiPackage>> {
                        basePackages[0].getSubPackages(ProjectScope.getProjectScope(project))
                    })
                }
            }
            if (rootPackage != null) {
                return rootPackage
            }
        }

        //look explicitly for /src/main/ pattern
        var srcMains = basePackages.flatMap { it.directories.toList() }
            .filter { it.toString().contains("/src/main/") }
            .filterNot { it.toString().contains("/META-INF") }
        val genPackage = StringBuilder()
        var commonSuffix = commonSuffix(srcMains.map { it.toString() }).replace("/", "")
        while (commonSuffix.isNotEmpty()) {
            genPackage.append(commonSuffix)

            srcMains = srcMains.flatMap { it.subdirectories.toList() }
            commonSuffix = commonSuffix(srcMains.map { it.toString() }).replace("/", "")
            if (commonSuffix.isNotEmpty()) {
                genPackage.append(".")
            }
        }
        if (genPackage.isNotEmpty()) {
            return genPackage.toString()
        }

        return null
    }

    @Suppress("UnstableApiUsage")
    suspend fun findArtifact(vertx: Vertx, artifact: ArtifactQualifiedName): PsiElement? {
        val promise = Promise.promise<Optional<PsiElement>>()
        val project = ProjectManager.getInstance().openProjects[0]

        ApplicationManager.getApplication().runReadAction {
            if (artifact.type == ArtifactType.CLASS) {
                val psiClass = JavaPsiFacade.getInstance(project).findClass(
                    artifact.identifier, GlobalSearchScope.allScope(project)
                )
                promise.complete(Optional.ofNullable(psiClass))
            } else if (artifact.type == ArtifactType.METHOD) {
                val psiClass = JavaPsiFacade.getInstance(project).findClass(
                    ArtifactNameUtils.getQualifiedClassName(artifact.identifier)!!,
                    GlobalSearchScope.allScope(project)
                )
                for (theMethod in psiClass!!.methods) {
                    val uMethod = theMethod.toUElement() as UMethod
                    val qualifiedName = JVMMarkerUtils.getFullyQualifiedName(uMethod)
                    if (qualifiedName == artifact) {
                        promise.complete(Optional.of(theMethod))
                    }
                }
                promise.tryComplete(Optional.empty())
            } else {
                //todo: should be saving endpoints somewhere so don't always have to scan entire application
                val groovySourceFiles = FileTypeIndex.getFiles(
                    GroovyFileType.GROOVY_FILE_TYPE, GlobalSearchScope.projectScope(project)
                )
                val javaSourceFiles = FileTypeIndex.getFiles(
                    JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project)
                )
                val kotlinSourceFiles = FileTypeIndex.getFiles(
                    KotlinFileType.INSTANCE, GlobalSearchScope.projectScope(project)
                )

                val endpointDetector = EndpointDetector(vertx)
                var keepSearching = true
                for (virtualFile in groovySourceFiles.union(javaSourceFiles).union(kotlinSourceFiles)) {
                    val file = PsiManager.getInstance(project).findFile(virtualFile)!!
                    file.accept(object : PsiRecursiveElementVisitor(true) {
                        override fun visitElement(element: PsiElement) {
                            if (element is PsiMethod) {
                                runBlocking {
                                    val endpointName =
                                        endpointDetector.determineEndpointName(element.toUElementOfType()!!).await()
                                    if (endpointName.isPresent && endpointName.get().name == artifact.identifier) {
                                        promise.complete(Optional.of(element))
                                        keepSearching = false
                                    } else {
                                        super.visitElement(element)
                                    }
                                }
                            } else {
                                super.visitElement(element)
                            }
                        }
                    })

                    if (!keepSearching) break
                }
                promise.tryComplete(Optional.empty())
            }
        }
        return promise.future().await().orElse(null)
    }

    //taken from: https://rosettacode.org/wiki/Longest_common_suffix#Kotlin
    private fun commonSuffix(a: List<String>): String {
        val le = a.size
        if (le == 0) {
            return ""
        }
        if (le == 1) {
            return a[0]
        }
        val le0 = a[0].length
        var minLen = le0
        for (i in 1 until le) {
            if (a[i].length < minLen) {
                minLen = a[i].length
            }
        }
        if (minLen == 0) {
            return ""
        }
        var res = ""
        val a1 = a.subList(1, a.size)
        for (i in 1..minLen) {
            val suffix = a[0].substring(le0 - i)
            for (e in a1) {
                if (!e.endsWith(suffix)) {
                    return res
                }
            }
            res = suffix
        }
        return ""
    }
}
