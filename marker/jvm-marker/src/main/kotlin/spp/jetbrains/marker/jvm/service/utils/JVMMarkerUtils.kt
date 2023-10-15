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
package spp.jetbrains.marker.jvm.service.utils

import com.intellij.lang.jvm.util.JvmClassUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import spp.jetbrains.artifact.service.*
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import java.util.*

/**
 * JVM utility functions for working with [SourceMark]s.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object JVMMarkerUtils {

    private val log = logger<JVMMarkerUtils>()

    fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        when {
            element.isKotlin() && element is KtClass -> return getFullyQualifiedName(element)
            element.isKotlin() && element is KtNamedFunction -> return getFullyQualifiedName(element)
            element is PsiAnnotation -> return getFullyQualifiedName(element)
            element is PsiClass -> return getFullyQualifiedName(element)
            element is PsiMethod -> return getFullyQualifiedName(element)
            else -> Unit
        }

        var expressionString = element.text
        var parentIdentifier = ArtifactScopeService.getParentFunction(element)?.let { getFullyQualifiedName(it) }
        if (parentIdentifier == null) {
            parentIdentifier = ArtifactScopeService.getParentClass(element)?.let { getFullyQualifiedName(it) }
        }
        if (parentIdentifier == null) {
            //todo: extension function, see SourceMarkerConfig, make test, groovy import statements
            error("Could not determine parent of element: $element")
        }

        element.textRange.startOffset.let {
            expressionString = "$expressionString:$it"
        }
        return ArtifactQualifiedName(
            """${parentIdentifier.identifier}#${
                Base64.getEncoder().encodeToString(expressionString.toByteArray())
            }""",
            type = ArtifactType.EXPRESSION,
            lineNumber = SourceMarkerUtils.getLineNumber(element)
        )
    }

    private fun getFullyQualifiedName(annotation: PsiAnnotation): ArtifactQualifiedName {
        val qualifiedName = getFullyQualifiedName(annotation, annotation.qualifiedName.toString())
        return ArtifactQualifiedName(
            qualifiedName,
            type = ArtifactType.ANNOTATION,
            lineNumber = annotation.nameReferenceElement?.let { SourceMarkerUtils.getLineNumber(it) }
        )
    }

    private fun getFullyQualifiedName(psiElement: PsiElement, simpleName: String): String {
        if (psiElement.isJava() && psiElement.containingFile is PsiJavaFile) {
            val javaFile = psiElement.containingFile as PsiJavaFile
            for (importStatement in javaFile.importList?.importStatements ?: emptyArray()) {
                val qName = importStatement.qualifiedName
                if (qName?.endsWith(".$simpleName") == true || qName == simpleName) {
                    return qName
                }
            }

            val packageName = javaFile.packageStatement?.packageName
            return if (packageName != null) "$packageName.$simpleName" else simpleName
        } else if (psiElement.isKotlin() && psiElement.containingFile is KtFile) {
            val ktFile = psiElement.containingFile as KtFile
            for (importDirective in ktFile.importDirectives) {
                val qName = importDirective.importedFqName?.asString()
                if (qName?.endsWith(".$simpleName") == true || qName == simpleName) {
                    return qName
                }
            }

            val packageName = ktFile.packageFqName.asString()
            return "$packageName.$simpleName"
        } else if (psiElement.isGroovy() && psiElement.containingFile is GroovyFile) {
            val groovyFile = psiElement.containingFile as GroovyFile
            for (importStatement in groovyFile.importStatements) {
                val qName = importStatement.importFqn.toString()
                if (qName.endsWith(".$simpleName") || qName == simpleName) {
                    return qName
                }
            }

            val packageName = groovyFile.packageDefinition?.packageName
            return if (packageName != null) "$packageName.$simpleName" else simpleName
        } else {
            return simpleName
        }
    }

    private fun getFullyQualifiedName(clazz: PsiClass): ArtifactQualifiedName {
        return ArtifactQualifiedName(
            "${JvmClassUtil.getJvmClassName(clazz)}",
            type = ArtifactType.CLASS,
            lineNumber = clazz.nameIdentifier?.let { SourceMarkerUtils.getLineNumber(it) }
        )
    }

    private fun getFullyQualifiedName(theClass: KtClass): ArtifactQualifiedName {
        val fullyQualifiedName = KotlinPsiHeuristics.getJvmName(theClass)
            ?: throw IllegalArgumentException("Could not determine fully qualified name for class: $theClass")

        return ArtifactQualifiedName(
            fullyQualifiedName,
            type = ArtifactType.CLASS,
            lineNumber = theClass.nameIdentifier?.let { SourceMarkerUtils.getLineNumber(it) }
        )
    }

    private fun getFullyQualifiedName(method: PsiMethod): ArtifactQualifiedName {
        val classQualifiedName = method.findAnyContainingStrict(PsiClass::class.java)?.let {
            getFullyQualifiedName(it).identifier
        }
        return ArtifactQualifiedName(
            "$classQualifiedName.${getQualifiedName(method)}",
            type = ArtifactType.FUNCTION,
            lineNumber = method.nameIdentifier?.let { SourceMarkerUtils.getLineNumber(it) }
        )
    }

    private fun getFullyQualifiedName(method: KtNamedFunction): ArtifactQualifiedName {
        val classQualifiedName = method.findAnyContainingStrict(KtClass::class.java)?.let {
            getFullyQualifiedName(it).identifier
        }
        return ArtifactQualifiedName(
            "$classQualifiedName.${getQualifiedName(method)}",
            type = ArtifactType.FUNCTION,
            lineNumber = method.nameIdentifier?.let { SourceMarkerUtils.getLineNumber(it) }
        )
    }

    fun getMethodAnnotations(element: PsiElement): List<PsiElement> {
        return when (element) {
            is PsiMethod -> element.annotations.toList()
            is KtFunction -> element.annotationEntries
            else -> emptyList()
        }
    }

    //todo: better
    private fun getQualifiedName(method: PsiMethod): String {
        val methodName = method.nameIdentifier!!.text
        var methodParams = ""
        method.parameterList.parameters.forEach {
            if (methodParams.isNotEmpty()) {
                methodParams += ","
            }
            val qualifiedType = PsiUtil.resolveClassInType(it.type)
            val arrayDimensions = getArrayDimensions(it.type.toString())
            if (qualifiedType != null) {
                methodParams += if (qualifiedType.containingClass != null) {
                    qualifiedType.containingClass!!.qualifiedName + '$' + qualifiedType.name
                } else {
                    qualifiedType.qualifiedName
                }
                repeat(arrayDimensions) {
                    methodParams += "[]"
                }
            } else if (it.typeElement != null) {
                methodParams += findFullyQualifiedName(it.type, it.containingFile) ?: when (it.typeElement!!.text) {
                    "String" -> "java.lang.String"
                    "String[]" -> "java.lang.String[]"
                    else -> it.typeElement!!.text
                }
            } else if (it.type is PsiPrimitiveType) {
                methodParams += if (ArtifactTypeService.isKotlin(it)) {
                    (it.type as PsiPrimitiveType).boxedTypeName
                } else {
                    it.type.canonicalText
                }
            } else {
                log.warn("Unable to detect element type: $it")
            }
        }
        return "$methodName($methodParams)"
    }

    /**
     * Search imports for a fully qualified name that ends with the simple name of the type.
     */
    private fun findFullyQualifiedName(psiType: PsiType, psiFile: PsiFile): String? {
        val simpleName = psiType.presentableText
        val psiJavaFile = psiFile as? PsiJavaFile ?: return null
        val importList = psiJavaFile.importList ?: return null
        for (importStatement in importList.allImportStatements) {
            val qualifiedName = importStatement.importReference?.qualifiedName
            if (qualifiedName?.endsWith(".$simpleName") == true) {
                return qualifiedName
            }
        }
        return null
    }

    //todo: better
    private fun getQualifiedName(method: KtNamedFunction): String {
        val methodName = method.nameIdentifier?.text ?: method.name ?: "unknown"
        var methodParams = ""
        method.valueParameters.forEach {
            if (methodParams.isNotEmpty()) {
                methodParams += ","
            }
            val paramType = (it.resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType
            val qualifiedType = if (paramType != null && KotlinBuiltIns.isPrimitiveArray(paramType)) {
                val arrayType = KotlinBuiltIns.getPrimitiveArrayElementType(paramType)
                arrayType?.let { JvmPrimitiveType.get(it).javaKeywordName + "[]" }
            } else if (paramType != null && KotlinBuiltIns.isArray(paramType)
                && paramType.arguments.firstOrNull()?.type?.fqName?.asString() == "kotlin.String"
            ) {
                "java.lang.String[]"
            } else {
                paramType?.let {
                    KotlinBuiltIns.getPrimitiveType(it)?.let { JvmPrimitiveType.get(it) }?.javaKeywordName
                } ?: if (paramType != null && KotlinBuiltIns.isString(paramType)) {
                    "java.lang.String"
                } else if (paramType?.unwrap()?.constructor?.declarationDescriptor?.psiElement is KtClass) {
                    val clazz = paramType.unwrap().constructor.declarationDescriptor?.psiElement as KtClass
                    getFullyQualifiedName(clazz).identifier
                } else {
                    paramType?.fqName?.toString()?.replace(".", "$")
                }
            }
            if (qualifiedType != null) {
                methodParams += qualifiedType
                repeat(getArrayDimensions(it.text)) {
                    methodParams += "[]"
                }
            } else if (it.typeReference != null) {
                methodParams += it.typeReference!!.text
            } else {
                log.warn("Unable to detect element type: $it")
            }
        }
        return "$methodName($methodParams)"
    }

    private fun getArrayDimensions(s: String): Int {
        var arrayDimensions = 0
        for (element in s) {
            if (element == '[') {
                arrayDimensions++
            }
        }
        return arrayDimensions
    }

    private fun <T : PsiElement> PsiElement?.findAnyContainingStrict(
        vararg types: Class<out T>
    ): T? {
        val depthLimit: Int = Integer.MAX_VALUE
        var element = this?.parent
        var i = 0
        while (i < depthLimit && element != null && element !is PsiFileSystemItem) {
            element.let {
                for (type in types) {
                    if (type.isInstance(it)) {
                        return it as T
                    }
                }
            }
            element = element.parent
            i++
        }
        return null
    }
}
