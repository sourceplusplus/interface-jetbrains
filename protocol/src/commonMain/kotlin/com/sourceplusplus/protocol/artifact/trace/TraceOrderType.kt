package com.sourceplusplus.protocol.artifact.trace

import com.sourceplusplus.protocol.artifact.OrderType

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class TraceOrderType : OrderType {
    LATEST_TRACES,
    SLOWEST_TRACES,
    FAILED_TRACES;

    //todo: not need to replace _TRACES?
    val id = name.replace("_TRACES", "").toLowerCase()
    override val description = id.toLowerCase().capitalize()
    val fullDescription = name.toLowerCase().split("_").joinToString(" ") { it.capitalize() }
}
