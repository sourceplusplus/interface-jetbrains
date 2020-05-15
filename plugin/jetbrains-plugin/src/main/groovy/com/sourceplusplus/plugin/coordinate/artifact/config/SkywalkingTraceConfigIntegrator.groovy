package com.sourceplusplus.plugin.coordinate.artifact.config

import com.google.common.collect.Sets
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJMethodGutterMark
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle

/**
 * Automatically appends the appropriate artifact configuration
 * on artifacts annotated with Apache SkyWalking's @Trace.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class SkywalkingTraceConfigIntegrator extends AbstractVerticle {

    private static final String TRACE_ANNOTATION_QUALIFIED_NAMES = Sets.newHashSet([
            "org.apache.skywalking.apm.toolkit.trace.Trace"
    ])

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(IntelliJSourceMark.SOURCE_MARK_CREATED, {
            handleSourceMark(it.body() as IntelliJMethodGutterMark)
        })
    }

    private static void handleSourceMark(IntelliJMethodGutterMark mark) {
        mark.getMethodAnnotations({
            it.result().findAll {
                TRACE_ANNOTATION_QUALIFIED_NAMES.contains(it.qualifiedName)
            }.each {
                def artifactConfig = SourceArtifactConfig.builder()
                        .component("Local")
                        .moduleName(mark.getModuleName())
                        .endpoint(false)
                def operationName = it.attributeMap.get("operationName") as String
                if (operationName) {
                    artifactConfig.endpointName(operationName)
                }
                SourcePluginConfig.current.activeEnvironment.coreClient.createOrUpdateArtifactConfig(
                        SourcePluginConfig.current.activeEnvironment.appUuid, mark.artifactQualifiedName,
                        artifactConfig.build(), {
                    if (it.succeeded()) {
                        log.debug("Created/Updated artifact config for artifact: " + mark.artifactQualifiedName)
                    } else {
                        log.error("Failed to create artifact config", it.cause())
                    }
                })
            }
        })
    }
}
