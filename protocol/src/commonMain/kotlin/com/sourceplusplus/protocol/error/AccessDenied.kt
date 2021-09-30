package com.sourceplusplus.protocol.error

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class AccessDenied : RuntimeException {

    val reason: String

    constructor(reason: String) : this(reason, "Access denied: $reason")

    private constructor(reason: String, message: String) : super(message) {
        this.reason = reason
    }

    fun toEventBusException(): AccessDenied {
        return AccessDenied(
            reason, "EventBusException:AccessDenied[$reason]"
        )
    }
}
