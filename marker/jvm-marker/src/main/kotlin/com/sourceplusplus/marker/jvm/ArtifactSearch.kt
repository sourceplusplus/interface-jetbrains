package com.sourceplusplus.marker.jvm

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.sourceplusplus.marker.source.JVMMarkerUtils
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.marker.jvm.psi.EndpointDetector
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.plugins.groovy.GroovyFileType
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType
import org.slf4j.LoggerFactory
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
    fun detectRootPackage(project: Project): String? {
        var basePackages = JavaPsiFacade.getInstance(project).findPackage("")
            ?.getSubPackages(ProjectScope.getProjectScope(project))

        //remove non-code packages
        basePackages = basePackages!!.filter {
            val dirs = it.directories
            dirs.isNotEmpty() && !dirs[0].virtualFile.path.contains("/src/main/resources/")
        }.toTypedArray()
        basePackages = basePackages.filter {
            it.qualifiedName != "asciidoc" && it.qualifiedName != "lib"
        }.toTypedArray() //todo: probably shouldn't be necessary

        //determine deepest common source package
        if (basePackages.isNotEmpty()) {
            var rootPackage: String? = null
            while (basePackages!!.size == 1) {
                rootPackage = basePackages[0]!!.qualifiedName
                basePackages = basePackages[0]!!.getSubPackages(ProjectScope.getProjectScope(project))
            }
            if (rootPackage != null) {
                log.info("Detected root source package: $rootPackage")
                return rootPackage
            }
        }
        return null
    }

    @Suppress("UnstableApiUsage")
    suspend fun findArtifact(vertx: Vertx, artifact: ArtifactQualifiedName): PsiElement? {
        val promise = Promise.promise<Optional<PsiElement>>()
        val project = ProjectManager.getInstance().openProjects[0]

        ApplicationManager.getApplication().runReadAction {
            if (artifact.type == ArtifactType.CLASS) {
                val artifactQualifiedName = artifact.identifier
                val classQualifiedName = JVMMarkerUtils.getQualifiedClassName(artifactQualifiedName)
                val psiClass = JavaPsiFacade.getInstance(project).findClass(
                    classQualifiedName,
                    GlobalSearchScope.allScope(project)
                )
                promise.complete(Optional.ofNullable(psiClass))
            } else if (artifact.type == ArtifactType.METHOD) {
                val artifactQualifiedName = artifact.identifier
                val classQualifiedName = JVMMarkerUtils.getQualifiedClassName(artifactQualifiedName)
                val psiClass = JavaPsiFacade.getInstance(project).findClass(
                    classQualifiedName,
                    GlobalSearchScope.allScope(project)
                )
                for (theMethod in psiClass!!.methods) {
                    val uMethod = theMethod.toUElement() as UMethod
                    val qualifiedName = JVMMarkerUtils.getFullyQualifiedName(uMethod)
                    if (qualifiedName == artifactQualifiedName) {
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
}
