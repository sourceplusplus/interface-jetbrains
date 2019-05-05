package com.sourceplusplus.plugin.coordinate.artifact.config

import com.google.common.collect.Sets
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.marker.mark.SourceMark
import io.vertx.core.AbstractVerticle
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SpringMVCArtifactConfigIntegrator extends AbstractVerticle {

    private static final String SPRING_WEB_ANNOTATIONS = Sets.newHashSet([
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.PatchMapping",
            "org.springframework.web.bind.annotation.RequestMapping"
    ])
    private static final Logger log = LoggerFactory.getLogger(this.name)

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(SourcePlugin.SOURCE_FILE_MARKER_ACTIVATED, {
            def sourceMarks = PluginBootstrap.sourcePlugin.getSourceFileMarker(it.body() as String).sourceMarks
            handleSourceMarks(sourceMarks)
        })
    }

    private static List<SourceMark> handleSourceMarks(List<SourceMark> sourceMarks) {
        sourceMarks.each { mark ->
            if (mark.isMethodMark()) {
                mark.getMethodAnnotations({
                    it.result().findAll {
                        SPRING_WEB_ANNOTATIONS.contains(it.qualifiedName)
                    }.each {
                        def requestUrl = it.attributeMap.get("value") as String
                        PluginBootstrap.sourcePlugin.coreClient.createOrUpdateArtifactConfig(
                                SourcePluginConfig.current.activeEnvironment.appUuid, mark.artifactQualifiedName,
                                SourceArtifactConfig.builder()
                                        .component("SpringMVC")
                                        .moduleName(mark.getModuleName())
                                        .endpoint(true).subscribeAutomatically(true)
                                        .endpointName(requestUrl).build(), {
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
