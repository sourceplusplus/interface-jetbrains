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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import spp.jetbrains.artifact.service.ArtifactScopeService
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.JVMLanguageProvider

@TestDataPath("\$CONTENT_ROOT/testData/scope/")
class JVMVariableScopeTest : BasePlatformTestCase() {

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

    fun testVariableScope() {
        doVariableScope("groovy")
        doVariableScope("java")
        doVariableScope("kt")
    }

    private fun doVariableScope(extension: String) {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".$extension")
        val scopeVariables = ArtifactScopeService.getScopeVariables(psiFile, 7)
        assertEquals(4, scopeVariables.size)
        assertTrue(scopeVariables.any { it == "i" })
        assertTrue(scopeVariables.any { it == "c" })
        assertTrue(scopeVariables.any { it == "s" })
        assertTrue(scopeVariables.any { it == "f" })
    }
}
