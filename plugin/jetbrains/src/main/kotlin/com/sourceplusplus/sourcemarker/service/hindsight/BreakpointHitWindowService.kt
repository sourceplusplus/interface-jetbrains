package com.sourceplusplus.sourcemarker.service.hindsight

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
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import com.sourceplusplus.protocol.artifact.debugger.BreakpointHit
import com.sourceplusplus.sourcemarker.service.hindsight.ui.BreakpointHitWindow
import com.sourceplusplus.sourcemarker.service.hindsight.ui.EventsWindow
import com.sourceplusplus.sourcemarker.icons.SourceMarkerIcons

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Service
class BreakpointHitWindowService(private val project: Project) : Disposable {

    companion object {
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
                .registerToolWindow(HindsightConstants.HINDSIGHT_NAME, true, ToolWindowAnchor.BOTTOM, this, true)
            _toolWindow!!.setIcon(SourceMarkerIcons.GREY_EYE_ICON)
        } catch (ignored: Throwable) {
            _toolWindow = ToolWindowManager.getInstance(project) //2020+
                .registerToolWindow(
                    RegisterToolWindowTask.closable(
                        HindsightConstants.HINDSIGHT_NAME,
                        SourceMarkerIcons.GREY_EYE_ICON
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

    fun addBreakpointHit(hit: BreakpointHit) {
        eventsWindow.eventsTab.model.insertRow(0, hit)
    }

    fun showBreakpointHit(hit: BreakpointHit) {
        removeExecutionShower()
        breakpointWindow = BreakpointHitWindow(project, executionPointHighlighter)
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
