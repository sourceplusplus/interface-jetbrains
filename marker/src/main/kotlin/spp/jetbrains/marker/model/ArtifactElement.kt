/*
 * Source++, the continuous feedback platform for developers.
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
package spp.jetbrains.marker.model

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.descendants
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.service.ArtifactTypeService
import spp.jetbrains.marker.service.getCalls
import spp.jetbrains.marker.service.toArtifact
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.protocol.insight.InsightValue
import java.util.concurrent.ConcurrentHashMap

abstract class ArtifactElement(private val psiElement: PsiElement) : PsiElement by psiElement {

    val data = ConcurrentHashMap<SourceKey<*>, Any>()

    val descendantArtifacts: List<ArtifactElement> by lazy {
        val descendants = mutableListOf<ArtifactElement>()
        descendants {
            val artifact = it.toArtifact()
            var visitChildren = true
            if (artifact != null) {
                visitChildren = false
                descendants.add(artifact)
            }
            visitChildren
        }.toList()
        descendants
    }

    fun <T> getData(key: SourceKey<T>): T? {
        return data[key] as T?
    }

    fun isControlStructure(): Boolean = this is ControlStructureArtifact
    fun isCall(): Boolean = this is CallArtifact
    fun isLiteral(): Boolean = this is ArtifactLiteralValue

    fun getInsights(): List<InsightValue<*>> {
        return SourceMarkerKeys.ALL_INSIGHTS.mapNotNull { getUserData(it.asPsiKey()) } +
                SourceMarkerKeys.ALL_INSIGHTS.mapNotNull { getData(it) }
    }

    fun getDuration(includingPredictions: Boolean = true): Long? {
        if (includingPredictions) {
            val durationPrediction = getUserData(SourceMarkerKeys.METHOD_DURATION_PREDICTION.asPsiKey())?.value
            if (durationPrediction != null) {
                return durationPrediction
            }
        }

        return getUserData(SourceMarkerKeys.METHOD_DURATION.asPsiKey())?.value
    }

    fun getPathExecutionProbability(): InsightValue<Double> {
        return getUserData(SourceMarkerKeys.PATH_EXECUTION_PROBABILITY.asPsiKey())!!
    }

    override fun <T : Any?> getUserData(key: Key<T>): T? {
        //todo: rethink this
        var guideMark = psiElement.getUserData(SourceKey.GuideMark)
        if (guideMark == null && psiElement is PsiNameIdentifierOwner) {
            guideMark = psiElement.nameIdentifier?.getUserData(SourceKey.GuideMark)
        }
        if (guideMark != null) {
            return guideMark.getUserData(SourceKey<T>(key.toString()))
        }

        return psiElement.getUserData(key)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ArtifactElement
        if (psiElement != other.psiElement) return false
        return true
    }

    override fun hashCode(): Int {
        return psiElement.hashCode()
    }

    abstract fun clone(): ArtifactElement
}

// Extensions

fun ArtifactElement?.isLiteral(): Boolean {
    return (this != null) && ArtifactTypeService.isLiteral(this)
}

fun ArtifactElement.getCalls(): List<CallArtifact> {
    return getCalls().map { it.toArtifact() }.filterIsInstance<CallArtifact>()
}
