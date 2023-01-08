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

import com.google.common.base.CaseFormat
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.*
import com.intellij.psi.util.descendants
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.service.JVMArtifactNamingService
import spp.jetbrains.marker.jvm.service.utils.JVMMarkerUtils
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

        JVMLanguageProvider().setup(project)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(PsiJavaFile::class.java)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(KtFile::class.java)
        SourceFileMarker.SUPPORTED_FILE_TYPES.add(GroovyFile::class.java)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/naming/"
    }

    fun testClassName() {
        doTestClassName<PsiClass>("java")
        doTestClassName<KtClass>("kt")
        doTestClassName<PsiClass>("groovy")
    }

    private inline fun <reified T : PsiElement> doTestClassName(extension: String) {
        val className = getTestName(false)
        val testDir = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert(className)
        val psiFile = myFixture.configureByFile("$testDir/$className.$extension")
        val clazz = psiFile.findDescendantOfType<T> { true }
        assertNotNull(clazz)

        val name = JVMArtifactNamingService().getFullyQualifiedName(clazz!!)
        assertEquals(getTestName(false) + "", name.identifier)
        assertEquals(ArtifactType.CLASS, name.type)
        assertNotNull(name.lineNumber)
    }

    fun testKotlinExpectOpenClassName() {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".kt")
        val clazz = psiFile.findDescendantOfType<KtClass> { true }
        assertNotNull(clazz)

        val name = JVMMarkerUtils.getFullyQualifiedName(clazz!!)
        assertEquals(getTestName(false) + "", name.identifier)
        assertEquals(ArtifactType.CLASS, name.type)
        assertNotNull(name.lineNumber)
    }

    fun testInnerClassName() {
        doTestInnerClassName<PsiClass>("java")
        doTestInnerClassName<KtClass>("kt")
        doTestInnerClassName<PsiClass>("groovy")
    }

    private inline fun <reified T : PsiElement> doTestInnerClassName(extension: String) {
        val className = getTestName(false)
        val testDir = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert(className)
        val psiFile = myFixture.configureByFile("$testDir/$className.$extension")
        val clazz = psiFile.findDescendantOfType<T> { true }
        assertNotNull(clazz)

        val parentName = JVMArtifactNamingService().getFullyQualifiedName(clazz!!)
        assertEquals(getTestName(false) + "", parentName.identifier)
        assertEquals(ArtifactType.CLASS, parentName.type)
        assertNotNull(parentName.lineNumber)

        val innerClazz = clazz.findDescendantOfType<T> { it !== clazz }
        assertNotNull(innerClazz)

        val innerName = JVMArtifactNamingService().getFullyQualifiedName(innerClazz!!)
        assertEquals(getTestName(false) + "\$ClassName", innerName.identifier)
        assertEquals(ArtifactType.CLASS, innerName.type)
        assertNotNull(innerName.lineNumber)
    }

    fun testMethodName() {
        doTestMethodName<PsiMethod>("java")
        doTestMethodName<KtFunction>("kt")
        doTestMethodName<PsiMethod>("groovy")
    }

    private inline fun <reified T : PsiElement> doTestMethodName(extension: String) {
        val className = getTestName(false)
        val testDir = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert(className)
        val psiFile = myFixture.configureByFile("$testDir/$className.$extension")
        val methods = psiFile.descendants().filterIsInstance<T>().toList()
        assertEquals(5, methods.size)

        val foo1 = methods[0]
        val name = JVMArtifactNamingService().getFullyQualifiedName(foo1)
        assertEquals(getTestName(false) + ".foo1()", name.identifier)
        assertEquals(ArtifactType.METHOD, name.type)
        assertNotNull(name.lineNumber)

        val foo2 = methods[1]
        val name2 = JVMArtifactNamingService().getFullyQualifiedName(foo2)
        assertEquals(getTestName(false) + ".foo2(java.lang.String)", name2.identifier)
        assertEquals(ArtifactType.METHOD, name2.type)
        assertNotNull(name2.lineNumber)

        val foo3 = methods[2]
        val name3 = JVMArtifactNamingService().getFullyQualifiedName(foo3)
        assertEquals(
            getTestName(false) + ".foo3(java.lang.String,int,long,double,float,boolean,char,byte,short)",
            name3.identifier
        )
        assertEquals(ArtifactType.METHOD, name3.type)
        assertNotNull(name3.lineNumber)

        val foo4 = methods[3]
        val name4 = JVMArtifactNamingService().getFullyQualifiedName(foo4)
        assertEquals(getTestName(false) + ".foo4($className\$MyObject)", name4.identifier)
        assertEquals(ArtifactType.METHOD, name4.type)
        assertNotNull(name4.lineNumber)

        val foo5 = methods[4]
        val name5 = JVMArtifactNamingService().getFullyQualifiedName(foo5)
        assertEquals(
            getTestName(false) + ".foo5(java.lang.String[],int[],long[],double[],float[],boolean[],char[],byte[],short[])",
            name5.identifier
        )
        assertEquals(ArtifactType.METHOD, name5.type)
        assertNotNull(name5.lineNumber)
    }

    fun testInnerClassMethodName() {
        doTestInnerClassMethodName<PsiMethod>("java")
        doTestInnerClassMethodName<KtFunction>("kt")
        doTestInnerClassMethodName<PsiMethod>("groovy")
    }

    private inline fun <reified T : PsiElement> doTestInnerClassMethodName(extension: String) {
        val className = getTestName(false)
        val testDir = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert(className)
        val psiFile = myFixture.configureByFile("$testDir/$className.$extension")
        val method = psiFile.findDescendantOfType<T> { true }
        assertNotNull(method)

        val name = JVMArtifactNamingService().getFullyQualifiedName(method!!)
        assertEquals(getTestName(false) + "\$ClassName.foo()", name.identifier)
        assertEquals(ArtifactType.METHOD, name.type)
        assertNotNull(name.lineNumber)
    }

    fun testMethodVariable() {
        doTestMethodVariable("java")
        doTestMethodVariable("kt")
        doTestMethodVariable("groovy")
    }

    private fun doTestMethodVariable(extension: String) {
        val className = getTestName(false)
        val testDir = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert(className)
        val psiFile = myFixture.configureByFile("$testDir/$className.$extension")
        val identifier = psiFile.findDescendantOfType<PsiNameIdentifierOwner> {
            it.identifyingElement?.text == "id"
        }!!.identifyingElement
        assertNotNull(identifier)

        val name = JVMArtifactNamingService().getFullyQualifiedName(identifier!!)
        when (extension) {
            "kt" -> assertEquals(getTestName(false) + ".foo()#aWQ6NTE=", name.identifier)
            "java" -> assertEquals(getTestName(false) + ".foo()#aWQ6NjY=", name.identifier)
            "groovy" -> assertEquals(getTestName(false) + ".foo()#aWQ6NTI=", name.identifier)
            else -> fail("Unknown extension: $extension")
        }
        assertEquals(ArtifactType.EXPRESSION, name.type)
        assertEquals(3, name.lineNumber)
    }

    fun testInnerClassMethodVariable() {
        doTestInnerClassMethodVariable("java")
        doTestInnerClassMethodVariable("kt")
        doTestInnerClassMethodVariable("groovy")
    }

    private fun doTestInnerClassMethodVariable(extension: String) {
        val className = getTestName(false)
        val testDir = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert(className)
        val psiFile = myFixture.configureByFile("$testDir/$className.$extension")
        val identifier = psiFile.findDescendantOfType<PsiNameIdentifierOwner> {
            it.identifyingElement?.text == "id"
        }!!.identifyingElement
        assertNotNull(identifier)

        val name = JVMArtifactNamingService().getFullyQualifiedName(identifier!!)
        when (extension) {
            "kt" -> assertEquals(getTestName(false) + "\$ClassName.foo()#aWQ6OTc=", name.identifier)
            "java" -> assertEquals(getTestName(false) + "\$ClassName.foo()#aWQ6MTEz", name.identifier)
            "groovy" -> assertEquals(getTestName(false) + "\$ClassName.foo()#aWQ6OTI=", name.identifier)
            else -> fail("Unknown extension: $extension")
        }
        assertEquals(ArtifactType.EXPRESSION, name.type)
        assertEquals(4, name.lineNumber)
    }

    fun testClassVariable() {
        doTestClassVariable("java")
        doTestClassVariable("kt")
        doTestClassVariable("groovy")
    }

    private fun doTestClassVariable(extension: String) {
        val className = getTestName(false)
        val testDir = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert(className)
        val psiFile = myFixture.configureByFile("$testDir/$className.$extension")
        val identifier = psiFile.findDescendantOfType<PsiNameIdentifierOwner> {
            it.identifyingElement?.text == "id"
        }!!.identifyingElement
        assertNotNull(identifier)

        val name = JVMArtifactNamingService().getFullyQualifiedName(identifier!!)
        when (extension) {
            "kt" -> assertEquals(getTestName(false) + "#aWQ6MzA=", name.identifier)
            "java" -> assertEquals(getTestName(false) + "#aWQ6Mzc=", name.identifier)
            "groovy" -> assertEquals(getTestName(false) + "#aWQ6MzA=", name.identifier)
            else -> fail("Unknown extension: $extension")
        }
        assertEquals(ArtifactType.EXPRESSION, name.type)
        assertEquals(2, name.lineNumber)
    }

    fun testInnerClassVariable() {
        doTestInnerClassVariable("java")
        doTestInnerClassVariable("kt")
        doTestInnerClassVariable("groovy")
    }

    private fun doTestInnerClassVariable(extension: String) {
        val className = getTestName(false)
        val testDir = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN).convert(className)
        val psiFile = myFixture.configureByFile("$testDir/$className.$extension")
        val identifier = psiFile.findDescendantOfType<PsiNameIdentifierOwner> {
            it.identifyingElement?.text == "id"
        }!!.identifyingElement
        assertNotNull(identifier)

        val name = JVMArtifactNamingService().getFullyQualifiedName(identifier!!)
        when (extension) {
            "kt" -> assertEquals(getTestName(false) + "\$ClassName#aWQ6Njc=", name.identifier)
            "java" -> assertEquals(getTestName(false) + "\$ClassName#aWQ6NzU=", name.identifier)
            "groovy" -> assertEquals(getTestName(false) + "\$ClassName#aWQ6NjE=", name.identifier)
            else -> fail("Unknown extension: $extension")
        }
        assertEquals(ArtifactType.EXPRESSION, name.type)
        assertEquals(3, name.lineNumber)
    }
}
