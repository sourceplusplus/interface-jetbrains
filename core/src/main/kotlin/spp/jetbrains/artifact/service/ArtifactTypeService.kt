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
package spp.jetbrains.artifact.service

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import spp.jetbrains.artifact.model.FunctionArtifact
import spp.jetbrains.artifact.service.define.AbstractSourceMarkerService
import spp.jetbrains.artifact.service.define.IArtifactTypeService
import spp.protocol.artifact.ArtifactType

@Suppress("TooManyFunctions") // public API
object ArtifactTypeService : AbstractSourceMarkerService<IArtifactTypeService>(), IArtifactTypeService {

    override fun getNameIdentifier(element: PsiElement): PsiElement {
        return getService(element.language).getNameIdentifier(element)
    }

    override fun getAnnotations(element: PsiElement): List<PsiElement> {
        return getService(element.language).getAnnotations(element)
    }

    override fun getAnnotationOwnerIfAnnotation(element: PsiElement): PsiElement? {
        return getService(element.language).getAnnotationOwnerIfAnnotation(element)
    }

    override fun getAnnotationOwnerIfAnnotation(element: PsiElement, line: Int): PsiElement? {
        return getService(element.language).getAnnotationOwnerIfAnnotation(element, line)
    }

    override fun isComment(element: PsiElement): Boolean {
        //language-agnostic check
        if (element is PsiComment) return true

        //language-specific check
        return getService(element.language).isComment(element)
    }

    override fun getType(element: PsiElement): ArtifactType? {
        if (!isSupported(element)) return null
        //language-agnostic check
        if (element is PsiFile) return ArtifactType.FILE

        //language-specific check
        return getService(element.language).getType(element)
    }

    override fun isLiteral(element: PsiElement): Boolean {
        return getService(element.language).isLiteral(element)
    }

    fun isSupported(element: PsiElement): Boolean {
        return getServiceIfPresent(element.language) != null
    }

    fun isFunction(element: PsiElement): Boolean {
        return element is FunctionArtifact || getType(element) == ArtifactType.FUNCTION
    }

    fun isClass(element: PsiElement): Boolean {
        return getType(element) == ArtifactType.CLASS
    }

    fun isFile(element: PsiElement): Boolean {
        return getType(element) == ArtifactType.FILE
    }

    fun isPython(element: PsiElement): Boolean {
        return element.language.id == "Python"
    }

    fun isJvm(element: PsiElement): Boolean {
        return getJvmLanguages().contains(element.language.id)
    }

    fun isJava(element: PsiElement): Boolean {
        return element.language.id == "JAVA"
    }

    fun isKotlin(element: PsiElement): Boolean {
        return element.language.id == "kotlin"
    }

    fun isGroovy(element: PsiElement): Boolean {
        return element.language.id == "Groovy"
    }

    fun isScala(element: PsiElement): Boolean {
        return element.language.id == "Scala"
    }

    fun isJavaScript(element: PsiElement): Boolean {
        return getJavaScriptLanguages().contains(element.language.id)
    }

    fun getJvmLanguages(): List<String> {
        return listOf("JAVA", "kotlin", "Groovy", "Scala")
    }

    fun getJavaScriptLanguages(): List<String> {
        return listOf("JavaScript", "ECMAScript 6")
    }
}

// Extensions

fun PsiElement.isComment(): Boolean {
    return ArtifactTypeService.isComment(this)
}

fun PsiElement.getType(): ArtifactType? {
    return ArtifactTypeService.getType(this)
}

fun PsiElement?.isLiteral(): Boolean {
    return this?.let { ArtifactTypeService.isLiteral(it) } ?: false
}

fun PsiElement.isFunction(): Boolean {
    return ArtifactTypeService.isFunction(this)
}

fun PsiElement.isClass(): Boolean {
    return ArtifactTypeService.isClass(this)
}

fun PsiElement.isFile(): Boolean {
    return ArtifactTypeService.isFile(this)
}

fun PsiElement.isPython(): Boolean {
    return ArtifactTypeService.isPython(this)
}

fun PsiElement.isJvm(): Boolean {
    return ArtifactTypeService.isJvm(this)
}

fun PsiElement.isJava(): Boolean {
    return ArtifactTypeService.isJava(this)
}

fun PsiElement.isKotlin(): Boolean {
    return ArtifactTypeService.isKotlin(this)
}

fun PsiElement.isGroovy(): Boolean {
    return ArtifactTypeService.isGroovy(this)
}

fun PsiElement.isScala(): Boolean {
    return ArtifactTypeService.isScala(this)
}

fun PsiElement.isJavaScript(): Boolean {
    return ArtifactTypeService.isJavaScript(this)
}
