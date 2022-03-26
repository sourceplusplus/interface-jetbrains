/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.marker.plugin

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.*
import com.intellij.ide.ui.AntialiasingType
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.paint.EffectPainter
import org.joor.Reflect
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarker.getSourceFileMarker
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode.MARK_REMOVED
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkVirtualText
import spp.jetbrains.marker.source.mark.inlay.event.InlayMarkEventCode.*
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("UnstableApiUsage")
abstract class SourceInlayHintProvider : InlayHintsProvider<NoSettings> {

    companion object {
        private val log = LoggerFactory.getLogger(SourceInlayHintProvider::class.java)

        val EVENT_LISTENER = SourceMarkEventListener { event ->
            when (event.eventCode) {
                VIRTUAL_TEXT_UPDATED, INLAY_MARK_VISIBLE, INLAY_MARK_HIDDEN -> {
                    ApplicationManager.getApplication().invokeLater {
                        FileEditorManager.getInstance(event.sourceMark.project).selectedTextEditor?.inlayModel
                            //todo: smaller range
                            ?.getInlineElementsInRange(0, Integer.MAX_VALUE)?.forEach {
                                it.repaint()
                            }
                        FileEditorManager.getInstance(event.sourceMark.project).selectedTextEditor?.inlayModel
                            //todo: smaller range
                            ?.getAfterLineEndElementsInRange(0, Integer.MAX_VALUE)?.forEach {
                                it.repaint()
                            }
                        FileEditorManager.getInstance(event.sourceMark.project).selectedTextEditor?.inlayModel
                            //todo: smaller range
                            ?.getBlockElementsInRange(0, Integer.MAX_VALUE)?.forEach {
                                it.repaint()
                            }
                    }
                }
                MARK_REMOVED -> {
                    ApplicationManager.getApplication().invokeLater {
                        FileEditorManager.getInstance(event.sourceMark.project).selectedTextEditor?.inlayModel
                            //todo: smaller range
                            ?.getBlockElementsInRange(0, Integer.MAX_VALUE)?.forEach {
                                if (it.renderer is BlockInlayRenderer) {
                                    val cachedPresentation = Reflect.on(it.renderer).field("cachedPresentation").get<Any>()
                                    if (cachedPresentation is RecursivelyUpdatingRootPresentation) {
                                        if (cachedPresentation.content is StaticDelegatePresentation) {
                                            val delegatePresentation = cachedPresentation.content as StaticDelegatePresentation
                                            if (delegatePresentation.presentation is DynamicTextInlayPresentation) {
                                                val dynamicPresentation = delegatePresentation.presentation as DynamicTextInlayPresentation
                                                if (dynamicPresentation.inlayMark == event.sourceMark) {
                                                    Disposer.dispose(it)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }

        @Volatile
        @JvmField
        var latestInlayMarkAddedAt: Long = -1L
    }

    override val key: SettingsKey<NoSettings> = SettingsKey("SourceMarker/InlayHints")
    override val name: String = SourceMarker.PLUGIN_NAME
    override val previewText: String? = null
    override val isVisibleInSettings: Boolean = false
    override fun isLanguageSupported(language: Language): Boolean = true
    override fun createSettings(): NoSettings = NoSettings()
    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener): JComponent = JPanel()
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        if (!SourceMarker.enabled) {
            log.warn("SourceMarker is disabled. Skipping inlay hints.")
            return null
        }

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                val fileMarker = getSourceFileMarker(element.containingFile) ?: return true
                var inlayMark = element.getUserData(SourceKey.InlayMark)
                if (inlayMark == null) {
                    if (SourceMarker.configuration.inlayMarkConfiguration.strictlyManualCreation) return true else {
                        inlayMark = createInlayMarkIfNecessary(element) ?: return true
                    }
                }
                if (!fileMarker.containsSourceMark(inlayMark) && !inlayMark.canApply()) return true
                if (!fileMarker.containsSourceMark(inlayMark)) inlayMark.apply()
                if (!inlayMark.isVisible()) {
                    return true
                }

                val virtualText = inlayMark.configuration.virtualText ?: return true
                virtualText.inlayMark = inlayMark

                var representation = AttributesTransformerPresentation(
                    (DynamicTextInlayPresentation(editor, inlayMark, virtualText))
                ) {
                    it.withDefault(editor.colorsScheme.getAttributes(INLINE_PARAMETER_HINT) ?: TextAttributes())
                } as InlayPresentation
                if (inlayMark.configuration.activateOnMouseClick) {
                    representation = factory.onClick(representation, MouseButton.Left) { _: MouseEvent, _: Point ->
                        inlayMark.displayPopup(editor)
                    }
                }

                displayVirtualText(element, virtualText, sink, representation)
                latestInlayMarkAddedAt = System.currentTimeMillis()
                return true
            }
        }
    }

    abstract fun createInlayMarkIfNecessary(element: PsiElement): InlayMark?

    abstract fun displayVirtualText(
        element: PsiElement,
        virtualText: InlayMarkVirtualText,
        sink: InlayHintsSink,
        representation: InlayPresentation
    )

    private inner class DynamicTextInlayPresentation(val editor: Editor, val inlayMark: InlayMark,
                                                     val virtualText: InlayMarkVirtualText) :
        BasePresentation() {

        private val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        override val width: Int
            get() = editor.contentComponent.getFontMetrics(font).stringWidth(virtualText.getRenderedVirtualText())
        override val height = editor.lineHeight

        override fun paint(g: Graphics2D, attributes: TextAttributes) {
            val ascent = editor.ascent
            val descent = (editor as EditorImpl).descent
            val savedHint = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING)
            try {
                var foreground = virtualText.textAttributes.foregroundColor
                if (foreground == null) {
                    foreground = attributes.foregroundColor
                }

                if (virtualText.icon != null) {
                    virtualText.icon!!.paintIcon(null, g, virtualText.iconLocation.x, virtualText.iconLocation.y)
                }
                g.font = virtualText.font ?: font
                g.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    AntialiasingType.getKeyForCurrentScope(false)
                )
                g.color = foreground
                g.drawString(virtualText.getRenderedVirtualText(), virtualText.xOffset, ascent)
                val effectColor = virtualText.textAttributes.effectColor
                if (effectColor != null) {
                    g.color = effectColor
                    when (virtualText.textAttributes.effectType) {
                        EffectType.LINE_UNDERSCORE -> EffectPainter.LINE_UNDERSCORE.paint(
                            g,
                            0,
                            ascent,
                            width,
                            descent,
                            font
                        )
                        EffectType.BOLD_LINE_UNDERSCORE -> EffectPainter.BOLD_LINE_UNDERSCORE.paint(
                            g,
                            0,
                            ascent,
                            width,
                            descent,
                            font
                        )
                        EffectType.STRIKEOUT -> EffectPainter.STRIKE_THROUGH.paint(g, 0, ascent, width, height, font)
                        EffectType.WAVE_UNDERSCORE -> EffectPainter.WAVE_UNDERSCORE.paint(
                            g,
                            0,
                            ascent,
                            width,
                            descent,
                            font
                        )
                        EffectType.BOLD_DOTTED_LINE -> EffectPainter.BOLD_DOTTED_UNDERSCORE.paint(
                            g,
                            0,
                            ascent,
                            width,
                            descent,
                            font
                        )
                        else -> {
                        }
                    }
                }
            } finally {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedHint)
            }
        }

        override fun toString(): String = virtualText.getVirtualText()
    }

    private fun TextAttributes.withDefault(other: TextAttributes): TextAttributes {
        val result = this.clone()
        if (result.foregroundColor == null) {
            result.foregroundColor = other.foregroundColor
        }
        if (result.backgroundColor == null) {
            result.backgroundColor = other.backgroundColor
        }
        if (result.effectType == null) {
            result.effectType = other.effectType
        }
        if (result.effectColor == null) {
            result.effectColor = other.effectColor
        }
        return result
    }
}
