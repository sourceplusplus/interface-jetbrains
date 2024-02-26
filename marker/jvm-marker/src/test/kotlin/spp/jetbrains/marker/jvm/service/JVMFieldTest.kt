/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.marker.jvm.service

import com.google.common.base.CaseFormat
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiNamedElement
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import spp.jetbrains.artifact.service.getFields
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.source.SourceFileMarker

@TestDataPath("\$CONTENT_ROOT/testData/field/")
class JVMFieldTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        ApplicationManager.getApplication().runReadAction(Computable {
            runBlocking {
                SourceMarker.getInstance(myFixture.project).clearAvailableSourceFileMarkers()
            }
        })

        JVMLanguageProvider().setup(project)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(PsiJavaFile::class.java)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(KtFile::class.java)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(GroovyFile::class.java)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/field/"
    }

    fun testSingleField() {
        doTest<PsiClass>("java")
        doTest<KtClass>("kt")
        doTest<PsiClass>("groovy")
    }

    fun testFieldAndFunction() {
        doTest<PsiClass>("java")
        doTest<KtClass>("kt")
        doTest<PsiClass>("groovy")
    }

    private inline fun <reified T : PsiElement> doTest(extension: String) {
        val className = getTestName(false)
        val testDir = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert(className)
        val psiFile = myFixture.configureByFile("$testDir/$className.$extension")
        val clazz = psiFile.findDescendantOfType<T> { true }
        assertNotNull(clazz)

        val fields = clazz!!.getFields()
        assertEquals(1, fields.size)
        assertEquals("foo", (fields[0] as PsiNamedElement).name)
    }
}
