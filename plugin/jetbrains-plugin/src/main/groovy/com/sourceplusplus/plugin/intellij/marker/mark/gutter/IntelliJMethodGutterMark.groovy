package com.sourceplusplus.plugin.intellij.marker.mark.gutter

import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiLiteral
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.trace.TraceOrderType
import com.sourceplusplus.marker.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.gutter.MethodGutterMark
import com.sourceplusplus.marker.source.mark.gutter.component.jcef.GutterMarkJcefComponent
import com.sourceplusplus.marker.source.mark.gutter.event.GutterMarkEventCode
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.coordinate.artifact.track.PluginArtifactTracker
import com.sourceplusplus.plugin.coordinate.integration.IntegrationInfoTracker
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJKeys
import com.sourceplusplus.plugin.intellij.portal.IntelliJPortalUI
import com.sourceplusplus.plugin.intellij.portal.IntelliJSourcePortal
import com.sourceplusplus.plugin.source.model.SourceMethodAnnotation
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.display.PortalTab
import groovy.util.logging.Slf4j
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.groovy.lang.psi.uast.GrUAnnotation
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.java.JavaUAnnotation

import javax.swing.*

/**
 * Extension of the MethodGutterMark for handling IntelliJ.
 *
 * @version 0.3.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class IntelliJMethodGutterMark extends MethodGutterMark implements IntelliJGutterMark, SourceMarkEventListener {

    IntelliJMethodGutterMark(SourceFileMarker sourceFileMarker, UMethod psiMethod) {
        super(sourceFileMarker, psiMethod)

        def appUuid = SourcePluginConfig.current.activeEnvironment.appUuid
        def sourceArtifact = SourceArtifact.builder()
                .appUuid(appUuid).artifactQualifiedName(artifactQualifiedName).build()
        putUserData(IntelliJKeys.SourceArtifact, sourceArtifact)
        PluginArtifactTracker.getOrCreateSourceArtifact(psiMethod, {
            if (it.succeeded()) {
                updateSourceArtifact(it.result())
            } else {
                log.error("Failed to create artifact: " + artifactQualifiedName, it.cause())
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void apply() {
        super.apply()

        SourcePlugin.vertx.eventBus().publish(SOURCE_MARK_APPLIED, this)
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
            if (getUserData(IntelliJKeys.PortalRefresher) != null) {
                SourcePlugin.vertx.cancelTimer(getUserData(IntelliJKeys.PortalRefresher))
                putUserData(IntelliJKeys.PortalRefresher, null)
            }
            if (portalRegistered) {
                IntelliJSourcePortal.getPortal(portalUuid).close()
                putUserData(IntelliJKeys.PortalUUID, null)
            }

            def unsubscribeRequest = SourceArtifactUnsubscribeRequest.builder()
                    .appUuid(SourcePluginConfig.current.activeEnvironment.appUuid)
                    .artifactQualifiedName(artifactQualifiedName)
                    .build()
            SourcePluginConfig.current.activeEnvironment.coreClient.unsubscribeFromArtifact(unsubscribeRequest, {
                if (it.failed()) {
                    log.error("Failed to unsubscribe from artifact: $artifactQualifiedName", it.cause())
                }
            })
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
                    if (it instanceof JavaUAnnotation || it instanceof GrUAnnotation) {
                        def attributeMap = new HashMap<String, Object>()
                        it.attributeValues.each {
                            if (it.name) {
                                attributeMap.put(it.name, (it.expression as ULiteralExpression).value)
                            } else {
                                //log.warn("Unknown annotation expression: " + it)
                            }
                        }
                        annotations.add(new SourceMethodAnnotation(qualifiedName, attributeMap))
                    } else {
                        def attributeMap = new HashMap<String, Object>()
                        it.attributes.each {
                            if (it.attributeValue instanceof JvmAnnotationConstantValue) {
                                def annotationConstantValue = it.attributeValue as JvmAnnotationConstantValue
                                attributeMap.put(it.attributeName, annotationConstantValue.constantValue)
                            } else if (it.attributeValue.sourceElement instanceof PsiLiteral) {
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
    void updateSourceArtifact(SourceArtifact sourceArtifact) {
        putUserData(IntelliJKeys.SourceArtifact, sourceArtifact)
        def updatedIcon = determineMostSuitableIcon()
        if (configuration.icon != updatedIcon) {
            configuration.icon = updatedIcon
            sourceFileMarker.refresh()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    SourceArtifact getSourceArtifact() {
        return getUserData(IntelliJKeys.SourceArtifact)
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
        registerPortal(null)
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

            def newPortal = null
            def markComponent = gutterMarkComponent as GutterMarkJcefComponent
            if (initialTab == null) {
                if (sourceArtifact.status().activelyFailing()) {
                    markComponent.configuration.initialUrl =
                            IntelliJPortalUI.getPortalUrl(PortalTab.Traces, portalUuid) +
                                    "&order_type=" + TraceOrderType.FAILED_TRACES +
                                    "&hide_overview_tab=true"

                    newPortal = new IntelliJPortalUI(portalUuid, markComponent.browser)
                    newPortal.tracesView.orderType = TraceOrderType.FAILED_TRACES
                } else {
                    markComponent.configuration.initialUrl = IntelliJPortalUI.getPortalUrl(PortalTab.Overview, portalUuid)
                    newPortal = new IntelliJPortalUI(portalUuid, markComponent.browser)
                }
            } else {
                markComponent.configuration.initialUrl = IntelliJPortalUI.getPortalUrl(initialTab, portalUuid)
            }

            IntelliJSourcePortal finalPortal
            if (existingPortal.present) {
                existingPortal.get().portalUI.lateInitBrowser(markComponent.browser)
                finalPortal = existingPortal.get()
            } else {
                IntelliJSourcePortal.register(appUuid, portalUuid, artifactQualifiedName, newPortal)
                finalPortal = IntelliJSourcePortal.getPortal(portalUuid)
            }

            if (getUserData(IntelliJKeys.PortalRefresher) == null) {
                putUserData(IntelliJKeys.PortalRefresher, SourcePlugin.vertx.setPeriodic(60_000 * 2, {
                    SourcePortal.ensurePortalActive(finalPortal)
                }))
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    IntelliJSourcePortal getPortal() {
        return IntelliJSourcePortal.getPortal(portalUuid)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Icon determineMostSuitableIcon() {
        if (SourcePluginConfig.current.methodGutterMarksEnabled) {
            if (sourceArtifact.status().activelyFailing()) {
                return failingMethod
            } else if (sourceArtifact.config().endpoint()) {
                if (IntegrationInfoTracker.getActiveIntegrationInfo("apache_skywalking")) {
                    return activeEntryMethod
                } else {
                    return inactiveEntryMethod
                }
            }
        }
        return null
    }
}
