package com.sourceplusplus.plugin.intellij.patcher

import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.SourcePluginDefines
import com.sourceplusplus.plugin.intellij.patcher.tail.LogTailer
import com.sourceplusplus.portal.display.PortalInterface
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.apache.commons.io.FileUtils

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
trait SourceAgentPatcher {

    @PackageScope static File agentFile
    private static AtomicBoolean patched = new AtomicBoolean()

    static void patchAgent() {
        if (!SourcePluginConfig.current.agentPatcherEnabled) {
            log.info("Skipped patching program. Agent patcher is disabled.")
            return
        }
        if (PluginBootstrap.sourcePlugin != null && !patched.getAndSet(true)) {
            log.info("Patching Source++ Agent for executing program...")
            URL inputUrl = SourceAgentPatcher.class.getResource("/source-agent-" + SourcePluginDefines.VERSION + ".jar")
            File destDir = File.createTempDir()
            agentFile = new File(destDir, "source-agent-" + SourcePluginDefines.VERSION + ".jar")
            FileUtils.copyURLToFile(inputUrl, agentFile)
            agentFile.deleteOnExit()
            destDir.deleteOnExit()

            //extract plugins
            def pluginsUrl = SourceAgentPatcher.class.getResource("/plugins")
            def pluginsDir = new File(destDir, "plugins")
            pluginsDir.mkdir()
            PortalInterface.extract(pluginsUrl, "/plugins", pluginsDir.absolutePath)

            //extract activations
            def activationsUrl = SourceAgentPatcher.class.getResource("/activations")
            def activationsDir = new File(destDir, "activations")
            activationsDir.mkdir()
            PortalInterface.extract(activationsUrl, "/activations", activationsDir.absolutePath)

            //redirect agent logs to console
            def logFile = new File(destDir.absolutePath + File.separator + "source-agent.log")
            log.info("Tailing log file: " + logFile)
            logFile.createNewFile()
            logFile.deleteOnExit()
            new Thread(new LogTailer(logFile, "AGENT")).start()

            //redirect skywalking logs to console
            def skywalkingLogFile = new File(destDir.absolutePath + File.separator + "logs" + File.separator + "skywalking-api.log")
            skywalkingLogFile.parentFile.mkdirs()
            log.info("Tailing log file: " + skywalkingLogFile)
            skywalkingLogFile.createNewFile()
            skywalkingLogFile.deleteOnExit()
            new Thread(new LogTailer(skywalkingLogFile, "SKYWALKING")).start()
        }

        if (agentFile != null) {
            //inject agent config
            modifyAgentJar(agentFile.absolutePath)
        }
    }

    private static void modifyAgentJar(String agentPath) throws IOException {
        Path agentFilePath = Paths.get(agentPath)
        FileSystems.newFileSystem(agentFilePath, null).withCloseable { fs ->
            Path source = fs.getPath("/source-agent.json")
            Path temp = fs.getPath("/temp_source-agent.json")
            Files.move(source, temp)
            modifyAgentSettings(temp, source)
            Files.delete(temp)
        }
    }

    private static void modifyAgentSettings(Path src, Path dst) throws IOException {
        def agentConfig = new JsonObject(Files.newInputStream(src).getText())

        agentConfig.put("log_location", agentFile.parentFile.absolutePath)
        agentConfig.getJsonObject("application").put("app_uuid", SourcePluginConfig.current.activeEnvironment.appUuid)
        agentConfig.getJsonObject("api").put("host", SourcePluginConfig.current.activeEnvironment.apiHost)
        agentConfig.getJsonObject("api").put("port", SourcePluginConfig.current.activeEnvironment.apiPort)
        agentConfig.getJsonObject("api").put("ssl", SourcePluginConfig.current.activeEnvironment.apiSslEnabled)
        agentConfig.getJsonObject("api").put("key", SourcePluginConfig.current.activeEnvironment.apiKey)

        agentConfig.getJsonObject("plugin-bridge").put("host", SourcePluginConfig.current.remoteAgentHost)
        agentConfig.getJsonObject("plugin-bridge").put("port", SourcePluginConfig.current.remoteAgentPort)

        Files.newOutputStream(dst).withWriter {
            it.write(Json.encodePrettily(agentConfig))
        }
    }
}
