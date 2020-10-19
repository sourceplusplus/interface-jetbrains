package com.sourceplusplus.portal.model

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class ArtifactConfigType(val description: String) {
    ENTRY_METHOD("Entry method"),
    AUTO_SUBSCRIBE("Auto-subscribe");

    val id = name.toLowerCase()
}
