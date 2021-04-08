package com.sourceplusplus.sourcemarker.navigate

import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex.getFilesByName
import com.intellij.psi.search.GlobalSearchScope.allScope
import com.intellij.util.PsiNavigateUtil
import com.sourceplusplus.marker.source.SourceMarkerUtils
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.artifact.exception.JvmStackTraceElement
import com.sourceplusplus.protocol.artifact.exception.sourceAsFilename
import com.sourceplusplus.protocol.artifact.exception.sourceAsLineNumber
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin.vertx
import com.sourceplusplus.sourcemarker.search.ArtifactSearch
import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ArtifactNavigator {

    private val log = LoggerFactory.getLogger(ArtifactNavigator::class.java)

    //todo: remove method from method names and support navigating to classes?

    fun navigateTo(project: Project, element: JvmStackTraceElement) {
        ApplicationManager.getApplication().invokeLater {
            val foundFiles = getFilesByName(project, element.sourceAsFilename()!!, allScope(project))
            if (foundFiles.isNotEmpty()) {
                val file = foundFiles[0]
                val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file)!!
                val offset = document.getLineStartOffset(element.sourceAsLineNumber()!! - 1)
                PsiNavigationSupport.getInstance().createNavigatable(project, file.virtualFile, offset).navigate(true)
            }
        }
    }

    fun navigateTo(project: Project, artifactQualifiedName: ArtifactQualifiedName) {
        if (artifactQualifiedName.type == ArtifactType.ENDPOINT) {
            GlobalScope.launch(vertx.dispatcher()) {
                val artifactPsi = ArtifactSearch.findArtifact(artifactQualifiedName)
                if (artifactPsi != null) {
                    ApplicationManager.getApplication().invokeLater {
                        PsiNavigateUtil.navigate(artifactPsi)
                    }
                } else {
                    log.warn("Could not find artifact: {}", artifactQualifiedName)
                }
            }
        } else {
            ApplicationManager.getApplication().invokeLater {
                navigateToMethod(project, artifactQualifiedName.identifier)
            }
        }
    }

    fun navigateToMethod(project: Project, artifactQualifiedName: String): PsiElement {
        val classQualifiedName = SourceMarkerUtils.getQualifiedClassName(artifactQualifiedName)
        val psiClass = JavaPsiFacade.getInstance(project).findClass(classQualifiedName, allScope(project))
        for (theMethod in psiClass!!.methods) {
            val uMethod = theMethod.toUElement() as UMethod
            val qualifiedName = SourceMarkerUtils.getFullyQualifiedName(uMethod)
            if (qualifiedName == artifactQualifiedName) {
                PsiNavigateUtil.navigate(theMethod)
                return SourceMarkerUtils.getNameIdentifier(theMethod)!!
            }
        }
        throw IllegalArgumentException("Failed to find: $artifactQualifiedName")
    }

    suspend fun canNavigateTo(project: Project, artifactQualifiedName: ArtifactQualifiedName): Boolean {
        val promise = Promise.promise<Boolean>()
        ApplicationManager.getApplication().invokeLater {
            promise.complete(canNavigateToMethod(project, artifactQualifiedName.identifier))
        }
        return promise.future().await()
    }

    fun canNavigateToMethod(project: Project, artifactQualifiedName: String): Boolean {
        val classQualifiedName = SourceMarkerUtils.getQualifiedClassName(artifactQualifiedName)
        val psiClass = JavaPsiFacade.getInstance(project).findClass(classQualifiedName, allScope(project))
        for (theMethod in psiClass!!.methods) {
            val uMethod = theMethod.toUElement() as UMethod
            val qualifiedName = SourceMarkerUtils.getFullyQualifiedName(uMethod)
            if (qualifiedName == artifactQualifiedName) {
                return true
            }
        }
        return false
    }
}
