package com.sourceplusplus.plugin.intellij.patcher

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.components.ServiceManager
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.SourcePluginDefines
import com.sourceplusplus.plugin.intellij.IntelliJStartupActivity
import com.sourceplusplus.plugin.intellij.tool.SourcePluginConsoleService
import com.sourceplusplus.portal.display.PortalInterface
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.apache.commons.io.FileUtils
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SourceAgentPatcher extends JavaProgramPatcher {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private static AtomicBoolean patched = new AtomicBoolean()
    private static File agentFile

    @Override
    void patchJavaParameters(Executor executor, RunProfile configuration, JavaParameters javaParameters) {
        if (!SourcePluginConfig.current.agentPatcherEnabled) {
            log.info("Skipped patching program. Agent patcher is disabled.")
            return
        }
        if (PluginBootstrap.sourcePlugin != null && !patched.getAndSet(true)) {
            log.info("Patching Source++ Agent for executing program...")
            URL inputUrl = getClass().getResource("/source-agent-" + SourcePluginDefines.VERSION + ".jar")
            File destDir = File.createTempDir()
            agentFile = new File(destDir, "source-agent-" + SourcePluginDefines.VERSION + ".jar")
            FileUtils.copyURLToFile(inputUrl, agentFile)
            agentFile.deleteOnExit()
            destDir.deleteOnExit()

            //inject temp app id
            modifyAgentJar(agentFile.absolutePath)

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
            new Thread(new Runnable() {
                @Override
                void run() {
                    def consoleView = ServiceManager.getService(IntelliJStartupActivity.currentProject,
                            SourcePluginConsoleService.class).getConsoleView()
                    new Tailer(logFile, new TailerListenerAdapter() {
                        @Override
                        void handle(Exception ex) {
                            ex.stackTrace.each {
                                consoleView.print(it.toString() + "\n", ConsoleViewContentType.ERROR_OUTPUT)
                            }
                        }

                        @Override
                        void handle(String line) {
                            consoleView.print(line + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
                        }
                    }).run()
                }
            }).start()

            //redirect skywalking logs to console
            def skywalkingLogFile = new File(destDir.absolutePath + File.separator + "logs" + File.separator + "skywalking-api.log")
            skywalkingLogFile.parentFile.mkdirs()
            log.info("Tailing log file: " + skywalkingLogFile)
            skywalkingLogFile.createNewFile()
            skywalkingLogFile.deleteOnExit()
            new Thread(new Runnable() {
                @Override
                void run() {
                    def consoleView = ServiceManager.getService(IntelliJStartupActivity.currentProject,
                            SourcePluginConsoleService.class).getConsoleView()
                    new Tailer(skywalkingLogFile, new TailerListenerAdapter() {
                        @Override
                        void handle(Exception ex) {
                            ex.stackTrace.each {
                                consoleView.print("[SKYWALKING] - " + it.toString() + "\n", ConsoleViewContentType.ERROR_OUTPUT)
                            }
                        }

                        @Override
                        void handle(String line) {
                            consoleView.print("[SKYWALKING] - " + line + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
                        }
                    }).run()
                }
            }).start()
        }

        if (agentFile != null) {
            javaParameters.getVMParametersList()?.add("-javaagent:$agentFile.absolutePath")
            log.info("Attached Source++ Agent to executing program")
        }
    }

    static void modifyAgentJar(String agentPath) throws IOException {
        Path agentFilePath = Paths.get(agentPath)
        FileSystems.newFileSystem(agentFilePath, null).withCloseable { fs ->
            Path source = fs.getPath("/source-agent.json")
            Path temp = fs.getPath("/temp_source-agent.json")
            Files.move(source, temp)
            modifyAgentSettings(temp, source)
            Files.delete(temp)
        }
    }

    static void modifyAgentSettings(Path src, Path dst) throws IOException {
        def agentConfig = new JsonObject(Files.newInputStream(src).getText())

        agentConfig.put("log_location", agentFile.parentFile.absolutePath)
        agentConfig.getJsonObject("application").put("app_uuid", SourcePluginConfig.current.appUuid)
        agentConfig.getJsonObject("api").put("host", SourcePluginConfig.current.apiHost)
        agentConfig.getJsonObject("api").put("port", SourcePluginConfig.current.apiPort)
        agentConfig.getJsonObject("api").put("ssl", SourcePluginConfig.current.apiSslEnabled)
        agentConfig.getJsonObject("api").put("key", SourcePluginConfig.current.apiKey)

        agentConfig.getJsonObject("skywalking").put("backend_service",
                SourcePluginConfig.current.apiHost + ':11800') //todo: configurable skywalking port

        agentConfig.getJsonObject("plugin-bridge").put("host", SourcePluginConfig.current.remoteAgentHost)
        agentConfig.getJsonObject("plugin-bridge").put("port", SourcePluginConfig.current.remoteAgentPort)

        Files.newOutputStream(dst).withWriter {
            it.write(Json.encodePrettily(agentConfig))
        }
    }
}
