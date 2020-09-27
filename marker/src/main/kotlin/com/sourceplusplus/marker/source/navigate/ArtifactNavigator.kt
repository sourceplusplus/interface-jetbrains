package com.sourceplusplus.marker.source.navigate

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.PsiNavigateUtil
import com.sourceplusplus.marker.MarkerUtils
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ArtifactNavigator {

    //todo: remove method from method names and support navigating to classes?

    fun navigateToMethod(project: Project, artifactQualifiedName: String): PsiElement {
        val classQualifiedName = MarkerUtils.getQualifiedClassName(artifactQualifiedName)
        val psiClass = JavaPsiFacade.getInstance(project).findClass(
            classQualifiedName,
            GlobalSearchScope.allScope(project)
        )
        for (theMethod in psiClass!!.methods) {
            val uMethod = theMethod.toUElement() as UMethod
            val qualifiedName = MarkerUtils.getFullyQualifiedName(uMethod)
            if (qualifiedName == artifactQualifiedName) {
                PsiNavigateUtil.navigate(theMethod)
                return MarkerUtils.getNameIdentifier(theMethod)!!
            }
        }
        throw IllegalArgumentException("Failed to find: $artifactQualifiedName")
    }

    fun canNavigateToMethod(project: Project, artifactQualifiedName: String): Boolean {
        val classQualifiedName = MarkerUtils.getQualifiedClassName(artifactQualifiedName)
        val psiClass = JavaPsiFacade.getInstance(project).findClass(
            classQualifiedName,
            GlobalSearchScope.allScope(project)
        )
        for (theMethod in psiClass!!.methods) {
            val uMethod = theMethod.toUElement() as UMethod
            val qualifiedName = MarkerUtils.getFullyQualifiedName(uMethod)
            if (qualifiedName == artifactQualifiedName) {
                return true
            }
        }
        return false
    }
}
