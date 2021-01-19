package com.sourceplusplus.protocol.artifact.trace

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class TraceOrderType {
    LATEST_TRACES,
    SLOWEST_TRACES,
    FAILED_TRACES;

    //todo: not need to replace _TRACES?
    val id = name.replace("_TRACES", "").toLowerCase()
    val description = id.toLowerCase().capitalize()
    val fullDescription = name.toLowerCase().split("_").joinToString(" ") { it.capitalize() }
}
