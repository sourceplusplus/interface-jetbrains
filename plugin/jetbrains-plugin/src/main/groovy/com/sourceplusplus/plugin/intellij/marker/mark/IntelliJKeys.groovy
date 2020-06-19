package com.sourceplusplus.plugin.intellij.marker.mark

import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.marker.source.mark.api.key.SourceKey

/**
 * Keys used by Source++ to attribute data to IntelliJ elements.
 *
 * @version 0.2.6
 * @since 0.2.5
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJKeys {
    public static final SourceKey<SourceArtifact> SourceArtifact = new SourceKey<>("SourceArtifact")
    public static final SourceKey<String> PortalUUID = new SourceKey<>("PortalUUID")
    public static final SourceKey<Long> PortalRefresher = new SourceKey<>("PortalRefresher")
}
