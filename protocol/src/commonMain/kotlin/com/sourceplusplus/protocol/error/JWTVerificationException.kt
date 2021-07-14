package com.sourceplusplus.protocol.error

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
