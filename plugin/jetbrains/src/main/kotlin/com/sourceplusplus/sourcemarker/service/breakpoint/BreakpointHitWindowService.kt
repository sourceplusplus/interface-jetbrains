package com.sourceplusplus.sourcemarker.service.breakpoint

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import com.sourceplusplus.protocol.instrument.breakpoint.event.LiveBreakpointHit
import com.sourceplusplus.protocol.instrument.breakpoint.event.LiveBreakpointRemoved
import com.sourceplusplus.sourcemarker.PluginBundle
import com.sourceplusplus.sourcemarker.icons.SourceMarkerIcons
import com.sourceplusplus.sourcemarker.service.breakpoint.model.LiveBreakpointProperties
import com.sourceplusplus.sourcemarker.service.breakpoint.ui.BreakpointHitWindow
import com.sourceplusplus.sourcemarker.service.breakpoint.ui.EventsWindow
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Service
class BreakpointHitWindowService(private val project: Project) : Disposable {

    companion object {
        private val log = LoggerFactory.getLogger(BreakpointHitWindowService::class.java)

        fun getInstance(project: Project): BreakpointHitWindowService {
            return ServiceManager.getService(project, BreakpointHitWindowService::class.java)
        }
    }

    private val executionPointHighlighter: ExecutionPointHighlighter = ExecutionPointHighlighter(project)
    private var _toolWindow: ToolWindow? = null
    private var contentManager: ContentManager? = null
    private lateinit var breakpointWindow: BreakpointHitWindow
    lateinit var eventsWindow: EventsWindow

    init {
        try {
            _toolWindow = ToolWindowManager.getInstance(project) //2019.3+
                .registerToolWindow(LiveBreakpointConstants.LIVE_BREAKPOINT_NAME, true, ToolWindowAnchor.BOTTOM, this, true)
            _toolWindow!!.setIcon(SourceMarkerIcons.LIVE_BREAKPOINT_DISABLED_ICON)
        } catch (ignored: Throwable) {
            _toolWindow = ToolWindowManager.getInstance(project) //2020+
                .registerToolWindow(
                    RegisterToolWindowTask.closable(
                        LiveBreakpointConstants.LIVE_BREAKPOINT_NAME,
                        SourceMarkerIcons.LIVE_BREAKPOINT_DISABLED_ICON
                    )
                )
        }

        _toolWindow!!.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun contentAdded(contentManagerEvent: ContentManagerEvent) {}
            override fun contentRemoved(event: ContentManagerEvent) {
                if (_toolWindow!!.contentManager.contentCount == 0) {
                    _toolWindow!!.setAvailable(false, null)
                }
            }

            override fun contentRemoveQuery(contentManagerEvent: ContentManagerEvent) {}
            override fun selectionChanged(event: ContentManagerEvent) {
                val disposable = event.content.disposer
                if (disposable is BreakpointHitWindow) {
                    if (event.operation == ContentManagerEvent.ContentOperation.add) {
                        disposable.showExecutionLine()
                    }
                }
            }
        })
        contentManager = _toolWindow!!.contentManager
    }

    fun showEventsWindow() {
        eventsWindow = EventsWindow(project)
        val content = ContentFactory.SERVICE.getInstance().createContent(eventsWindow.layoutComponent, "Events", true)
        content.setDisposer(eventsWindow)
        content.isCloseable = false
        contentManager!!.addContent(content)
    }

    fun processRemoveBreakpoint(bpr: LiveBreakpointRemoved) {
        XDebuggerManager.getInstance(project).breakpointManager.allBreakpoints.forEach {
            if (it.type.id == "live-breakpoint") {
                val props = (it.properties as LiveBreakpointProperties?)
                if (bpr.breakpointId == props?.getBreakpointId()) {
                    props.setFinished(true)
                    props.setActive(false)

                    if (bpr.cause == null) {
                        XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                            it as XLineBreakpoint<*>, SourceMarkerIcons.LIVE_BREAKPOINT_COMPLETE_ICON, null
                        )
                    } else if (bpr.cause != null) {
                        XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                            it as XLineBreakpoint<*>, SourceMarkerIcons.LIVE_BREAKPOINT_ERROR_ICON, null
                        )

                        log.warn("Breakpoint failed: " + bpr.cause!!.message)
                        Notifications.Bus.notify(
                            Notification(
                                PluginBundle.message("plugin_name"), "Live breakpoint failed",
                                "Breakpoint failed: " + bpr.cause!!.message,
                                NotificationType.ERROR
                            )
                        )
                    }
                }
            }
        }
    }

    fun clearContent() {
        contentManager!!.removeAllContents(true)
    }

    fun addBreakpointHit(hit: LiveBreakpointHit) {
        eventsWindow.eventsTab.model.insertRow(0, hit)
        val max = 1000
        if (eventsWindow.eventsTab.model.items.size > max) {
            eventsWindow.eventsTab.model.items.removeAt(0)
        }
    }

    fun showBreakpointHit(hit: LiveBreakpointHit, showExecutionPoint: Boolean = true) {
        if (showExecutionPoint) removeExecutionShower()
        breakpointWindow = BreakpointHitWindow(project, executionPointHighlighter, showExecutionPoint)
        breakpointWindow.showFrames(hit.stackTrace, hit.stackTrace.first())
        val content = ContentFactory.SERVICE.getInstance().createContent(
            breakpointWindow.layoutComponent, hit.stackTrace.first().source + " at #0", false
        )
        content.setDisposer(breakpointWindow)
        breakpointWindow.content = content
        contentManager!!.addContent(content)
        contentManager!!.setSelectedContent(content)
        _toolWindow!!.setAvailable(true, null)
        if (_toolWindow!!.isActive) {
            _toolWindow!!.show(null)
        } else {
            _toolWindow!!.activate(null)
        }
    }

    fun removeExecutionShower() {
        executionPointHighlighter.hide()
    }

    fun getBreakpointWindow(): BreakpointHitWindow? {
        return if (::breakpointWindow.isInitialized) {
            breakpointWindow
        } else {
            null
        }
    }

    override fun dispose() {}
}
