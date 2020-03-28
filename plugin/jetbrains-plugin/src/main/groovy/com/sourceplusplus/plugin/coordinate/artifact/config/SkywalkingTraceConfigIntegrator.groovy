package com.sourceplusplus.plugin.coordinate.artifact.config

import com.google.common.collect.Sets
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle

/**
 * todo: description
 *
 * @version 0.2.3
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
        vertx.eventBus().consumer(SourcePlugin.SOURCE_FILE_MARKER_ACTIVATED, {
            def sourceMarks = PluginBootstrap.sourcePlugin.getSourceFileMarker(it.body() as String).sourceMarks
            handleSourceMarks(sourceMarks)
        })
    }

    private static List<IntelliJSourceMark> handleSourceMarks(List<IntelliJSourceMark> sourceMarks) {
        sourceMarks.each { mark ->
            if (mark.isMethodMark()) {
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
                            if (it.failed()) {
                                log.error("Failed to create artifact config", it.cause())
                            } else {
                                log.debug("Created/Updated artifact config for artifact: " + mark.artifactQualifiedName)
                            }
                        })
                    }
                })
            }
        }
    }
}
