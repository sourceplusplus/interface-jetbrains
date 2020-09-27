package com.sourceplusplus.mentor.advice.cautionary

import com.sourceplusplus.protocol.advice.AdviceCategory
import com.sourceplusplus.protocol.advice.AdviceMarkType
import com.sourceplusplus.protocol.advice.ArtifactAdvice

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class RampDetectionAdvice : ArtifactAdvice {

    override val category: AdviceCategory = AdviceCategory.CAUTIONARY
    override val markType: AdviceMarkType = AdviceMarkType.GUTTER
}