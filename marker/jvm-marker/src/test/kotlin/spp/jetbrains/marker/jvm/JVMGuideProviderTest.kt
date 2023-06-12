/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.marker.jvm

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import spp.jetbrains.UserData
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.service.SourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.guide.ClassGuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark

@TestDataPath("\$CONTENT_ROOT/testData/jvmGuide/")
class JVMGuideProviderTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        ApplicationManager.getApplication().runReadAction(Computable {
            runBlocking {
                SourceMarker.getInstance(myFixture.project).clearAvailableSourceFileMarkers()
            }
        })

        UserData.vertx(project, Vertx.vertx())
        JVMLanguageProvider().setup(project)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(PsiJavaFile::class.java)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(KtFile::class.java)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(GroovyFile::class.java)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/jvmGuide/"
    }

    fun testJavaMethod() {
        doTest("java")
    }

    fun testKotlinMethod() {
        doTest("kt")
    }

    fun testGroovyMethod() {
        doTest("groovy")
    }

    private fun doTest(extension: String) {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".$extension")
        val fileMarker = SourceMarker.getSourceFileMarker(psiFile)
        assertNotNull(fileMarker)

        SourceGuideProvider.getProvider(fileMarker!!.psiFile.language)?.determineGuideMarks(fileMarker)

        val sourceMarks = fileMarker.getSourceMarks()
        assertNotNull(sourceMarks)
        assertEquals(2, sourceMarks.size)

        val clazzMarks = sourceMarks.filterIsInstance<ClassGuideMark>()
        assertEquals(1, clazzMarks.size)
        assertEquals(getTestName(false), clazzMarks[0].artifactQualifiedName.identifier)

        val methodMarks = sourceMarks.filterIsInstance<MethodGuideMark>()
        assertEquals(1, methodMarks.size)
        assertEquals("${getTestName(false)}.foo()", methodMarks[0].artifactQualifiedName.identifier)
    }
}
