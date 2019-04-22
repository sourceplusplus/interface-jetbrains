package com.sourceplusplus.plugin.intellij.source.navigate

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.impl.PsiShortNamesCacheImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.PsiNavigateUtil
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.intellij.IntelliJStartupActivity
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJMethodGutterMark
import com.sourceplusplus.plugin.intellij.util.IntelliUtils
import com.sourceplusplus.plugin.source.navigate.ArtifactNavigator
import com.sourceplusplus.portal.coordinate.track.PortalViewTracker
import io.vertx.core.json.JsonObject
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastContextKt
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJArtifactNavigator extends ArtifactNavigator {

    private static final Logger log = LoggerFactory.getLogger(this.name)

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(CAN_NAVIGATE_TO_ARTIFACT, { message ->
            def artifactQualifiedName = message.body() as String
            ApplicationManager.getApplication().invokeLater({
                message.reply(canNavigateTo(artifactQualifiedName))
            })
        })
        vertx.eventBus().consumer(NAVIGATE_TO_ARTIFACT, { message ->
            def request = message.body() as JsonObject
            def artifactQualifiedName = request.getString("artifact_qualified_name") //todo: remove, get from portal
            ApplicationManager.getApplication().invokeLater({
                IntelliJMethodGutterMark.closePortalIfOpen()
                navigateTo(artifactQualifiedName)

                def sourceMark = PluginBootstrap.getSourcePlugin().getSourceMark(artifactQualifiedName) as IntelliJMethodGutterMark
                if (sourceMark != null) {
                    handleMark(sourceMark)
                } else {
                    //todo: smarter
                    vertx.setPeriodic(1000, {
                        sourceMark = PluginBootstrap.getSourcePlugin().getSourceMark(artifactQualifiedName) as IntelliJMethodGutterMark
                        if (sourceMark != null) {
                            vertx.cancelTimer(it)
                            message.reply(true)
                            handleMark(sourceMark)
                        }
                    })
                }
            })
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private void handleMark(IntelliJMethodGutterMark mark) {
        mark.artifactDataAvailable = true
        mark.sourceFileMarker.refresh()

        ApplicationManager.getApplication().invokeLater({
            ApplicationManager.getApplication().runReadAction({
                def editor = FileEditorManager.getInstance(IntelliJStartupActivity.currentProject).getSelectedTextEditor()
                mark.displayPortal(vertx, editor, false)
            })
        })
    }

    static void navigateTo(String artifactQualifiedName) {
        def classQualifiedName = getQualifiedClassName(artifactQualifiedName)
        def psiCache = PsiShortNamesCacheImpl.getInstance(IntelliJStartupActivity.currentProject)
        def scope = GlobalSearchScope.allScope(IntelliJStartupActivity.currentProject)
        def className = classQualifiedName.substring(classQualifiedName.lastIndexOf(".") + 1)
        def theClasses = psiCache.getClassesByName(className, scope)

        def foundArtifact = null
        for (theClass in theClasses) {
            for (theMethod in theClass.methods) {
                def uMethod = UastContextKt.toUElement(theMethod) as UMethod
                def qualifiedName = IntelliUtils.getArtifactQualifiedName(uMethod)
                if (qualifiedName == artifactQualifiedName) {
                    foundArtifact = theMethod
                    break
                }
            }

            if (foundArtifact != null) {
                break
            }
        }
        PsiNavigateUtil.navigate(foundArtifact)
    }

    static boolean canNavigateTo(String artifactQualifiedName) {
        def classQualifiedName
        try {
            classQualifiedName = getQualifiedClassName(artifactQualifiedName)
        } catch (all) {
            return false
        }

        def psiCache = PsiShortNamesCacheImpl.getInstance(IntelliJStartupActivity.currentProject)
        def scope = GlobalSearchScope.allScope(IntelliJStartupActivity.currentProject)
        def className = classQualifiedName.substring(classQualifiedName.lastIndexOf(".") + 1)
        def theClasses = psiCache.getClassesByName(className, scope)

        def foundArtifact = null
        for (theClass in theClasses) {
            for (theMethod in theClass.methods) {
                def uMethod = UastContextKt.toUElement(theMethod) as UMethod
                def qualifiedName = IntelliUtils.getArtifactQualifiedName(uMethod)
                if (qualifiedName == artifactQualifiedName) {
                    foundArtifact = theMethod
                    break
                }
            }

            if (foundArtifact != null) {
                break
            }
        }
        return foundArtifact != null
    }

    static String getQualifiedClassName(String qualifiedName) {
        def withoutArgs = qualifiedName.substring(0, qualifiedName.indexOf("("))
        if (withoutArgs.contains("<")) {
            withoutArgs = withoutArgs.substring(0, withoutArgs.indexOf("<"))
            return withoutArgs.substring(withoutArgs.lastIndexOf("?") + 1, withoutArgs.lastIndexOf("."))
        } else {
            return withoutArgs.substring(withoutArgs.lastIndexOf("?") + 1, withoutArgs.lastIndexOf("."))
        }
    }
}
