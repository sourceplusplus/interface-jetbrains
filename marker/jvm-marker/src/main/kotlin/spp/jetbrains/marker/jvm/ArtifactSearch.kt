/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.marker.jvm

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType
import spp.jetbrains.ScopeExtensions.safeRunBlocking
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

    @Suppress("UnstableApiUsage")
    suspend fun findArtifact(vertx: Vertx, artifact: ArtifactQualifiedName): PsiElement? {
        val promise = Promise.promise<Optional<PsiElement>>()
        val project = ProjectManager.getInstance().openProjects[0] //todo: support multiple projects

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
//                val groovySourceFiles = FileTypeIndex.getFiles(
//                    GroovyFileType.GROOVY_FILE_TYPE, GlobalSearchScope.projectScope(project)
//                )
                //todo: can't search here, need language specific search then combine
                val javaSourceFiles = FileTypeIndex.getFiles(
                    JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project)
                )
//                val kotlinSourceFiles = FileTypeIndex.getFiles(
//                    KotlinFileType.INSTANCE, GlobalSearchScope.projectScope(project)
//                )

                val endpointDetector = JVMEndpointDetector(project) //todo: python
                var keepSearching = true
//                for (virtualFile in groovySourceFiles.union(javaSourceFiles).union(kotlinSourceFiles)) {
                for (virtualFile in javaSourceFiles) {
                    val file = PsiManager.getInstance(project).findFile(virtualFile)!!
                    file.accept(object : PsiRecursiveElementVisitor(true) {
                        override fun visitElement(element: PsiElement) {
                            if (element is PsiMethod) {
                                safeRunBlocking {
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
