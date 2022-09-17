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
package spp.jetbrains.marker.jvm

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase5
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.impl.SourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark

/*
 * todo: have to call SourceGuideProvider.determineGuideMarks() manually since
 *  otherwise "Read access is allowed from inside read-action (or EDT) only" is thrown.
 */
@TestDataPath("\$CONTENT_ROOT/testData/jvmGuide/")
class JVMGuideProviderTest : LightJavaCodeInsightFixtureTestCase5() {

    @BeforeEach
    fun setup() {
        ApplicationManager.getApplication().runReadAction(Computable {
            runBlocking {
                SourceMarker.getInstance(fixture.project).clearAvailableSourceFileMarkers()
            }
        })

        JVMMarker.setup()
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(PsiJavaFile::class.java)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(KtFile::class.java)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(GroovyFile::class.java)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/jvmGuide/"
    }

    @Test
    fun javaMethod() {
        val psiFile = fixture.configureByFile("javaMethod.java")
        val fileMarker = ApplicationManager.getApplication().runReadAction(Computable {
            val fileMarker = SourceMarker.getInstance(fixture.project).getSourceFileMarker(psiFile)
            SourceGuideProvider.determineGuideMarks(fileMarker!!) //todo: shouldn't need to manually call
            fileMarker
        })
        assertNotNull(fileMarker)

        runBlocking {
            delay(2_500)
        }

        val sourceMarks = fileMarker!!.getSourceMarks()
        assertNotNull(sourceMarks)
        assertEquals(1, sourceMarks.size)

        val methodMarks = sourceMarks.filterIsInstance<MethodGuideMark>()
        assertEquals("javaMethod.foo()", methodMarks[0].artifactQualifiedName.identifier)
    }

    @Test
    fun kotlinMethod() {
        val psiFile = fixture.configureByFile("kotlinMethod.kt")
        val fileMarker = ApplicationManager.getApplication().runReadAction(Computable {
            val fileMarker = SourceMarker.getInstance(fixture.project).getSourceFileMarker(psiFile)
            SourceGuideProvider.determineGuideMarks(fileMarker!!) //todo: shouldn't need to manually call
            fileMarker
        })
        assertNotNull(fileMarker)

        runBlocking {
            delay(2_500)
        }

        val sourceMarks = fileMarker!!.getSourceMarks()
        assertNotNull(sourceMarks)
        assertEquals(1, sourceMarks.size)

        val methodMarks = sourceMarks.filterIsInstance<MethodGuideMark>()
        assertEquals("kotlinMethod.foo()", methodMarks[0].artifactQualifiedName.identifier)
    }

    @Test
    fun groovyMethod() {
        val psiFile = fixture.configureByFile("groovyMethod.groovy")
        val fileMarker = ApplicationManager.getApplication().runReadAction(Computable {
            val fileMarker = SourceMarker.getInstance(fixture.project).getSourceFileMarker(psiFile)
            SourceGuideProvider.determineGuideMarks(fileMarker!!) //todo: shouldn't need to manually call
            fileMarker
        })
        assertNotNull(fileMarker)

        runBlocking {
            delay(2_500)
        }

        val sourceMarks = fileMarker!!.getSourceMarks()
        assertNotNull(sourceMarks)
        assertEquals(1, sourceMarks.size)

        val methodMarks = sourceMarks.filterIsInstance<MethodGuideMark>()
        assertEquals("groovyMethod.foo()", methodMarks[0].artifactQualifiedName.identifier)
    }
}
