package com.sourceplusplus.plugin.intellij.marker.mark.gutter

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiLiteral
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJKeys
import com.sourceplusplus.plugin.source.model.SourceMethodAnnotation
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.display.PortalInterface
import com.sourceplusplus.portal.display.PortalTab
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import org.jetbrains.annotations.NotNull
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.java.JavaUAnnotation
import plus.sourceplus.marker.SourceFileMarker
import plus.sourceplus.marker.source.mark.api.event.SourceMarkEvent
import plus.sourceplus.marker.source.mark.api.event.SourceMarkEventCode
import plus.sourceplus.marker.source.mark.api.event.SourceMarkEventListener
import plus.sourceplus.marker.source.mark.gutter.MethodGutterMark
import plus.sourceplus.marker.source.mark.gutter.component.jcef.GutterMarkJcefComponent

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
        addEventListener(this)
        super.apply()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void handleEvent(@NotNull SourceMarkEvent sourceMarkEvent) {
        if (sourceMarkEvent.eventCode == SourceMarkEventCode.MARK_REMOVED) {
            def unsubscribeRequest = SourceArtifactUnsubscribeRequest.builder()
                    .appUuid(SourcePluginConfig.current.activeEnvironment.appUuid)
                    .artifactQualifiedName(artifactQualifiedName)
                    .removeAllArtifactSubscriptions(true)
                    .build()
            PluginBootstrap.sourcePlugin.vertx.eventBus().send(UNSUBSCRIBE_FROM_ARTIFACT, unsubscribeRequest)
        } else  if (sourceMarkEvent.eventCode == SourceMarkEventCode.NAME_CHANGED) {
            //todo: this
        } else {
            throw new UnsupportedOperationException("Event: " + sourceMarkEvent.eventCode)
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
            configuration.icon = artifactDataAvailable ? sppActive : sppInactive
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
            configuration.icon = artifactDataAvailable ? sppActive : null
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
            configuration.icon = sppActive
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
        if (!portalRegistered) {
            def appUuid = SourcePluginConfig.current.activeEnvironment.appUuid
            def portalUuid = UUID.randomUUID().toString()
            putUserData(IntelliJKeys.PortalUUID, portalUuid)

            def markComponent = gutterMarkComponent as GutterMarkJcefComponent
            markComponent.configuration.initialUrl = PortalInterface.getPortalUrl(PortalTab.Overview, portalUuid)
            markComponent.initialize()
            SourcePortal.registerInternal(appUuid, portalUuid, artifactQualifiedName, markComponent.browser)
        }
    }
}
