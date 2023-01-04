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

    fun testJavaClassName() {
        doTestClassName<PsiClass>("java")
    }

    fun testKotlinClassName() {
        doTestClassName<KtClass>("kt")
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
        assertNotNull(name.lineNumber)
    }

    fun testJavaInnerClassName() {
        doTestInnerClassName<PsiClass>("java")
    }

    fun testKotlinInnerClassName() {
        doTestInnerClassName<KtClass>("kt")
    }

    fun testGroovyInnerClassName() {
        doTestInnerClassName<PsiClass>("groovy")
    }

    private inline fun <reified T : PsiElement> doTestInnerClassName(extension: String) {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".$extension")
        val clazz = psiFile.findDescendantOfType<T> { true }
        assertNotNull(clazz)

        val parentName = JVMArtifactNamingService().getFullyQualifiedName(clazz!!)
        assertEquals(getTestName(false) + "", parentName.identifier)
        assertEquals(ArtifactType.CLASS, parentName.type)
        assertNotNull(parentName.lineNumber)

        val innerClazz = clazz.findDescendantOfType<T> { it !== clazz }
        assertNotNull(innerClazz)

        val innerName = JVMArtifactNamingService().getFullyQualifiedName(innerClazz!!)
        assertEquals(getTestName(false) + "\$InnerClassName", innerName.identifier)
        assertEquals(ArtifactType.CLASS, innerName.type)
        assertNotNull(innerName.lineNumber)
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
        val className = getTestName(false)
        val psiFile = myFixture.configureByFile("$className.$extension")
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

    fun testJavaInnerClassMethodName() {
        doTestInnerClassMethodName<PsiMethod>("java")
    }

    fun testKotlinInnerClassMethodName() {
        doTestInnerClassMethodName<KtFunction>("kt")
    }

    fun testGroovyInnerClassMethodName() {
        doTestInnerClassMethodName<PsiMethod>("groovy")
    }

    private inline fun <reified T : PsiElement> doTestInnerClassMethodName(extension: String) {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".$extension")
        val method = psiFile.findDescendantOfType<T> { true }
        assertNotNull(method)

        val name = JVMArtifactNamingService().getFullyQualifiedName(method!!)
        assertEquals(getTestName(false) + "\$InnerClassName.foo()", name.identifier)
        assertEquals(ArtifactType.METHOD, name.type)
        assertNotNull(name.lineNumber)
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

    fun testJavaInnerClassMethodVariable() {
        doTestInnerClassMethodVariable("java")
    }

    fun testKotlinInnerClassMethodVariable() {
        doTestInnerClassMethodVariable("kt")
    }

    fun testGroovyInnerClassMethodVariable() {
        doTestInnerClassMethodVariable("groovy")
    }

    private fun doTestInnerClassMethodVariable(extension: String) {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".$extension")
        val identifier = psiFile.findDescendantOfType<PsiNameIdentifierOwner> {
            it.identifyingElement?.text == "id"
        }!!.identifyingElement
        assertNotNull(identifier)

        val name = JVMArtifactNamingService().getFullyQualifiedName(identifier!!)
        when (extension) {
            "kt" -> assertEquals(getTestName(false) + "\$InnerClassName.foo()#aWQ6MTA4", name.identifier)
            "java" -> assertEquals(getTestName(false) + "\$InnerClassName.foo()#aWQ6MTIy", name.identifier)
            "groovy" -> assertEquals(getTestName(false) + "\$InnerClassName.foo()#aWQ6MTAz", name.identifier)
            else -> fail("Unknown extension: $extension")
        }
        assertEquals(ArtifactType.EXPRESSION, name.type)
        assertEquals(4, name.lineNumber)
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

    fun testJavaInnerClassVariable() {
        doTestInnerClassVariable("java")
    }

    fun testKotlinInnerClassVariable() {
        doTestInnerClassVariable("kt")
    }

    fun testGroovyInnerClassVariable() {
        doTestInnerClassVariable("groovy")
    }

    private fun doTestInnerClassVariable(extension: String) {
        val psiFile = myFixture.configureByFile(getTestName(false) + ".$extension")
        val identifier = psiFile.findDescendantOfType<PsiNameIdentifierOwner> {
            it.identifyingElement?.text == "id"
        }!!.identifyingElement
        assertNotNull(identifier)

        val name = JVMArtifactNamingService().getFullyQualifiedName(identifier!!)
        when (extension) {
            "kt" -> assertEquals(getTestName(false) + "\$InnerClassName#aWQ6Nzg=", name.identifier)
            "java" -> assertEquals(getTestName(false) + "\$InnerClassName#aWQ6ODQ=", name.identifier)
            "groovy" -> assertEquals(getTestName(false) + "\$InnerClassName#aWQ6NzI=", name.identifier)
            else -> fail("Unknown extension: $extension")
        }
        assertEquals(ArtifactType.EXPRESSION, name.type)
        assertEquals(3, name.lineNumber)
    }
}
