package com.sourceplusplus.sourcemarker.command

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.SourceMarkerUtils
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.component.swing.SwingSourceMarkComponentProvider
import com.sourceplusplus.marker.source.mark.inlay.InlayMark
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.sourcemarker.CommandBar
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys
import com.sourceplusplus.sourcemarker.status.LiveLogStatusManager
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object CommandBarController {

    private val log = LoggerFactory.getLogger(CommandBarController::class.java)
    private var previousCommandBar: InlayMark? = null

    fun handleCommandInput(input: String, editor: Editor) {
        log.info("Processing command input: {}", input)
        if (input == "/add-live-log") {
            //replace command inlay with log status inlay
            previousCommandBar!!.dispose()
            LiveLogStatusManager.showStatusBar(editor, previousCommandBar!!.lineNumber)
            previousCommandBar = null
        }
    }

    fun showCommandBar(editor: Editor, lineNumber: Int) {
        //close previous command bar (if open)
        previousCommandBar?.dispose(true, false)
        previousCommandBar = null

        //determine command bar location
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: {}", editor.document)
            return
        }

        val findInlayMark = SourceMarkerUtils.getOrCreateExpressionInlayMark(fileMarker, lineNumber)
        if (findInlayMark.isPresent) {
            val inlayMark = findInlayMark.get()
            if (!fileMarker.containsSourceMark(inlayMark)) {
                //create and display command bar
                previousCommandBar = inlayMark

                val wrapperPanel = JPanel()
                wrapperPanel.layout = BorderLayout()
                val statusBar = CommandBar(inlayMark)
                wrapperPanel.add(statusBar)
                statusBar.setEditor(editor)

                inlayMark.configuration.showComponentInlay = true
                inlayMark.configuration.componentProvider = object : SwingSourceMarkComponentProvider() {
                    override fun makeSwingComponent(sourceMark: SourceMark): JComponent = wrapperPanel
                }
                inlayMark.apply()

                val sourcePortal = inlayMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
                sourcePortal.configuration.currentPage = PageType.LOGS
                sourcePortal.configuration.statusBar = true

                statusBar.focus()
            }
        } else {
            log.warn("No detected expression at line {}. Inlay mark ignored", lineNumber)
        }
    }
}
