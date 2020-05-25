package com.sourceplusplus.plugin.intellij.patcher

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.intellij.IntelliJStartupActivity
import com.sourceplusplus.plugin.intellij.patcher.tail.LogTailer
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.FileUtils

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Used to add the Source++ Agent to project executions.
 *
 * @version 0.2.6
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
            URL apmArchiveResource = SourceAgentPatcher.class.getResource("/apm-agent/apache-skywalking-apm-7.0.0.tar.gz")
            File destDir = File.createTempDir()
            File skywalkingArchive = new File(destDir, "apache-skywalking-apm-7.0.0.tar.gz")
            FileUtils.copyURLToFile(apmArchiveResource, skywalkingArchive)
            skywalkingArchive.deleteOnExit()
            destDir.deleteOnExit()

            //extract Apache SkyWalking agent
            unTarGz(skywalkingArchive.toPath(), destDir.toPath())
            agentFile = new File(destDir, "apache-skywalking-apm-bin/agent/skywalking-agent.jar")
            agentFile.deleteOnExit()

            //move apm-customize-enhance-plugin-*.jar to plugins
            URL customEnhancePlugin = SourceAgentPatcher.class.getResource("/apm-agent/apm-customize-enhance-plugin-7.0.0.jar")
            FileUtils.copyURLToFile(customEnhancePlugin, new File(agentFile.parentFile,
                    "plugins" + File.separator + "apm-customize-enhance-plugin-7.0.0.jar"))
//            Files.move(new File(agentFile.parentFile, "optional-plugins" + File.separator + "apm-customize-enhance-plugin-7.0.0.jar").toPath(),
//                    new File(agentFile.parentFile, "plugins" + File.separator + "apm-customize-enhance-plugin-7.0.0.jar").toPath(),
//                    StandardCopyOption.REPLACE_EXISTING)

            //redirect skywalking logs to console
            def skywalkingLogFile = new File(agentFile.parentFile, "logs" + File.separator + "skywalking-api.log")
            skywalkingLogFile.parentFile.mkdirs()
            log.info("Tailing log file: " + skywalkingLogFile)
            skywalkingLogFile.createNewFile()
            skywalkingLogFile.deleteOnExit()
            new Thread(new LogTailer(skywalkingLogFile, "SKYWALKING")).start()
        }

        if (agentFile != null) {
            //inject agent config
            modifyAgentInstallation(agentFile.parentFile)
        }
    }

    private static void modifyAgentInstallation(File agentDirectory) throws IOException {
        def customizeEnhanceFile = new File(agentDirectory, "config" + File.separator + "customize_enhance.xml")
        customizeEnhanceFile.withWriter { writer ->
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            writer.append("<enhanced>\n")

            def coreClient = new SourceCoreClient(SourcePluginConfig.current.activeEnvironment.sppUrl)
            if (SourcePluginConfig.current.activeEnvironment.apiKey) {
                coreClient.apiKey = SourcePluginConfig.current.activeEnvironment.apiKey
            }
            def endpointArtifacts = coreClient.getApplicationEndpoints(SourcePluginConfig.current.activeEnvironment.appUuid)
            Map<String, List<SourceArtifact>> classEndpoints = new HashMap<>()
            endpointArtifacts.each {
                def className = getClassName(it.artifactQualifiedName())
                classEndpoints.putIfAbsent(className, new ArrayList<>())
                classEndpoints.get(className).add(it)
            }

            ApplicationManager.getApplication().runReadAction {
                def sourceFiles = FileBasedIndex.getInstance().getContainingFiles(
                        FileTypeIndex.NAME, JavaFileType.INSTANCE,
                        GlobalSearchScope.projectScope(IntelliJStartupActivity.currentProject))
                sourceFiles.each {
                    def sourceFile = PsiManager.getInstance(IntelliJStartupActivity.currentProject).findFile(it)
                    if (sourceFile instanceof PsiClassOwner) {
                        sourceFile.classes.each {
                            writer.append("\t<class class_name=\"" + it.qualifiedName + "\">\n")
                            if (classEndpoints.containsKey(it.qualifiedName)) {
                                classEndpoints.get(it.qualifiedName).each {
                                    def methodName = removePackageAndClassName(it.artifactQualifiedName())
                                    def isStatic = "true" //todo: dynamic
                                    writer.append("\t\t<method method=\"$methodName\" static=\"$isStatic\" entry_method=\"true\"></method>\n")
                                }
                            }
                            writer.append("\t\t<method method=\"*\" static=\"true\"></method>\n")
                            writer.append("\t\t<method method=\"*\" static=\"false\"></method>\n")
                            writer.append("\t</class>\n")
                        }
                    }
                }
            }
            writer.append("</enhanced>")
        }

        def agentConfig = new File(agentDirectory, "config" + File.separator + "agent.config")
        Properties prop = new Properties()
        new FileInputStream(agentConfig).withCloseable { input ->
            prop.load(input)
        }
        new FileOutputStream(agentConfig).withCloseable { output ->
            //prop.setProperty("agent.instance_properties[keyz]", "valuez")
            //prop.setProperty("agent.is_open_debugging_class", "true")
            prop.setProperty("agent.service_name", SourcePluginConfig.current.activeEnvironment.appUuid)
            prop.setProperty("plugin.customize.enhance_file", customizeEnhanceFile.absolutePath)
            prop.store(output, null)
        }
    }

    private static void unTarGz(Path pathInput, Path pathOutput) throws IOException {
        TarArchiveInputStream archiveInputStream = new TarArchiveInputStream(
                new GzipCompressorInputStream(new BufferedInputStream(Files.newInputStream(pathInput))))

        ArchiveEntry entry
        while ((entry = archiveInputStream.getNextEntry()) != null) {
            Path pathEntryOutput = pathOutput.resolve(entry.getName())
            if (entry.isDirectory()) {
                if (!Files.exists(pathEntryOutput)) {
                    pathEntryOutput.toFile().mkdirs()
                }
            } else {
                Files.copy(archiveInputStream, pathEntryOutput)
            }
        }
        archiveInputStream.close()
    }

    static String getClassName(String qualifiedMethodName) {
        if (!qualifiedMethodName || qualifiedMethodName.indexOf('.') == -1) return qualifiedMethodName
        return qualifiedMethodName.substring(0, qualifiedMethodName.substring(
                0, qualifiedMethodName.indexOf("(")).lastIndexOf("."))
    }

    static removePackageAndClassName(String qualifiedMethodName) {
        if (!qualifiedMethodName || qualifiedMethodName.indexOf('.') == -1 || qualifiedMethodName.indexOf('(') == -1) {
            return qualifiedMethodName
        }
        return qualifiedMethodName.substring(qualifiedMethodName.substring(
                0, qualifiedMethodName.indexOf("(")).lastIndexOf(".") + 1)
    }
}
