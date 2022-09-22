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
import com.intellij.psi.*
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.SourceFileMarker
import spp.protocol.artifact.ArtifactType

@TestDataPath("\$CONTENT_ROOT/testData/naming/")
class JVMArtifactNamingServiceTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        ApplicationManager.getApplication().runReadAction(Computable {
            runBlocking {
                SourceMarker.getInstance(myFixture.project).clearAvailableSourceFileMarkers()
            }
        })

        JVMMarker.setup()
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(PsiJavaFile::class.java)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(KtFile::class.java)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(GroovyFile::class.java)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/naming/"
    }

    fun testJavaClassName() {
        doTestClassName<PsiClass>("java")
    }

    fun testKotlinClassName() {
        doTestClassName<KtClass>("kt")
    }

    fun testGroovyClassName() {
        doTestClassName<PsiClass>("groovy")
    }

    private inline fun <reified T : PsiElement> doTestClassName(extension: String) {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".$extension")
        val clazz = psiFile.findDescendantOfType<T> { true }
        assertNotNull(clazz)

        val name = JVMArtifactNamingService().getFullyQualifiedName(clazz!!)
        assertEquals(getTestName(false) + "", name.identifier)
        assertEquals(ArtifactType.CLASS, name.type)
    }

    fun testJavaMethodName() {
        doTestMethodName<PsiMethod>("java")
    }

    fun testKotlinMethodName() {
        doTestMethodName<KtFunction>("kt")
    }

    fun testGroovyMethodName() {
        doTestMethodName<PsiMethod>("groovy")
    }

    private inline fun <reified T : PsiElement> doTestMethodName(extension: String) {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".$extension")
        val method = psiFile.findDescendantOfType<T> { true }
        assertNotNull(method)

        val name = JVMArtifactNamingService().getFullyQualifiedName(method!!)
        assertEquals(getTestName(false) + ".foo()", name.identifier)
        assertEquals(ArtifactType.METHOD, name.type)
    }

    fun testJavaMethodVariable() {
        doTestMethodVariable("java")
    }

    fun testKotlinMethodVariable() {
        doTestMethodVariable("kt")
    }

    fun testGroovyMethodVariable() {
        doTestMethodVariable("groovy")
    }

    private fun doTestMethodVariable(extension: String) {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".$extension")
        val identifier = psiFile.findDescendantOfType<PsiNameIdentifierOwner> {
            it.identifyingElement?.text == "id"
        }!!.identifyingElement
        assertNotNull(identifier)

        val name = JVMArtifactNamingService().getFullyQualifiedName(identifier!!)
        when (extension) {
            "kt" -> assertEquals(getTestName(false) + ".foo()#aWQ6NTc=", name.identifier)
            "java" -> assertEquals(getTestName(false) + ".foo()#aWQ6NzA=", name.identifier)
            "groovy" -> assertEquals(getTestName(false) + ".foo()#aWQ6NTg=", name.identifier)
            else -> fail("Unknown extension: $extension")
        }
        assertEquals(ArtifactType.EXPRESSION, name.type)
        assertEquals(3, name.lineNumber)
    }

    fun testJavaClassVariable() {
        doTestClassVariable("java")
    }

    fun testKotlinClassVariable() {
        doTestClassVariable("kt")
    }

    fun testGroovyClassVariable() {
        doTestClassVariable("groovy")
    }

    private fun doTestClassVariable(extension: String) {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".$extension")
        val identifier = psiFile.findDescendantOfType<PsiNameIdentifierOwner> {
            it.identifyingElement?.text == "id"
        }!!.identifyingElement
        assertNotNull(identifier)

        val name = JVMArtifactNamingService().getFullyQualifiedName(identifier!!)
        when (extension) {
            "kt" -> assertEquals(getTestName(false) + "#aWQ6MzY=", name.identifier)
            "java" -> assertEquals(getTestName(false) + "#aWQ6NDE=", name.identifier)
            "groovy" -> assertEquals(getTestName(false) + "#aWQ6MzY=", name.identifier)
            else -> fail("Unknown extension: $extension")
        }
        assertEquals(ArtifactType.EXPRESSION, name.type)
        assertEquals(2, name.lineNumber)
    }
}