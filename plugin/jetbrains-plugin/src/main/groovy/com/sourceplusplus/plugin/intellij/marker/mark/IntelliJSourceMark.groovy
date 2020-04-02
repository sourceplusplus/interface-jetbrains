package com.sourceplusplus.plugin.intellij.marker.mark

import com.sourceplusplus.marker.source.mark.api.SourceMark

/**
 * todo: description
 *
 * @version 0.2.5
 * @since 0.2.5
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
interface IntelliJSourceMark extends SourceMark {

    static final String SOURCE_MARK_CREATED = "SourceMarkCreated"

    void markArtifactSubscribed()

    void markArtifactUnsubscribed()

    void markArtifactDataAvailable()

    boolean isArtifactSubscribed()

    boolean isArtifactDataAvailable()
}
