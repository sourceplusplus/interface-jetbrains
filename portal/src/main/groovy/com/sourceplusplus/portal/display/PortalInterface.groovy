package com.sourceplusplus.portal.display

import com.google.common.base.Joiner
import com.intellij.ui.jcef.JBCefBrowser
import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.display.tabs.views.ConfigurationView
import com.sourceplusplus.portal.display.tabs.views.OverviewView
import com.sourceplusplus.portal.display.tabs.views.TracesView
import groovy.util.logging.Slf4j
import io.netty.handler.codec.http.QueryStringDecoder
import io.vertx.core.Vertx
import org.apache.commons.io.FileUtils
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandler
import org.cef.handler.CefLifeSpanHandler
import org.jetbrains.annotations.NotNull

import javax.swing.*
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Used to render the Source++ Portal's Semantic UI HTML files as a JComponent.
 *
 * @version 0.2.3
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PortalInterface {

    public static final String PORTAL_READY = "PortalReady"

    private static File uiDirectory
    private static Vertx vertx
    private final AtomicBoolean portalReady = new AtomicBoolean()
    private final String portalUuid
    private final OverviewView overviewView
    private final TracesView tracesView
    private final ConfigurationView configurationView
    private JBCefBrowser browser
    public String viewingPortalArtifact
    public PortalTab currentTab = PortalTab.Overview
    private Map<String, String> currentQueryParams = [:]
    private static boolean DARK_MODE

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
        queryParams.put("dark_mode", Boolean.toString(DARK_MODE))
        currentQueryParams = new HashMap<>(queryParams)
        def userQuery = Joiner.on("&").withKeyValueSeparator("=").join(queryParams)
        if (userQuery) {
            userQuery = "&$userQuery"
        }

        def page = tab.name().toLowerCase() + ".html"
        browser.cefBrowser.loadURL("file:///" + uiDirectory.absolutePath + "/tabs/$page?portal_uuid=$portalUuid$userQuery")
    }

    OverviewView getOverviewView() {
        return overviewView
    }

    TracesView getTracesView() {
        return tracesView
    }

    JBCefBrowser getBrowser() {
        return browser
    }

    void close() {
        browser.cefBrowser.close(true)
    }

    void reload() {
        if (browser != null) {
            loadPage(currentTab, currentQueryParams)
        }
    }

    @NotNull
    JComponent getUIComponent() {
        return browser.getComponent()
    }

    void cloneViews(PortalInterface portalInterface) {
        this.overviewView.cloneView(portalInterface.overviewView)
        this.tracesView.cloneView(portalInterface.tracesView)
    }

    void initPortal() {
        if (!portalReady.getAndSet(true)) {
            if (uiDirectory == null) {
                createScene()
            }
            browser = new JBCefBrowser("file:///" + uiDirectory.absolutePath + "/tabs/overview.html?portal_uuid=$portalUuid")
            browser.getComponent().setPreferredSize(new Dimension(775, 250))
            browser.getComponent().setSize(775, 250)
//            browser.browser.addConsoleListener({
//                log.info("[PORTAL_CONSOLE] - " + it)
//            })
            browser.cefBrowser.client.addDisplayHandler(new CefDisplayHandler() {
                @Override
                void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                }

                @Override
                void onTitleChange(CefBrowser browser, String title) {
                }

                @Override
                boolean onTooltip(CefBrowser browser, String text) {
                    return false
                }

                @Override
                void onStatusMessage(CefBrowser browser, String value) {
                }

                @Override
                boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                    log.info("[PORTAL_CONSOLE] - " + message)
                    return false
                }
            })

            browser.getJBCefClient().addLifeSpanHandler(new CefLifeSpanHandler() {
                @Override
                boolean onBeforePopup(CefBrowser cefBrowser, CefFrame cefFrame, String targetUrl, String targetFrameName) {
                    def portal = SourcePortal.getPortal(new QueryStringDecoder(
                            targetUrl).parameters().get("portal_uuid").get(0))
                    def browserView = ((JBCefBrowser.MyComponent) browser.getJBCefClient().getCefClient().createBrowser(
                            targetUrl, false, false).getUIComponent().getParent()).getJBCefBrowser()
                    portal.interface.browser = browserView

                    def popupFrame = new JFrame(portal.interface.viewingPortalArtifact)
                    popupFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
                    popupFrame.setPreferredSize(new Dimension(800, 600))
                    popupFrame.add(browserView.getComponent(), BorderLayout.CENTER)
                    popupFrame.pack()
                    popupFrame.setLocationByPlatform(true)
                    popupFrame.setVisible(true)
                    popupFrame.addWindowListener(new WindowAdapter() {
                        @Override
                        void windowClosing(WindowEvent e) {
                            portal.close()
                        }
                    })
                    return true
                }

                @Override
                void onAfterCreated(CefBrowser cefBrowser) {
                    //https://github.com/CodeBrig/Journey/issues/13
                    if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                        new java.util.Timer().schedule(
                                new TimerTask() {
                                    @Override
                                    void run() {
                                        if (browser.getCefBrowser().getZoomLevel() != -1.5d) {
                                            browser.getCefBrowser().setZoomLevel(-1.5)
                                        }
                                    }
                                }, 0, 50
                        )
                    }
                }

                @Override
                void onAfterParentChanged(CefBrowser cefBrowser) {
                }

                @Override
                boolean doClose(CefBrowser cefBrowser) {
                    return false
                }

                @Override
                void onBeforeClose(CefBrowser cefBrowser) {
                }
            }, browser.getCefBrowser())
            vertx.eventBus().publish(PORTAL_READY, portalUuid)
        }
    }

    static void updateTheme(boolean dark) {
        DARK_MODE = dark
        SourcePortal.getPortals().each {
            it.interface.reload()
        }
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
