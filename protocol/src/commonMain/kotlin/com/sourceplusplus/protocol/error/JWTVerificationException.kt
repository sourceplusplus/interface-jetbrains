package com.sourceplusplus.protocol.error

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JWTVerificationException : RuntimeException {

    private val reason: String

    constructor(reason: String) : this(reason, "JWT verification exception: $reason")

    private constructor(reason: String, message: String) : super(message) {
        this.reason = reason
    }

    fun toEventBusException(): JWTVerificationException {
        return JWTVerificationException(
            reason, "EventBusException:JWTVerificationException[$reason]"
        )
    }
}
