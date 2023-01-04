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
package spp.jetbrains.marker.js.detect.endpoint

import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.js.JavascriptGuideProvider
import spp.jetbrains.marker.js.service.JavascriptArtifactNamingService
import spp.jetbrains.marker.service.ArtifactNamingService
import spp.jetbrains.marker.service.SourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.info.EndpointDetector

@TestDataPath("\$CONTENT_ROOT/testData/endpoint/express")
class ExpressEndpointTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        ApplicationManager.getApplication().runReadAction(Computable {
            runBlocking {
                SourceMarker.getInstance(myFixture.project).clearAvailableSourceFileMarkers()
            }
        })

        SourceFileMarker.SUPPORTED_FILE_TYPES.add(JSFile::class.java)
        ArtifactNamingService.addService(JavascriptArtifactNamingService(), SourceMarkerUtils.getJavaScriptLanguages())
        SourceGuideProvider.addProvider(JavascriptGuideProvider(), SourceMarkerUtils.getJavaScriptLanguages())
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/endpoint/express"
    }

    fun testExpressVariableRouter(): Unit = runBlocking {
        doTest()
    }

    fun testExpressDirectRouter(): Unit = runBlocking {
        doTest()
    }

    fun testExpressMultipleRouter(): Unit = runBlocking {
        myFixture.configureByFile("ExpressMultipleRouter.js")
        val testEndpointFile = myFixture.configureByFile("test-endpoint.js")
        val fileMarker = SourceMarker.getInstance(project).getSourceFileMarker(testEndpointFile)
        assertNotNull(fileMarker)

        SourceGuideProvider.determineGuideMarks(fileMarker!!)

        val endpointGuideMark = fileMarker.getGuideMarks().find { it.lineNumber == 4 }
        assertNotNull(endpointGuideMark)

        val detectedEndpoints = ExpressEndpoint().detectEndpointNames(endpointGuideMark!!).await()
        assertEquals(2, detectedEndpoints.size)
        assertTrue(detectedEndpoints.any { it.name == "/test/hello-world" })
        assertTrue(detectedEndpoints.any { it.name == "/test2/hello-world" })
    }

    fun testExpressAllEndpoint(): Unit = runBlocking {
        myFixture.configureByFile("ExpressAllRouter.js")
        val testEndpointFile = myFixture.configureByFile("all-endpoint.js")
        val fileMarker = SourceMarker.getInstance(project).getSourceFileMarker(testEndpointFile)
        assertNotNull(fileMarker)

        SourceGuideProvider.determineGuideMarks(fileMarker!!)

        val endpointGuideMark = fileMarker.getGuideMarks().find { it.lineNumber == 4 }
        assertNotNull(endpointGuideMark)

        val detectedEndpoints = ExpressEndpoint().detectEndpointNames(endpointGuideMark!!).await()
        assertEquals(16, detectedEndpoints.size)
        EndpointDetector.httpMethods.forEach { httpType ->
            assertTrue(detectedEndpoints.any { it.name == "/test/hello-world" && it.type == httpType })
            assertTrue(detectedEndpoints.any { it.name == "/test2/hello-world" && it.type == httpType })
        }
    }

    private suspend fun doTest() {
        myFixture.configureByFile(getTestName(false) + ".js")
        val testEndpointFile = myFixture.configureByFile("test-endpoint.js")
        val fileMarker = SourceMarker.getInstance(project).getSourceFileMarker(testEndpointFile)
        assertNotNull(fileMarker)

        SourceGuideProvider.determineGuideMarks(fileMarker!!)

        val endpointGuideMark = fileMarker.getGuideMarks().find { it.lineNumber == 4 }
        assertNotNull(endpointGuideMark)

        val detectedEndpoint = ExpressEndpoint().detectEndpointNames(endpointGuideMark!!).await()
        assertEquals(1, detectedEndpoint.size)
        assertEquals("/test/hello-world", detectedEndpoint[0].name)
    }
}
