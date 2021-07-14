package com.sourceplusplus.protocol.error

class LiveInstrumentException(val errorType: ErrorType, message: String) : RuntimeException(message) {

    fun toEventBusException(): LiveInstrumentException {
        return LiveInstrumentException(errorType, "EventBusException:LiveInstrumentException[$errorType]: $message")
    }

    enum class ErrorType {
        CLASS_NOT_FOUND,
        CONDITIONAL_FAILED
    }
}
