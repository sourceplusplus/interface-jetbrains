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
package spp.jetbrains.marker.jvm.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.descendantsOfType
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import spp.jetbrains.artifact.service.ArtifactScopeService
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.JVMLanguageProvider

@TestDataPath("\$CONTENT_ROOT/testData/scope/")
class JVMArtifactScopeServiceTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        ApplicationManager.getApplication().runReadAction(Computable {
            runBlocking {
                SourceMarker.getInstance(myFixture.project).clearAvailableSourceFileMarkers()
            }
        })

        JVMLanguageProvider().setup(project)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/scope/"
    }

    fun testCalledFunctions() {
        doCalledFunctions("groovy")
        doCalledFunctions("java")
        doCalledFunctions("kt")
    }

    private fun doCalledFunctions(extension: String) {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".$extension")
        val callerFunction = psiFile.descendantsOfType<PsiNameIdentifierOwner>().find {
            it.name == "callerFunction"
        }
        assertNotNull(callerFunction)

        val directCalledFunctions = ArtifactScopeService.getCalledFunctions(callerFunction!!)
        assertEquals(1, directCalledFunctions.size)
        assertEquals("directCalledFunction", directCalledFunctions.first().name)

        val internalCalledFunctions = ArtifactScopeService.getCalledFunctions(
            callerFunction, includeIndirect = true
        )
        assertEquals(2, internalCalledFunctions.size)
        assertEquals("directCalledFunction", internalCalledFunctions.first().name)
        assertEquals("indirectCalledFunction", internalCalledFunctions.last().name)

        //todo: this
//        val allCalledFunctions = ArtifactScopeService.getCalledFunctions(
//            callerFunction, includeExternal = true, includeIndirect = true
//        )
//        assertEquals(3, allCalledFunctions.size)
//        assertEquals("directCalledFunction", allCalledFunctions.first().name)
//        assertEquals("indirectCalledFunction", allCalledFunctions[1].name)
//        assertEquals("println", allCalledFunctions.last().name)
    }

    fun testCallerFunctions() {
        doCallerFunctions("groovy")
        doCallerFunctions("java")
        doCallerFunctions("kt")
    }

    private fun doCallerFunctions(extension: String) {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".$extension")
        val indirectCalledFunction = psiFile.descendantsOfType<PsiNameIdentifierOwner>().find {
            it.name == "indirectCalledFunction"
        }
        assertNotNull(indirectCalledFunction)

        val directCallerFunctions = ArtifactScopeService.getCallerFunctions(indirectCalledFunction!!)
        assertEquals(1, directCallerFunctions.size)
        assertEquals("directCalledFunction", directCallerFunctions.first().name)

        //todo: this
//        val internalCallerFunctions = ArtifactScopeService.getCallerFunctions(
//            indirectCalledFunction, includeIndirect = true
//        )
//        assertEquals(2, internalCallerFunctions.size)
//        assertEquals("directCalledFunction", internalCallerFunctions.first().name)
//        assertEquals("callerFunction", internalCallerFunctions.last().name)
    }
}
