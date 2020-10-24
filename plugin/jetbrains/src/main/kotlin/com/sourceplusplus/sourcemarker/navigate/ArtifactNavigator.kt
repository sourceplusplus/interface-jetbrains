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
import com.sourceplusplus.protocol.artifact.exception.JvmStackTraceElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ArtifactNavigator {

    //todo: remove method from method names and support navigating to classes?

    fun navigateTo(project: Project, element: JvmStackTraceElement) {
        ApplicationManager.getApplication().invokeLater {
            val foundFiles = getFilesByName(project, element.sourceAsFilename!!, allScope(project))
            if (foundFiles.isNotEmpty()) {
                val file = foundFiles[0]
                val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file)!!
                val offset = document.getLineStartOffset(element.sourceAsLineNumber!! - 1)
                PsiNavigationSupport.getInstance().createNavigatable(project, file.virtualFile, offset).navigate(true)
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
