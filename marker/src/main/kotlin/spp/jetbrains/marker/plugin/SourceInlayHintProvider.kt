/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.marker.plugin

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.*
import com.intellij.ide.ui.AntialiasingType
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
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
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.service.ArtifactMarkService
import spp.jetbrains.marker.source.mark.api.SourceMark
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
class SourceInlayHintProvider : InlayHintsProvider<NoSettings> {

    companion object {
        private val log = logger<SourceInlayHintProvider>()

        val EVENT_LISTENER = SourceMarkEventListener { event ->
            when (event.eventCode) {
                VIRTUAL_TEXT_UPDATED, INLAY_MARK_VISIBLE, INLAY_MARK_HIDDEN -> {
                    ApplicationManager.getApplication().invokeLater {
                        if (event.sourceMark.project.isDisposed) {
                            log.warn("Project is disposed, ignoring event: ${event.eventCode}")
                            return@invokeLater
                        }

                        FileEditorManager.getInstance(event.sourceMark.project).selectedTextEditor?.inlayModel
                            //todo: smaller range
                            ?.getInlineElementsInRange(0, Integer.MAX_VALUE)?.forEach {
                                it.update()
                            }
                        FileEditorManager.getInstance(event.sourceMark.project).selectedTextEditor?.inlayModel
                            //todo: smaller range
                            ?.getAfterLineEndElementsInRange(0, Integer.MAX_VALUE)?.forEach {
                                it.update()
                            }
                        FileEditorManager.getInstance(event.sourceMark.project).selectedTextEditor?.inlayModel
                            //todo: smaller range
                            ?.getBlockElementsInRange(0, Integer.MAX_VALUE)?.forEach {
                                it.update()
                            }

                        InlayHintsPassFactory.forceHintsUpdateOnNextPass()
                    }
                }

                MARK_REMOVED -> {
                    ApplicationManager.getApplication().invokeLater {
                        if (event.sourceMark.project.isDisposed) {
                            log.warn("Project is disposed, ignoring event: ${event.eventCode}")
                            return@invokeLater
                        }

                        FileEditorManager.getInstance(event.sourceMark.project).selectedTextEditor?.inlayModel
                            //todo: smaller range
                            ?.getBlockElementsInRange(0, Integer.MAX_VALUE)?.forEach {
                                disposeInlayIfNecessary(it, event.sourceMark)
                            }

                        InlayHintsPassFactory.forceHintsUpdateOnNextPass()
                    }
                }
            }
        }

        private fun disposeInlayIfNecessary(it: Inlay<*>, sourceMark: SourceMark) {
            if (it.renderer is BlockInlayRenderer) {
                val cachedPresentation = Reflect.on(it.renderer).field("cachedPresentation").get<Any>()
                if (cachedPresentation is RecursivelyUpdatingRootPresentation) {
                    if (cachedPresentation.content is StaticDelegatePresentation) {
                        val delegatePresentation = cachedPresentation.content as StaticDelegatePresentation
                        if (delegatePresentation.presentation is DynamicTextInlayPresentation) {
                            val dynamicPresentation = delegatePresentation.presentation as DynamicTextInlayPresentation
                            if (dynamicPresentation.inlayMark == sourceMark) {
                                Disposer.dispose(it)
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
    ): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                element.getUserData(SourceKey.InlayMarks).orEmpty().forEach { inlayMark ->
                    displayInlayMark(inlayMark, element)
                }
                latestInlayMarkAddedAt = System.currentTimeMillis()
                return true
            }

            private fun displayInlayMark(inlayMark: InlayMark, element: PsiElement) {
                if (!inlayMark.isVisible()) return
                val virtualText = inlayMark.configuration.virtualText ?: return
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

                ArtifactMarkService.displayVirtualText(element, virtualText, sink, representation)
            }
        }
    }

    private inner class DynamicTextInlayPresentation(
        val editor: Editor,
        val inlayMark: InlayMark,
        val virtualText: InlayMarkVirtualText
    ) : BasePresentation() {

        private val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        override val width: Int
            get() = editor.contentComponent.getFontMetrics(font).stringWidth(virtualText.getRenderedVirtualText())
        override val height: Int
            get() {
                if (virtualText.getVirtualText().isEmpty() && virtualText.richText == null) return 0
                return editor.lineHeight
            }

        override fun paint(g: Graphics2D, attributes: TextAttributes) {
            if (!inlayMark.isVisible()) return
            val ascent = editor.ascent
            val descent = (editor as EditorImpl).descent
            val savedHint = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING)
            try {
                var foreground = virtualText.textAttributes.foregroundColor
                if (foreground == null) {
                    foreground = attributes.foregroundColor
                }

                var iconWidth = 0
                if (virtualText.icon != null) {
                    iconWidth = virtualText.icon!!.iconWidth + 4

                    val stringWidth = editor.contentComponent.getFontMetrics(font)
                        .stringWidth(" ".repeat(virtualText.spacingTillMethodText))
                    virtualText.icon!!.paintIcon(
                        null,
                        g,
                        virtualText.iconLocation.x + stringWidth,
                        virtualText.iconLocation.y
                    )
                }
                g.font = (virtualText.font ?: font).let {
                    if (virtualText.fontSize != null) {
                        if (virtualText.relativeFontSize) {
                            it.deriveFont(it.size + virtualText.fontSize!!)
                        } else {
                            it.deriveFont(virtualText.fontSize!!)
                        }
                    } else {
                        it
                    }
                }
                g.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    AntialiasingType.getKeyForCurrentScope(false)
                )
                g.color = foreground

                if (virtualText.richText != null) {
                    var xOffset = editor.contentComponent.getFontMetrics(font)
                        .stringWidth(" ".repeat(virtualText.spacingTillMethodText))
                    virtualText.richText!!.parts.forEach {
                        g.color = it.attributes.fgColor ?: foreground
                        g.drawString(it.text, iconWidth + xOffset, ascent)
                        xOffset += g.fontMetrics.stringWidth(it.text)
                    }
                } else {
                    g.drawString(virtualText.getRenderedVirtualText(), virtualText.xOffset + iconWidth, ascent)
                }

                val effectColor = virtualText.textAttributes.effectColor
                if (effectColor != null) {
                    g.color = effectColor
                    when (virtualText.textAttributes.effectType) {
                        EffectType.LINE_UNDERSCORE -> EffectPainter.LINE_UNDERSCORE
                            .paint(g, 0, ascent, width, descent, font)

                        EffectType.BOLD_LINE_UNDERSCORE -> EffectPainter.BOLD_LINE_UNDERSCORE
                            .paint(g, 0, ascent, width, descent, font)

                        EffectType.STRIKEOUT -> EffectPainter.STRIKE_THROUGH
                            .paint(g, 0, ascent, width, height, font)

                        EffectType.WAVE_UNDERSCORE -> EffectPainter.WAVE_UNDERSCORE
                            .paint(g, 0, ascent, width, descent, font)

                        EffectType.BOLD_DOTTED_LINE -> EffectPainter.BOLD_DOTTED_UNDERSCORE
                            .paint(g, 0, ascent, width, descent, font)

                        else -> Unit
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
