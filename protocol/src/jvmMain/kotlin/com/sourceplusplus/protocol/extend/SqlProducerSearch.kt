package com.sourceplusplus.protocol.extend

import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import org.jooq.Query
import java.util.*

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SqlProducerSearch {

    suspend fun determineSource(
        query: Query,
        searchPoint: ArtifactQualifiedName
    ): Optional<ArtifactQualifiedName>
}
