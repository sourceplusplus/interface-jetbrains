package com.sourceplusplus.marker.plugin

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.CENTER
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.source.SourceMarkerUtils
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.marker.source.mark.gutter.ClassGutterMark
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import com.sourceplusplus.marker.source.mark.gutter.MethodGutterMark
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

/**
 * Used to associate [GutterMark]s with IntelliJ PSI elements.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class SourceLineMarkerProvider : LineMarkerProviderDescriptor() {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        if (!SourceMarker.enabled) {
            return null
        }

        val parent = element.parent
        if (parent is PsiClass && element === parent.nameIdentifier) {
            //class gutter marks
            val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile) ?: return null
            val artifactQualifiedName = SourceMarkerUtils.getFullyQualifiedName(element.parent.toUElement() as UClass)
            if (!SourceMarker.configuration.createSourceMarkFilter.test(artifactQualifiedName)) return null

            //check by artifact name first due to user can erroneously name same class twice
            var gutterMark = fileMarker.getSourceMark(artifactQualifiedName, SourceMark.Type.GUTTER) as ClassGutterMark?
            if (gutterMark == null) {
                gutterMark = SourceMarkerUtils.getOrCreateClassGutterMark(fileMarker, element) ?: return null
            } else if (!gutterMark.isVisible()) {
                return null
            }

            var navigationHandler: GutterIconNavigationHandler<PsiElement>? = null
            if (gutterMark.configuration.activateOnMouseClick) {
                navigationHandler = GutterIconNavigationHandler { _, elt ->
                    elt!!.getUserData(SourceKey.GutterMark)!!.displayPopup()
                }
            }
            return LineMarkerInfo(
                getFirstLeaf(element),
                element.textRange,
                gutterMark.configuration.icon,
                null,
                navigationHandler,
                CENTER
            )
        } else if ((parent is PsiMethod && element === parent.nameIdentifier)
            || (parent is GrMethod && element === parent.nameIdentifierGroovy)
            || (parent is KtNamedFunction && element === parent.nameIdentifier)
        ) {
            //method gutter marks
            val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile) ?: return null
            val artifactQualifiedName = SourceMarkerUtils.getFullyQualifiedName(element.parent.toUElement() as UMethod)
            if (!SourceMarker.configuration.createSourceMarkFilter.test(artifactQualifiedName)) return null

            //check by artifact name first due to user can erroneously name same method twice
            var gutterMark = fileMarker.getSourceMark(
                artifactQualifiedName,
                SourceMark.Type.GUTTER
            ) as MethodGutterMark?
            if (gutterMark == null) {
                gutterMark = SourceMarkerUtils.getOrCreateMethodGutterMark(fileMarker, element) ?: return null
            } else if (!gutterMark.isVisible()) {
                return null
            }

            var navigationHandler: GutterIconNavigationHandler<PsiElement>? = null
            if (gutterMark.configuration.activateOnMouseClick) {
                navigationHandler = GutterIconNavigationHandler { _, elt ->
                    elt!!.getUserData(SourceKey.GutterMark)!!.displayPopup()
                }
            }
            return LineMarkerInfo(
                getFirstLeaf(element),
                element.textRange,
                gutterMark.configuration.icon,
                null,
                navigationHandler,
                CENTER
            )
        } else {
            //expression gutter marks
            //todo: only works for manually created expression gutter marks atm
            if (element.getUserData(SourceKey.GutterMark) != null) {
                val gutterMark = element.getUserData(SourceKey.GutterMark)!!
                if (!gutterMark.isVisible()) {
                    return null
                }

                var navigationHandler: GutterIconNavigationHandler<PsiElement>? = null
                if (gutterMark.configuration.activateOnMouseClick) {
                    navigationHandler = GutterIconNavigationHandler { _, elt ->
                        elt!!.getUserData(SourceKey.GutterMark)!!.displayPopup()
                    }
                }
                return LineMarkerInfo(
                    getFirstLeaf(element),
                    element.textRange,
                    gutterMark.configuration.icon,
                    null,
                    navigationHandler,
                    CENTER
                )
            }
        }

        return null
    }

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (!SourceMarker.enabled) {
            return
        }

        elements.stream().map { it.containingFile }.distinct().forEach {
            SourceMarker.getSourceFileMarker(it)?.removeInvalidSourceMarks()
        }
    }

    private fun getFirstLeaf(element: PsiElement): PsiElement {
        var e = element
        while (e.children.isNotEmpty()) {
            e = e.firstChild
        }
        return e
    }

    /**
     * Associates Groovy [GutterMark]s to PSI elements.
     *
     * @since 0.1.0
     */
    class GroovyDescriptor : SourceLineMarkerProvider() {
        override fun getName(): String {
            return "Groovy source line markers"
        }
    }

    /**
     * Associates Java [GutterMark]s to PSI elements.
     *
     * @since 0.1.0
     */
    class JavaDescriptor : SourceLineMarkerProvider() {
        override fun getName(): String {
            return "Java source line markers"
        }
    }

    /**
     * Associates Kotlin [GutterMark]s to PSI elements.
     *
     * @since 0.1.0
     */
    class KotlinDescriptor : SourceLineMarkerProvider() {
        override fun getName(): String {
            return "Kotlin source line markers"
        }
    }
}
