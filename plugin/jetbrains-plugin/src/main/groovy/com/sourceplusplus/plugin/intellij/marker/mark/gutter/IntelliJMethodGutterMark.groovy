package com.sourceplusplus.plugin.intellij.marker.mark.gutter

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiLiteral
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.marker.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.gutter.MethodGutterMark
import com.sourceplusplus.marker.source.mark.gutter.component.jcef.GutterMarkJcefComponent
import com.sourceplusplus.marker.source.mark.gutter.event.GutterMarkEventCode
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJKeys
import com.sourceplusplus.plugin.intellij.portal.IntelliJPortalUI
import com.sourceplusplus.plugin.intellij.portal.IntelliJSourcePortal
import com.sourceplusplus.plugin.source.model.SourceMethodAnnotation
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.display.PortalTab
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import org.jetbrains.annotations.NotNull
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.java.JavaUAnnotation

import java.time.Instant

import static com.sourceplusplus.plugin.coordinate.artifact.track.PluginArtifactSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT

/**
 * todo: description
 *
 * @version 0.2.5
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
//@Slf4j //todo: can't override log
class IntelliJMethodGutterMark extends MethodGutterMark implements IntelliJGutterMark, SourceMarkEventListener {

    IntelliJMethodGutterMark(SourceFileMarker sourceFileMarker, UMethod psiMethod) {
        super(sourceFileMarker, psiMethod)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void apply() {
        super.apply()

        PluginBootstrap.sourcePlugin.vertx.eventBus().publish(SOURCE_MARK_APPLIED, this)
        addEventListener(this)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void handleEvent(@NotNull SourceMarkEvent event) {
        if (event.eventCode == GutterMarkEventCode.GUTTER_MARK_VISIBLE) {
            (event.sourceMark as IntelliJGutterMark).registerPortal()
        } else if (event.eventCode == SourceMarkEventCode.MARK_REMOVED) {
            if (portalRegistered) {
                SourcePortal.getPortal(portalUuid).close()
            }

            def unsubscribeRequest = SourceArtifactUnsubscribeRequest.builder()
                    .appUuid(SourcePluginConfig.current.activeEnvironment.appUuid)
                    .artifactQualifiedName(artifactQualifiedName)
                    .removeAllArtifactSubscriptions(true)
                    .build()
            PluginBootstrap.sourcePlugin.vertx.eventBus().send(UNSUBSCRIBE_FROM_ARTIFACT, unsubscribeRequest)
        } else if (event.eventCode == SourceMarkEventCode.NAME_CHANGED) {
            //todo: this
        }
    }

    void getMethodAnnotations(Handler<AsyncResult<List<SourceMethodAnnotation>>> handler) {
        ReadAction.run({
            def annotations = new ArrayList<SourceMethodAnnotation>()
            psiMethod.getAnnotations().each {
                def qualifiedName = it.qualifiedName
                if (qualifiedName) {
                    if (it instanceof JavaUAnnotation) {
                        def attributeMap = new HashMap<String, Object>()
                        it.attributeValues.each {
                            if (it.name) {
                                attributeMap.put(it.name, (it.expression as ULiteralExpression).value)
                            } else {
                                log.warn("Unknown annotation expression: " + it)
                            }
                        }
                        annotations.add(new SourceMethodAnnotation(qualifiedName, attributeMap))
                    } else {
                        def attributeMap = new HashMap<String, Object>()
                        it.attributes.each {
                            if (it.attributeValue.sourceElement instanceof PsiLiteral) {
                                attributeMap.put(it.attributeName, (it.attributeValue.sourceElement as PsiLiteral).value)
                            } else {
                                attributeMap.put(it.attributeName, it.attributeValue.sourceElement.toString())
                            }
                        }
                        annotations.add(new SourceMethodAnnotation(qualifiedName, attributeMap))
                    }
                }
            }
            handler.handle(Future.succeededFuture(annotations))
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void markArtifactSubscribed() {
        if (!artifactSubscribed) {
            if (SourcePluginConfig.current.methodGutterMarksEnabled) {
                configuration.icon = artifactDataAvailable ? sppActive : sppInactive
            } else {
                configuration.icon = null
            }

            putUserData(IntelliJKeys.ArtifactSubscribed, true)
            putUserData(IntelliJKeys.ArtifactSubscribeTime, Instant.now())
            sourceFileMarker.refresh()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void markArtifactUnsubscribed() {
        if (artifactSubscribed) {
            if (SourcePluginConfig.current.methodGutterMarksEnabled) {
                configuration.icon = artifactDataAvailable ? sppActive : null
            } else {
                configuration.icon = null
            }

            putUserData(IntelliJKeys.ArtifactSubscribed, false)
            putUserData(IntelliJKeys.ArtifactUnsubscribeTime, Instant.now())
            sourceFileMarker.refresh()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void markArtifactDataAvailable() {
        if (!artifactDataAvailable) {
            if (SourcePluginConfig.current.methodGutterMarksEnabled) {
                configuration.icon = sppActive
            } else {
                configuration.icon = null
            }

            putUserData(IntelliJKeys.ArtifactDataAvailable, true)
            sourceFileMarker.refresh()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean isArtifactSubscribed() {
        return getUserData(IntelliJKeys.ArtifactSubscribed)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean isArtifactDataAvailable() {
        return getUserData(IntelliJKeys.ArtifactDataAvailable)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean isPortalRegistered() {
        return getUserData(IntelliJKeys.PortalUUID) != null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getPortalUuid() {
        return getUserData(IntelliJKeys.PortalUUID)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void registerPortal() {
        registerPortal(PortalTab.Overview)
    }

    void registerPortal(PortalTab initialTab) {
        if (!portalRegistered) {
            def appUuid = SourcePluginConfig.current.activeEnvironment.appUuid
            def existingPortal = IntelliJSourcePortal.getInternalPortal(appUuid, artifactQualifiedName)
            if (existingPortal.present) {
                putUserData(IntelliJKeys.PortalUUID, existingPortal.get().portalUuid)
            } else {
                def portalUuid = UUID.randomUUID().toString()
                putUserData(IntelliJKeys.PortalUUID, portalUuid)
            }

            def markComponent = gutterMarkComponent as GutterMarkJcefComponent
            markComponent.configuration.initialUrl = IntelliJPortalUI.getPortalUrl(initialTab, portalUuid)
            markComponent.initialize()

            if (existingPortal.present) {
                existingPortal.get().portalUI.lateInitBrowser(markComponent.browser)
            } else {
                IntelliJSourcePortal.register(appUuid, portalUuid, artifactQualifiedName,
                        new IntelliJPortalUI(portalUuid, markComponent.browser))
            }
        }
    }
}
