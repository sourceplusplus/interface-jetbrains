package com.sourceplusplus.sourcemarker.search

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.sourceplusplus.marker.source.SourceMarkerUtils
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.sourcemarker.psi.EndpointDetector
import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.plugins.groovy.GroovyFileType
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType
import java.util.*

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ArtifactSearch {

    @Suppress("UnstableApiUsage")
    suspend fun findArtifact(artifact: ArtifactQualifiedName): PsiElement? {
        val promise = Promise.promise<Optional<PsiElement>>()
        val project = ProjectManager.getInstance().openProjects[0]

        ApplicationManager.getApplication().runReadAction {
            if (artifact.type == ArtifactType.CLASS) {
                val artifactQualifiedName = artifact.identifier
                val classQualifiedName = SourceMarkerUtils.getQualifiedClassName(artifactQualifiedName)
                val psiClass = JavaPsiFacade.getInstance(project).findClass(
                    classQualifiedName,
                    GlobalSearchScope.allScope(project)
                )
                promise.complete(Optional.ofNullable(psiClass))
            } else if (artifact.type == ArtifactType.METHOD) {
                val artifactQualifiedName = artifact.identifier
                val classQualifiedName = SourceMarkerUtils.getQualifiedClassName(artifactQualifiedName)
                val psiClass = JavaPsiFacade.getInstance(project).findClass(
                    classQualifiedName,
                    GlobalSearchScope.allScope(project)
                )
                for (theMethod in psiClass!!.methods) {
                    val uMethod = theMethod.toUElement() as UMethod
                    val qualifiedName = SourceMarkerUtils.getFullyQualifiedName(uMethod)
                    if (qualifiedName == artifactQualifiedName) {
                        promise.complete(Optional.of(theMethod))
                    }
                }
                promise.tryComplete(Optional.empty())
            } else {
                //todo: should be saving endpoints somewhere so don't always have to scan entire application
                val groovySourceFiles = FileBasedIndex.getInstance().getContainingFiles(
                    FileTypeIndex.NAME, GroovyFileType.GROOVY_FILE_TYPE,
                    GlobalSearchScope.projectScope(project)
                )
                val javaSourceFiles = FileBasedIndex.getInstance().getContainingFiles(
                    FileTypeIndex.NAME, JavaFileType.INSTANCE,
                    GlobalSearchScope.projectScope(project)
                )
                val kotlinSourceFiles = FileBasedIndex.getInstance().getContainingFiles(
                    FileTypeIndex.NAME, KotlinFileType.INSTANCE,
                    GlobalSearchScope.projectScope(project)
                )

                val endpointDetector = EndpointDetector()
                var keepSearching = true
                for (virtualFile in groovySourceFiles.union(javaSourceFiles).union(kotlinSourceFiles)) {
                    val file = PsiManager.getInstance(project).findFile(virtualFile)!!
                    file.accept(object : PsiRecursiveElementVisitor(true) {
                        override fun visitElement(element: PsiElement) {
                            if (element is PsiMethod) {
                                runBlocking {
                                    val endpointName =
                                        endpointDetector.determineEndpointName(element.toUElementOfType()!!).await()
                                    if (endpointName.isPresent && endpointName.get() == artifact.identifier) {
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
