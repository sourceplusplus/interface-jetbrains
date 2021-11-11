package com.sourceplusplus.mapper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.sourceplusplus.mapper.extend.SourceCodeTokenizer
import com.sourceplusplus.marker.source.JVMMarkerUtils
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class SourceMapperTest : LightJavaCodeInsightFixtureTestCase() {

    protected lateinit var sourceCodeTokenizer: SourceCodeTokenizer

    @BeforeEach
    fun init() {
        super.setUp()

        sourceCodeTokenizer = object : SourceCodeTokenizer {
            override fun getMethods(
                filename: String,
                sourceCode: String
            ): List<SourceCodeTokenizer.TokenizedMethod> {
                val result = mutableListOf<SourceCodeTokenizer.TokenizedMethod>()
                ApplicationManager.getApplication().runReadAction {
                    val fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(filename)
                    val psiFile = PsiFileFactory.getInstance(project).createFileFromText(filename, fileType, sourceCode)
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
                                        JVMMarkerUtils.getFullyQualifiedName(it),
                                        "",
                                        ArtifactType.METHOD
                                    ), tokens
                                )
                            )
                        }
                    }
                }
                return result
            }
        }
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        TestApplicationManager.getInstance().setDataProvider(null)
    }
}
