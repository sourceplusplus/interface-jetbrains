package com.sourceplusplus.protocol.error

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveInstrumentException(val errorType: ErrorType, message: String) : RuntimeException(message) {

    fun toEventBusException(): LiveInstrumentException {
        return LiveInstrumentException(errorType, "EventBusException:LiveInstrumentException[$errorType]: $message")
    }

    enum class ErrorType {
        CLASS_NOT_FOUND,
        CONDITIONAL_FAILED
    }
}
