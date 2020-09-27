package com.sourceplusplus.mentor.advice.informative

import com.sourceplusplus.protocol.advice.AdviceCategory
import com.sourceplusplus.protocol.advice.AdviceMarkType
import com.sourceplusplus.protocol.advice.ArtifactAdvice

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ActiveExceptionAdvice : ArtifactAdvice {

    //todo: get active service instance
    //todo: find failing traces
    //todo: determine failing location
    //todo: create advice
    //todo: maintain created advice status (remove on new instances, etc)

    override val category: AdviceCategory = AdviceCategory.INFORMATIVE
    override val markType: AdviceMarkType = AdviceMarkType.GUTTER //todo: multiple marks per advice
}