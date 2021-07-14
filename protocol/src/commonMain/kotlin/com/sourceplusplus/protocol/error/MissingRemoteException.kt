package com.sourceplusplus.protocol.error

class MissingRemoteException : RuntimeException {

    private val remote: String

    constructor(remote: String) : this(remote, "Missing remote: $remote")

    private constructor(remote: String, message: String) : super(message) {
        this.remote = remote
    }

    fun toEventBusException(): MissingRemoteException {
        return MissingRemoteException(
            remote, "EventBusException:MissingRemoteException[$remote]"
        )
    }
}
