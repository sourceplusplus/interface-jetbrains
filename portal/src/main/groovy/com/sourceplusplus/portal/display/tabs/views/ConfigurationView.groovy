package com.sourceplusplus.portal.display.tabs.views

import com.sourceplusplus.api.model.artifact.SourceArtifact
import groovy.transform.Canonical

/**
 * Holds the current view for the Configuration portal tab.
 *
 * @version 0.2.5
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Canonical
class ConfigurationView {

    SourceArtifact artifact

    void cloneView(ConfigurationView view) {
        artifact = view.artifact
    }
}
