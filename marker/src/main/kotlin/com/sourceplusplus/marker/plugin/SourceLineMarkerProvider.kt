package com.sourceplusplus.marker.plugin

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.CENTER
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.sourceplusplus.marker.MarkerUtils
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
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class SourceLineMarkerProvider : LineMarkerProviderDescriptor() {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        if (!SourceMarkerPlugin.enabled) {
            return null
        }

        val parent = element.parent
        if (parent is PsiClass && element === parent.nameIdentifier) {
            //class gutter marks
            val fileMarker = SourceMarkerPlugin.getSourceFileMarker(element.containingFile) ?: return null
            val artifactQualifiedName = MarkerUtils.getFullyQualifiedName(element.parent.toUElement() as UClass)
            if (!SourceMarkerPlugin.configuration.createSourceMarkFilter.test(artifactQualifiedName)) return null

            //check by artifact name first due to user can erroneously name same class twice
            var gutterMark = fileMarker.getSourceMark(artifactQualifiedName, SourceMark.Type.GUTTER) as ClassGutterMark?
            if (gutterMark == null) {
                gutterMark = MarkerUtils.getOrCreateClassGutterMark(fileMarker, element) ?: return null
            }

            var navigationHandler: GutterIconNavigationHandler<PsiElement>? = null
            if (gutterMark.configuration.activateOnMouseClick) {
                navigationHandler = GutterIconNavigationHandler { _, elt ->
                    elt!!.getUserData(SourceKey.GutterMark)!!.displayPopup()
                }
            }
            return LineMarkerInfo(
                element,
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
            val fileMarker = SourceMarkerPlugin.getSourceFileMarker(element.containingFile) ?: return null
            val artifactQualifiedName = MarkerUtils.getFullyQualifiedName(element.parent.toUElement() as UMethod)
            if (!SourceMarkerPlugin.configuration.createSourceMarkFilter.test(artifactQualifiedName)) return null

            //check by artifact name first due to user can erroneously name same method twice
            var gutterMark = fileMarker.getSourceMark(
                artifactQualifiedName,
                SourceMark.Type.GUTTER
            ) as MethodGutterMark?
            if (gutterMark == null) {
                gutterMark = MarkerUtils.getOrCreateMethodGutterMark(fileMarker, element) ?: return null
            }

            var navigationHandler: GutterIconNavigationHandler<PsiElement>? = null
            if (gutterMark.configuration.activateOnMouseClick) {
                navigationHandler = GutterIconNavigationHandler { _, elt ->
                    elt!!.getUserData(SourceKey.GutterMark)!!.displayPopup()
                }
            }
            return LineMarkerInfo(
                element,
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
                var navigationHandler: GutterIconNavigationHandler<PsiElement>? = null
                if (gutterMark.configuration.activateOnMouseClick) {
                    navigationHandler = GutterIconNavigationHandler { _, elt ->
                        elt!!.getUserData(SourceKey.GutterMark)!!.displayPopup()
                    }
                }
                return LineMarkerInfo(
                    element,
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
        if (!SourceMarkerPlugin.enabled) {
            return
        }

        elements.stream().map { it.containingFile }.distinct().forEach {
            SourceMarkerPlugin.getSourceFileMarker(it)?.removeInvalidSourceMarks()
        }
    }

    /**
     * Associates Groovy [GutterMark]s to PSI elements.
     *
     * @since 0.0.1
     */
    class GroovyDescriptor : SourceLineMarkerProvider() {
        override fun getName(): String {
            return "Groovy source line markers"
        }
    }

    /**
     * Associates Java [GutterMark]s to PSI elements.
     *
     * @since 0.0.1
     */
    class JavaDescriptor : SourceLineMarkerProvider() {
        override fun getName(): String {
            return "Java source line markers"
        }
    }

    /**
     * Associates Kotlin [GutterMark]s to PSI elements.
     *
     * @since 0.0.1
     */
    class KotlinDescriptor : SourceLineMarkerProvider() {
        override fun getName(): String {
            return "Kotlin source line markers"
        }
    }
}
