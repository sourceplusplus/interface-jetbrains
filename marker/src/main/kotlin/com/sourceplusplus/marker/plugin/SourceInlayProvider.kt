package com.sourceplusplus.marker.plugin

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.AttributesTransformerPresentation
import com.intellij.codeInsight.hints.presentation.BasePresentation
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.ide.ui.AntialiasingType
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.BlockInlayPriority.CODE_VISION
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiStatement
import com.intellij.ui.paint.EffectPainter
import com.sourceplusplus.marker.MarkerUtils
import com.sourceplusplus.marker.source.mark.inlay.InlayMark
import com.sourceplusplus.marker.source.mark.inlay.config.InlayMarkVirtualText
import com.sourceplusplus.marker.source.mark.inlay.event.InlayMarkEventCode
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("UnstableApiUsage")
open class SourceInlayProvider : InlayHintsProvider<NoSettings> {

    companion object {
        @Volatile
        @JvmField
        var latestInlayMarkAddedAt: Long = -1L
        private var currentProject: Project? = null

        init {
            SourceMarkerPlugin.addGlobalSourceMarkEventListener { event ->
                when (event.eventCode) {
                    InlayMarkEventCode.VIRTUAL_TEXT_UPDATED -> {
                        ApplicationManager.getApplication().invokeLater {
                            FileEditorManager.getInstance(event.sourceMark.project).selectedTextEditor?.inlayModel
                                //todo: smaller range
                                ?.getInlineElementsInRange(0, Integer.MAX_VALUE)?.forEach {
                                    it.repaint()
                                }
                            FileEditorManager.getInstance(event.sourceMark.project).selectedTextEditor?.inlayModel
                                //todo: smaller range
                                ?.getBlockElementsInRange(0, Integer.MAX_VALUE)?.forEach {
                                    it.repaint()
                                }
                        }
                    }
                }
            }
        }

        private fun createInlayMarkIfNecessary(element: PsiElement): InlayMark? {
            val parent = element.parent
            if ((parent is PsiMethod && element === parent.nameIdentifier)
                || (parent is GrMethod && element === parent.nameIdentifierGroovy)
                || (parent is KtNamedFunction && element === parent.nameIdentifier)
            ) {
                val fileMarker = SourceMarkerPlugin.getSourceFileMarker(element.containingFile)!!
                currentProject = fileMarker.project

                val artifactQualifiedName = MarkerUtils.getFullyQualifiedName(element.parent.toUElement() as UMethod)
                return if (!SourceMarkerPlugin.configuration.createSourceMarkFilter.test(artifactQualifiedName)) {
                    null
                } else {
                    MarkerUtils.getOrCreateMethodInlayMark(fileMarker, element)
                }
            } else if (element is PsiStatement) {
                val fileMarker = SourceMarkerPlugin.getSourceFileMarker(element.containingFile)!!
                currentProject = fileMarker.project

                val artifactQualifiedName = MarkerUtils.getFullyQualifiedName(
                    MarkerUtils.getUniversalExpression(element).toUElement() as UExpression
                )
                return if (!SourceMarkerPlugin.configuration.createSourceMarkFilter.test(artifactQualifiedName)) {
                    null
                } else {
                    MarkerUtils.getOrCreateExpressionInlayMark(fileMarker, element)
                }
            }
            return null
        }
    }

    override val key: SettingsKey<NoSettings> = SettingsKey("SourceMarker/InlayHints")
    override val name: String = "SourceMarker"
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
        if (!SourceMarkerPlugin.enabled) {
            return null
        }

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                val fileMarker = SourceMarkerPlugin.getSourceFileMarker(element.containingFile) ?: return true
                val inlayMark = createInlayMarkIfNecessary(element) ?: return true
                if (!fileMarker.containsSourceMark(inlayMark) && !inlayMark.canApply()) return true
                if (!fileMarker.containsSourceMark(inlayMark)) inlayMark.apply()
                val virtualText = inlayMark.configuration.virtualText ?: return true
                virtualText.inlayMark = inlayMark

                var representation = AttributesTransformerPresentation(
                    (DynamicTextInlayPresentation(editor, virtualText))
                ) {
                    it.withDefault(editor.colorsScheme.getAttributes(INLINE_PARAMETER_HINT) ?: TextAttributes())
                } as InlayPresentation
                if (inlayMark.configuration.activateOnMouseClick) {
                    representation = factory.onClick(representation, MouseButton.Left) { _: MouseEvent, _: Point ->
                        inlayMark.displayPopup(editor)
                    }
                }

                if (virtualText.useInlinePresentation) {
                    if (virtualText.showAfterLastChildWhenInline) {
                        sink.addInlineElement(
                            element.lastChild.textRange.endOffset,
                            virtualText.relatesToPrecedingText,
                            representation
                        )
                    } else {
                        sink.addInlineElement(
                            element.textRange.startOffset,
                            virtualText.relatesToPrecedingText,
                            representation
                        )
                    }
                } else {
                    if (element.parent is PsiMethod) {
                        virtualText.spacingTillMethodText = element.parent.prevSibling.text
                            .replace("\n", "").count { it == ' ' }
                    }

                    var startOffset = element.textRange.startOffset
                    if (virtualText.showBeforeAnnotationsWhenBlock) {
                        if (element.parent is PsiMethod) {
                            val annotations = (element.parent as PsiMethod).annotations
                            if (annotations.isNotEmpty()) {
                                startOffset = annotations[0].textRange.startOffset
                            }
                        }
                    }
                    sink.addBlockElement(
                        startOffset,
                        virtualText.relatesToPrecedingText,
                        virtualText.showAbove,
                        CODE_VISION,
                        representation
                    )
                }
                latestInlayMarkAddedAt = System.currentTimeMillis()
                return true
            }
        }
    }

    private inner class DynamicTextInlayPresentation(val editor: Editor, val virtualText: InlayMarkVirtualText) :
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
                g.font = font
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, AntialiasingType.getKeyForCurrentScope(false))
                g.color = foreground
                g.drawString(virtualText.getRenderedVirtualText(), 0, ascent)
                val effectColor = virtualText.textAttributes.effectColor
                if (effectColor != null) {
                    g.color = effectColor
                    when (virtualText.textAttributes.effectType) {
                        EffectType.LINE_UNDERSCORE -> EffectPainter.LINE_UNDERSCORE.paint(g, 0, ascent, width, descent, font)
                        EffectType.BOLD_LINE_UNDERSCORE -> EffectPainter.BOLD_LINE_UNDERSCORE.paint(g, 0, ascent, width, descent, font)
                        EffectType.STRIKEOUT -> EffectPainter.STRIKE_THROUGH.paint(g, 0, ascent, width, height, font)
                        EffectType.WAVE_UNDERSCORE -> EffectPainter.WAVE_UNDERSCORE.paint(g, 0, ascent, width, descent, font)
                        EffectType.BOLD_DOTTED_LINE -> EffectPainter.BOLD_DOTTED_UNDERSCORE.paint(g, 0, ascent, width, descent, font)
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
