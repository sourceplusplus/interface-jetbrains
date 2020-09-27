package com.sourceplusplus.portal.display.views

import io.vertx.core.json.JsonObject

/**
 * Holds the current view for the Configuration portal tab.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ConfigurationView {

    var artifact: JsonObject? = null

    fun cloneView(view: ConfigurationView) {
        artifact = view.artifact
    }
}
