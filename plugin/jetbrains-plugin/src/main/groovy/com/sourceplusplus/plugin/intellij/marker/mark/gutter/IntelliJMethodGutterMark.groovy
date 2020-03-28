package com.sourceplusplus.plugin.intellij.marker.mark.gutter

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiLiteral
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJKeys
import com.sourceplusplus.plugin.source.model.SourceMethodAnnotation
import com.sourceplusplus.portal.SourcePortal
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.java.JavaUAnnotation
import plus.sourceplus.marker.SourceFileMarker
import plus.sourceplus.marker.source.mark.gutter.MethodGutterMark
import plus.sourceplus.marker.source.mark.gutter.config.GutterMarkIcon

import java.time.Instant

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.2.4
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
//@Slf4j //todo: can't override log
class IntelliJMethodGutterMark extends MethodGutterMark implements IntelliJGutterMark {

    IntelliJMethodGutterMark(SourceFileMarker sourceFileMarker, UMethod psiMethod) {
        super(sourceFileMarker, psiMethod)
    }

    //todo: impl
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
            configuration.icon = GutterMarkIcon.sppActive
            putUserData(IntelliJKeys.ArtifactSubscribed, true)
            putUserData(IntelliJKeys.ArtifactSubscribeTime, Instant.now())
            sourceFileMarker.refresh()
            //registerPortal()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void markArtifactUnsubscribed() {
        if (artifactSubscribed) {
            configuration.icon = null
            putUserData(IntelliJKeys.ArtifactSubscribed, false)
            putUserData(IntelliJKeys.ArtifactUnsubscribeTime, Instant.now())
            sourceFileMarker.refresh()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void markArtifactHasData() {
        if (!artifactDataAvailable) {
            configuration.icon = GutterMarkIcon.sppActive
            putUserData(IntelliJKeys.ArtifactDataAvailable, true)
            sourceFileMarker.refresh()
            //registerPortal()
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
    boolean isViewable() {
        return false //todo: this
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
            def portalUuid = SourcePortal.register(appUuid, artifactQualifiedName, false)
            SourcePortal.getPortal(portalUuid).interface.initPortal()
            putUserData(IntelliJKeys.PortalUUID, portalUuid)
        }
    }
}
