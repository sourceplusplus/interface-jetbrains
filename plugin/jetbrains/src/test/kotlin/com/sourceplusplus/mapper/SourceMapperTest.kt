package com.sourceplusplus.mapper

import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import com.sourceplusplus.mapper.extend.SourceCodeTokenizer
import com.sourceplusplus.marker.source.SourceMarkerUtils
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.junit.Before

abstract class SourceMapperTest : LightPlatformCodeInsightFixture4TestCase() {

    protected lateinit var sourceCodeTokenizer: SourceCodeTokenizer

    @Before
    fun init() {
        sourceCodeTokenizer = object : SourceCodeTokenizer {
            override fun getMethods(filename: String, sourceCode: String): List<SourceCodeTokenizer.TokenizedMethod> {
                val fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(filename)
                val psiFile = PsiFileFactory.getInstance(project).createFileFromText(filename, fileType, sourceCode)

                val result = mutableListOf<SourceCodeTokenizer.TokenizedMethod>()
                val uFile = psiFile.toUElement() as UFile
                uFile.classes.forEach { uClass ->
                    uClass.methods.forEach {
                        val tokens = mutableListOf<String>()
                        it.javaPsi.accept(object : JavaRecursiveElementVisitor() {
                            override fun visitElement(element: PsiElement) {
                                if (element is LeafPsiElement && element.text.isNotBlank()) {
                                    tokens.add(element.text)
                                }
                                super.visitElement(element)
                            }
                        })

                        result.add(
                            SourceCodeTokenizer.TokenizedMethod(
                                ArtifactQualifiedName(
                                    SourceMarkerUtils.getFullyQualifiedName(it),
                                    "",
                                    ArtifactType.METHOD
                                ), tokens
                            )
                        )
                    }
                }
                return result
            }
        }
    }
}
