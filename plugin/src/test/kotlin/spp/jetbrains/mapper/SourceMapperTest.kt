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
package spp.jetbrains.mapper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import spp.jetbrains.mapper.extend.SourceCodeTokenizer
import spp.jetbrains.marker.source.JVMMarkerUtils

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
                                    JVMMarkerUtils.getFullyQualifiedName(it), tokens
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
