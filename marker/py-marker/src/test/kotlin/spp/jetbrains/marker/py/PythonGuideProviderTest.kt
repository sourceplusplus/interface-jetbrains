/*
 * Source++, the open-source live coding platform.
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
package spp.jetbrains.marker.py

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.python.psi.PyFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark

@TestDataPath("\$CONTENT_ROOT/testData/pythonGuide/")
class PythonGuideProviderTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        ApplicationManager.getApplication().runReadAction(Computable {
            runBlocking {
                SourceMarker.getInstance(myFixture.project).clearAvailableSourceFileMarkers()
            }
        })

        PythonLanguageMarker.setup()
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(PyFile::class.java)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/pythonGuide/"
    }

    fun testPythonMethod() {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".py")
        val fileMarker = SourceMarker.getSourceFileMarker(psiFile)
        assertNotNull(fileMarker)

        runBlocking {
            delay(1_500)
        }

        val sourceMarks = fileMarker!!.getSourceMarks()
        assertNotNull(sourceMarks)
        assertEquals(1, sourceMarks.size)

        val methodMarks = sourceMarks.filterIsInstance<MethodGuideMark>()
        assertEquals("${getTestName(false)}.foo()", methodMarks[0].artifactQualifiedName.identifier)
    }
}
