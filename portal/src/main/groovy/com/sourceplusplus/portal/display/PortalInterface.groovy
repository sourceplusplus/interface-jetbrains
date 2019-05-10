package com.sourceplusplus.portal.display

import com.google.common.base.Joiner
import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.portal.display.tabs.views.ConfigurationView
import com.sourceplusplus.portal.display.tabs.views.OverviewView
import com.sourceplusplus.portal.display.tabs.views.TracesView
import com.teamdev.jxbrowser.chromium.swing.BrowserView
import io.vertx.core.Vertx
import org.apache.commons.io.FileUtils
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*
import java.awt.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Used to render the Source++ Portal's Semantic UI HTML files as a JFXPanel.
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class PortalInterface {

    public static final String PORTAL_READY = "PortalReady"

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private static File uiDirectory
    private static Vertx vertx
    private final AtomicBoolean portalReady = new AtomicBoolean()
    private final String portalUuid
    private final OverviewView overviewView
    private final TracesView tracesView
    private final ConfigurationView configurationView
    private BrowserView browser
    public String viewingPortalArtifact
    public PortalTab currentTab = PortalTab.Overview

    PortalInterface(String portalUuid) {
        this.portalUuid = portalUuid
        this.overviewView = new OverviewView(this)
        this.tracesView = new TracesView(this)
        this.configurationView = new ConfigurationView()
    }

    void loadPage(PortalTab tab) {
        loadPage(tab, [:])
    }

    void loadPage(PortalTab tab, Map<String, String> queryParams) {
        def userQuery = Joiner.on("&").withKeyValueSeparator("=").join(queryParams)
        if (userQuery) {
            userQuery = "&$userQuery"
        }

        def page = tab.name().toLowerCase() + ".html"
        browser.browser.loadURL("file:///" + uiDirectory.absolutePath + "/tabs/$page?portal_uuid=$portalUuid$userQuery")
    }

    OverviewView getOverviewView() {
        return overviewView
    }

    TracesView getTracesView() {
        return tracesView
    }

    void close() {
        browser.browser.dispose()
    }

    @NotNull
    JComponent getUIComponent() {
        return browser
    }

    void cloneViews(PortalInterface portalInterface) {
        this.overviewView.cloneView(portalInterface.overviewView)
        this.tracesView.cloneView(portalInterface.tracesView)
    }

    void initPortal() {
        if (!portalReady.getAndSet(true)) {
            browser = new BrowserView()
            browser.setPreferredSize(new Dimension(775, 250))
            browser.browser.setSize(775, 250)
            browser.browser.addConsoleListener({
                log.info("[PORTAL_CONSOLE] - " + it)
            })

            if (uiDirectory == null) {
                createScene()
            }
            browser.browser.loadURL("file:///" + uiDirectory.absolutePath + "/tabs/overview.html?portal_uuid=$portalUuid")
            vertx.eventBus().publish(PORTAL_READY, portalUuid)
        }
    }

    static void updateTheme(boolean dark) {
//        if (dark) {
//            def tabsFolder = new File(uiDirectory.absolutePath, "tabs")
//            tabsFolder.eachFile(FileType.FILES, {
//                def updatedHtml = it.text
//                        .replace('<!--<script src="../themes/default/assets/charts/gray.js"></script>-->', '<script src="../themes/default/assets/charts/gray.js"></script>')
//                        .replace("semantic.min.css", "semantic_dark.min.css")
//                it.text = ""
//                it << updatedHtml
//            })
//        } else {
//            def tabsFolder = new File(uiDirectory.absolutePath, "tabs")
//            tabsFolder.eachFile(FileType.FILES, {
//                def updatedHtml = it.text
//                        .replace('<script src="../themes/default/assets/charts/gray.js"></script>', '<!--<script src="../themes/default/assets/charts/gray.js"></script>-->')
//                        .replace("semantic_dark.min.css", "semantic.min.css")
//                it.text = ""
//                it << updatedHtml
//            })
//        }
//        view.browser.reload()
    }

    static void assignVertx(Vertx vertx) {
        this.vertx = vertx
    }

    private static void createScene() {
        uiDirectory = File.createTempDir()
        uiDirectory.deleteOnExit()

        def url = PortalInterface.class.getResource("/ui")
        extract(url, "/ui", uiDirectory.absolutePath)
        log.debug("Using portal ui directory: " + uiDirectory.absolutePath)

        def bridgePort = SourcePortalConfig.current.pluginUIPort
        def bridgeFile = new File(uiDirectory.absolutePath + "/source_eventbus_bridge.js").toPath()
        def fileContent = new ArrayList<>(Files.readAllLines(bridgeFile, StandardCharsets.UTF_8))
        for (int i = 0; i < fileContent.size(); i++) {
            if (fileContent.get(i) == "var eb = new EventBus('http://localhost:7529/eventbus');") {
                fileContent.set(i, "var eb = new EventBus('http://localhost:$bridgePort/eventbus');")
                break
            }
        }
        Files.write(bridgeFile, fileContent, StandardCharsets.UTF_8)
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
