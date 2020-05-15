package com.sourceplusplus.portal.display

import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.portal.display.tabs.views.ConfigurationView
import com.sourceplusplus.portal.display.tabs.views.OverviewView
import com.sourceplusplus.portal.display.tabs.views.TracesView
import groovy.util.logging.Slf4j
import io.vertx.core.Vertx
import org.apache.commons.io.FileUtils

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Used to render the Source++ Portal's Semantic UI.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PortalUI {

    public static final String PORTAL_READY = "PortalReady"

    private static File _uiDirectory
    private static Vertx vertx
    protected final String portalUuid
    private final OverviewView overviewView
    private final TracesView tracesView
    private final ConfigurationView configurationView
    public String viewingPortalArtifact
    public PortalTab currentTab = PortalTab.Overview

    PortalUI(String portalUuid) {
        this.portalUuid = portalUuid
        this.overviewView = new OverviewView(this)
        this.tracesView = new TracesView(this)
        this.configurationView = new ConfigurationView()
    }

    OverviewView getOverviewView() {
        return overviewView
    }

    TracesView getTracesView() {
        return tracesView
    }

    ConfigurationView getConfigurationView() {
        return configurationView
    }

    void cloneUI(PortalUI portalUI) {
        this.overviewView.cloneView(portalUI.overviewView)
        this.tracesView.cloneView(portalUI.tracesView)
        this.configurationView.cloneView(portalUI.configurationView)
    }

    static void assignVertx(Vertx vertx) {
        this.vertx = vertx
    }

    static String getPortalUrl(PortalTab tab, String portalUuid) {
        return getPortalUrl(tab, portalUuid, "")
    }

    static String getPortalUrl(PortalTab tab, String portalUuid, String userQuery) {
        return "file:///" + uiDirectory + "/tabs/" + tab.name().toLowerCase() + ".html?portal_uuid=$portalUuid$userQuery"
    }

    private static String getUiDirectory() {
        if (_uiDirectory == null) {
            createScene()
            vertx.eventBus().publish(PORTAL_READY, true)
        }
        return _uiDirectory
    }

    private static void createScene() {
        _uiDirectory = File.createTempDir()
        _uiDirectory.deleteOnExit()

        def url = PortalUI.class.getResource("/ui")
        extract(url, "/ui", _uiDirectory.absolutePath)
        log.debug("Using portal ui directory: " + _uiDirectory.absolutePath)

        def bridgePort = SourcePortalConfig.current.pluginUIPort
        def bridgeFile = new File(_uiDirectory.absolutePath + "/source_eventbus_bridge.js").toPath()
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
