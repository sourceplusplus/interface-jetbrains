package com.sourceplusplus.plugin.intellij.marker.mark

import plus.sourceplus.marker.source.mark.api.SourceMark

/**
 * todo: description
 *
 * @version 0.2.5
 * @since 0.2.5
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
interface IntelliJSourceMark extends SourceMark {

    void markArtifactSubscribed()

    void markArtifactUnsubscribed()

    void markArtifactDataAvailable()

    boolean isArtifactSubscribed()

    boolean isArtifactDataAvailable()
}
