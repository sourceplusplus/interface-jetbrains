package com.sourceplusplus.protocol.advice.cautionary

import com.sourceplusplus.protocol.advice.AdviceCategory
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class RampDetectionAdvice(
    override val artifact: ArtifactQualifiedName,
    val regression: SimpleRegression,
) : ArtifactAdvice {

    override val category: AdviceCategory = AdviceCategory.CAUTIONARY

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RampDetectionAdvice) return false
        if (artifact != other.artifact) return false
        if (regression != other.regression) return false
        return true
    }

    override fun hashCode(): Int {
        var result = artifact.hashCode()
        result = 31 * result + regression.hashCode()
        return result
    }

    interface SimpleRegression {
        val n: Long
        val intercept: Double
        val slope: Double
        val sumSquaredErrors: Double
        val totalSumSquares: Double
        val xSumSquares: Double
        val sumOfCrossProducts: Double
        val regressionSumSquares: Double
        val meanSquareError: Double
        val r: Double
        val rSquare: Double
        val interceptStdErr: Double
        val slopeStdErr: Double
        val slopeConfidenceInterval: Double
        val significance: Double

        fun predict(x: Double): Double
    }
}