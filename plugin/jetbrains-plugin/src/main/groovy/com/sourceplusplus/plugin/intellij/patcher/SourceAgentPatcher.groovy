package com.sourceplusplus.plugin.intellij.patcher

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.marker.MarkerUtils
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.intellij.IntelliJStartupActivity
import com.sourceplusplus.plugin.intellij.patcher.tail.LogTailer
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.FileUtils
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.plugins.groovy.GroovyFileType
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastContextKt

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
            URL apmArchiveResource = SourceAgentPatcher.class.getResource("/skywalking/apache-skywalking-apm-7.0.0.tar.gz")
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
            URL customEnhancePlugin = SourceAgentPatcher.class.getResource("/skywalking/apm-customize-enhance-plugin-7.0.0.jar")
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

            def endpointArtifacts = SourcePluginConfig.current.activeEnvironment.coreClient
                    .getApplicationEndpoints(SourcePluginConfig.current.activeEnvironment.appUuid)
            Set<String> artifactEndpoints = new HashSet<>()
            endpointArtifacts.each { artifactEndpoints.add(it.artifactQualifiedName()) }

            ApplicationManager.getApplication().runReadAction {
                //include groovy source files
                def groovySourceFiles = FileBasedIndex.getInstance().getContainingFiles(
                        FileTypeIndex.NAME, GroovyFileType.GROOVY_FILE_TYPE,
                        GlobalSearchScope.projectScope(IntelliJStartupActivity.currentProject))
                processSourceFiles(groovySourceFiles, writer, artifactEndpoints)

                //include java source files
                def javaSourceFiles = FileBasedIndex.getInstance().getContainingFiles(
                        FileTypeIndex.NAME, JavaFileType.INSTANCE,
                        GlobalSearchScope.projectScope(IntelliJStartupActivity.currentProject))
                processSourceFiles(javaSourceFiles, writer, artifactEndpoints)

                //include kotlin source files
                def kotlinSourceFiles = FileBasedIndex.getInstance().getContainingFiles(
                        FileTypeIndex.NAME, KotlinFileType.INSTANCE,
                        GlobalSearchScope.projectScope(IntelliJStartupActivity.currentProject))
                processSourceFiles(kotlinSourceFiles, writer, artifactEndpoints)
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

    private static void processSourceFiles(Collection<VirtualFile> sourceFiles, BufferedWriter writer,
                                           Set<String> artifactEndpoints) {
        sourceFiles.each {
            def sourceFile = PsiManager.getInstance(IntelliJStartupActivity.currentProject).findFile(it)
            if (sourceFile instanceof PsiClassOwner) {
                sourceFile.classes.each {
                    //todo: don't write class if no methods are found
                    writer.append("\t<class class_name=\"" + it.qualifiedName + "\">\n")
                    it.methods.each {
                        try {
                            def artifactQualifiedName = MarkerUtils.getFullyQualifiedName(UastContextKt.toUElement(it) as UMethod)
                            def methodName = removePackageAndClassName(artifactQualifiedName)
                            def entryMethod = artifactEndpoints.contains(artifactQualifiedName)
                            def isStatic = it.hasModifier(JvmModifier.STATIC)
                            writer.append("\t\t<method method=\"$methodName\" static=\"$isStatic\" entry_method=\"$entryMethod\"></method>\n")
                        } catch (Throwable ignored) {
                            log.warn("Failed to determine artifact qualified name of: " + it)
                        }
                    }
                    writer.append("\t</class>\n")
                }
            }
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
