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
 * on artifacts annotated with Spring Framework's web binding annotations.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class SpringMVCArtifactConfigIntegrator extends AbstractVerticle {

    private static final String SPRING_WEB_ANNOTATIONS = Sets.newHashSet([
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.PatchMapping",
            "org.springframework.web.bind.annotation.RequestMapping"
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
                SPRING_WEB_ANNOTATIONS.contains(it.qualifiedName)
            }.each {
                def requestUrl = it.attributeMap.get("value") as String
                SourcePluginConfig.current.activeEnvironment.coreClient.createOrUpdateArtifactConfig(
                        SourcePluginConfig.current.activeEnvironment.appUuid, mark.artifactQualifiedName,
                        SourceArtifactConfig.builder()
                                .component("SpringMVC")
                                .moduleName(mark.getModuleName())
                                .endpoint(true).automaticEndpoint(true)
                                .subscribeAutomatically(true)
                                .endpointName(requestUrl).build(), {
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
