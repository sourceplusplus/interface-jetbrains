package com.sourceplusplus.tooltip.display

import com.sourceplusplus.api.model.config.SourceTooltipConfig
import groovy.io.FileType
import io.vertx.core.Vertx
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import org.apache.commons.io.FileUtils
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*
import java.awt.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Used to render the Source++ Tooltip's Semantic UI HTML files as a JFXPanel.
 *
 * @version 0.1.3
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class TooltipUI {

    public static final String TOOLTIP_READY = "TooltipReady"

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private static Scene scene
    private static File uiDirectory
    private static Vertx vertx
    private static WebEngine webEngine

    @NotNull
    static JComponent getTooltipUI() {
        JFXPanel fxPanel = new JFXPanel()
        fxPanel.setPreferredSize(new Dimension(725, 275)) //580, 220

        Platform.setImplicitExit(false)
        Platform.runLater({
            if (scene == null) {
                scene = createScene()
                vertx.eventBus().publish(TOOLTIP_READY, true)
            }
            fxPanel.setScene(scene)
        })
        return fxPanel
    }

    static void updateTheme(boolean dark) {
        if (dark) {
            def tabsFolder = new File(uiDirectory.absolutePath, "tabs")
            tabsFolder.eachFile(FileType.FILES, {
                def updatedHtml = it.text
                        .replaceAll('<!--<script src="../themes/default/assets/charts/gray.js"></script>-->', '<script src="../themes/default/assets/charts/gray.js"></script>')
                        .replaceAll("semantic.min.css", "semantic_dark.min.css")
                it.text = ""
                it << updatedHtml
            })
        } else {
            def tabsFolder = new File(uiDirectory.absolutePath, "tabs")
            tabsFolder.eachFile(FileType.FILES, {
                def updatedHtml = it.text
                        .replaceAll('<script src="../themes/default/assets/charts/gray.js"></script>', '<!--<script src="../themes/default/assets/charts/gray.js"></script>-->')
                        .replaceAll("semantic_dark.min.css", "semantic.min.css")
                it.text = ""
                it << updatedHtml
            })
        }
        Platform.runLater({
            webEngine.reload()
        })
    }

    static void preloadTooltipUI(Vertx vertx) {
        this.vertx = vertx
        getTooltipUI()
    }

    private static Scene createScene() {
        log.debug("Creating tooltip scene")
        Scene scene = new Scene(new Group())
        WebView browser = new WebView()
        webEngine = browser.getEngine()

        uiDirectory = File.createTempDir()
        uiDirectory.deleteOnExit()

        def url = TooltipUI.class.getResource("/ui")
        extract(url, "/ui", uiDirectory.absolutePath)
        log.debug("Using tooltip ui directory: " + uiDirectory.absolutePath)

        webEngine.load("file:///" + uiDirectory.absolutePath + "/tabs/overview.html")
        scene.setRoot(browser)

        def bridgePort = SourceTooltipConfig.current.pluginUIPort
        def bridgeFile = new File(uiDirectory.absolutePath + "/source_plugin_bridge.js").toPath()
        def fileContent = new ArrayList<>(Files.readAllLines(bridgeFile, StandardCharsets.UTF_8))
        for (int i = 0; i < fileContent.size(); i++) {
            if (fileContent.get(i) == "var eb = new EventBus('http://localhost:7529/eventbus');") {
                fileContent.set(i, "var eb = new EventBus('http://localhost:$bridgePort/eventbus');")
                break
            }
        }
        Files.write(bridgeFile, fileContent, StandardCharsets.UTF_8)
        return scene
    }

    static void extract(URL dirURL, String sourceDirectory, String writeDirectory) throws IOException {
        final String path = sourceDirectory.substring(1)

        if ((dirURL != null) && dirURL.getProtocol() == "jar") {
            final JarURLConnection jarConnection = (JarURLConnection) dirURL.openConnection()
            final ZipFile jar = jarConnection.getJarFile()
            final Enumeration<? extends ZipEntry> entries = jar.entries()

            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement()
                final String name = entry.getName()
                if (!name.startsWith(path)) {
                    continue
                }

                final String entryTail = name.substring(path.length())
                final File f = new File(writeDirectory + File.separator + entryTail)
                if (entry.isDirectory()) {
                    f.mkdir()
                } else {
                    final InputStream is = jar.getInputStream(entry)
                    final OutputStream os = new BufferedOutputStream(new FileOutputStream(f))
                    final byte[] buffer = new byte[4096]
                    int readCount
                    while ((readCount = is.read(buffer)) > 0) {
                        os.write(buffer, 0, readCount)
                    }
                    os.close()
                    is.close()
                }
                f.deleteOnExit()
            }
        } else if ((dirURL != null) && dirURL.getProtocol() == "file") {
            FileUtils.copyDirectory(new File(dirURL.file), new File(writeDirectory))
        } else if (dirURL == null) {
            throw new IllegalStateException("can't find " + sourceDirectory + " on the classpath")
        } else {
            throw new IllegalStateException("don't know how to handle extracting from " + dirURL)
        }
    }
}
