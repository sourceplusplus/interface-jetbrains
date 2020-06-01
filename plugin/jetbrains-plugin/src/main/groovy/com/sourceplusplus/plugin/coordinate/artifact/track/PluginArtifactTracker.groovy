package com.sourceplusplus.plugin.coordinate.artifact.track

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.marker.MarkerUtils
import com.sourceplusplus.plugin.intellij.IntelliJStartupActivity
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.plugins.groovy.GroovyFileType
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastContextKt

import java.util.stream.Collectors

/**
 * Keeps track of all current artifacts and keeps them in sync with Source++ Core.
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PluginArtifactTracker extends AbstractVerticle {

//    public static final String SYNC_APPLICATION_ARTIFACTS = "SyncApplicationArtifacts"
    private static final Map<String, Set<PsiMethod>> CREATED_ARTIFACTS = new HashMap<>()

    @Override
    void start() throws Exception {
        syncApplicationArtifacts()
//        vertx.eventBus().consumer(SYNC_APPLICATION_ARTIFACTS, {
//            syncApplicationArtifacts()
//        })
    }

    private static void syncApplicationArtifacts() {
        SourcePluginConfig.current.activeEnvironment.coreClient.getArtifacts(
                SourcePluginConfig.current.activeEnvironment.appUuid, {
            if (it.succeeded()) {
                addNewApplicationArtifacts(
                        it.result().stream().map { it.artifactQualifiedName() }
                                .collect(Collectors.toSet())
                )
            } else {
                log.error("Failed to get application artifacts", it.cause())
            }
        })
    }

    private static void addNewApplicationArtifacts(Set<String> currentApplicationArtifacts) {
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

            def appUuid = SourcePluginConfig.current.activeEnvironment.appUuid
            CREATED_ARTIFACTS.putIfAbsent(appUuid, new HashSet<PsiMethod>())
            def createdArtifacts = CREATED_ARTIFACTS.get(appUuid)
            (groovySourceFiles + javaSourceFiles + kotlinSourceFiles).each {
                def sourceFile = PsiManager.getInstance(IntelliJStartupActivity.currentProject).findFile(it)
                if (sourceFile instanceof PsiClassOwner) {
                    sourceFile.classes.each {
                        it.methods.each { method ->
                            if (!createdArtifacts.contains(it)) {
                                createdArtifacts.add(method)

                                try {
                                    def artifactQualifiedName = MarkerUtils.getFullyQualifiedName(UastContextKt.toUElement(method) as UMethod)
                                    if (!currentApplicationArtifacts.contains(artifactQualifiedName)) {
                                        def sourceArtifact = SourceArtifact.builder()
                                                .appUuid(appUuid)
                                                .artifactQualifiedName(artifactQualifiedName).build()
                                        SourcePluginConfig.current.activeEnvironment.coreClient.createArtifact(
                                                appUuid, sourceArtifact, {
                                            if (it.succeeded()) {
                                                log.debug("Created artifact: " + artifactQualifiedName)
                                            } else {
                                                log.error("Failed to create artifact", it.cause())
                                                createdArtifacts.remove(method)
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
            }

            log.info("Synced application artifacts")
        }
    }
}
