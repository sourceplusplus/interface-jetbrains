package com.sourceplusplus.plugin.coordinate.artifact.track

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PropertyUtilBase
import com.intellij.util.indexing.FileBasedIndex
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.marker.MarkerUtils
import com.sourceplusplus.plugin.intellij.IntelliJStartupActivity
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.plugins.groovy.GroovyFileType
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastContextKt

import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED

/**
 * Keeps track of all current artifacts and keeps them in sync with Source++ Core.
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PluginArtifactTracker extends AbstractVerticle {

    private static final Map<String, SourceArtifact> APPLICATION_ARTIFACTS = new HashMap<>()

    @Override
    void start() throws Exception {
        syncApplicationArtifacts()

        vertx.eventBus().consumer(ARTIFACT_CONFIG_UPDATED.address, {
            def artifact = it.body() as SourceArtifact
            APPLICATION_ARTIFACTS.put(artifact.artifactQualifiedName(), artifact)
        })
    }

    static SourceArtifact getSourceArtifact(String artifactQualifiedName) {
        return APPLICATION_ARTIFACTS.get(artifactQualifiedName)
    }

    static void getOrCreateSourceArtifact(UMethod method, Handler<AsyncResult<SourceArtifact>> handler) {
        def appUuid = SourcePluginConfig.current.activeEnvironment.appUuid
        def artifactQualifiedName = MarkerUtils.getFullyQualifiedName(method)
        if (APPLICATION_ARTIFACTS.containsKey(artifactQualifiedName)) {
            handler.handle(Future.succeededFuture(APPLICATION_ARTIFACTS.get(artifactQualifiedName)))
        } else {
            def sourceArtifact = SourceArtifact.builder()
                    .appUuid(appUuid)
                    .artifactQualifiedName(artifactQualifiedName)
                    .config(SourceArtifactConfig.builder()
                            .subscribeAutomatically(!PropertyUtilBase.isSimplePropertyAccessor(method)).build())
                    .build()
            SourcePluginConfig.current.activeEnvironment.coreClient.upsertArtifact(appUuid, sourceArtifact, {
                if (it.succeeded()) {
                    APPLICATION_ARTIFACTS.put(artifactQualifiedName, it.result())
                    handler.handle(Future.succeededFuture(it.result()))
                } else {
                    handler.handle(Future.failedFuture(it.cause()))
                }
            })
        }
    }

    private static void syncApplicationArtifacts() {
        SourcePluginConfig.current.activeEnvironment.coreClient.getArtifacts(
                SourcePluginConfig.current.activeEnvironment.appUuid, {
            if (it.succeeded()) {
                it.result().each {
                    APPLICATION_ARTIFACTS.put(it.artifactQualifiedName(), it)
                }
                addNewApplicationArtifacts()
            } else {
                log.error("Failed to get application artifacts", it.cause())
            }
        })
    }

    private static void addNewApplicationArtifacts() {
        DumbService.getInstance(IntelliJStartupActivity.currentProject).smartInvokeLater {
            log.info("Syncing application artifacts")

            //include groovy source files
            def groovySourceFiles = FileBasedIndex.getInstance().getContainingFiles(
                    FileTypeIndex.NAME, GroovyFileType.GROOVY_FILE_TYPE,
                    GlobalSearchScope.projectScope(IntelliJStartupActivity.currentProject))

            //include java source files
            def javaSourceFiles = FileBasedIndex.getInstance().getContainingFiles(
                    FileTypeIndex.NAME, JavaFileType.INSTANCE,
                    GlobalSearchScope.projectScope(IntelliJStartupActivity.currentProject))

            //include kotlin source files
            def kotlinSourceFiles = FileBasedIndex.getInstance().getContainingFiles(
                    FileTypeIndex.NAME, KotlinFileType.INSTANCE,
                    GlobalSearchScope.projectScope(IntelliJStartupActivity.currentProject))

            (groovySourceFiles + javaSourceFiles + kotlinSourceFiles).each {
                def sourceFile = PsiManager.getInstance(IntelliJStartupActivity.currentProject).findFile(it)
                if (sourceFile instanceof PsiClassOwner) {
                    sourceFile.classes.each {
                        it.methods.each { method ->
                            try {
                                def uMethod = UastContextKt.toUElement(method) as UMethod
                                def artifactQualifiedName = MarkerUtils.getFullyQualifiedName(uMethod)
                                if (!APPLICATION_ARTIFACTS.containsKey(artifactQualifiedName)) {
                                    getOrCreateSourceArtifact(uMethod, {
                                        if (it.failed()) {
                                            log.error("Failed to create artifact", it.cause())
                                        }
                                    })
                                }
                            } catch (Throwable ignored) {
                                log.warn("Failed to determine artifact qualified name of: " + sourceFile + "." + method)
                            }
                        }
                    }
                }
            }

            log.info("Synced application artifacts")
        }
    }
}
