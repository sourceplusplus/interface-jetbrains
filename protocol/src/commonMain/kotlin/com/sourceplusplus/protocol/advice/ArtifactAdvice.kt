package com.sourceplusplus.protocol.advice

import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface ArtifactAdvice {

    val artifact: ArtifactQualifiedName
    val category: AdviceCategory
//    val markType: AdviceMarkType
}