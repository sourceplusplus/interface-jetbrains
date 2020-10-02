package com.sourceplusplus.protocol.advice.informative

import com.sourceplusplus.protocol.advice.AdviceCategory
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ActiveExceptionAdvice(
    override val artifact: ArtifactQualifiedName
) : ArtifactAdvice {

    //todo: get active service instance
    //todo: find failing traces
    //todo: determine failing location
    //todo: create advice
    //todo: maintain created advice status (remove on new instances, etc)

    override val category: AdviceCategory = AdviceCategory.INFORMATIVE
}