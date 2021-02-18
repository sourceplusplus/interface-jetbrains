package com.sourceplusplus.mentor.service

import io.vertx.core.AsyncResult
import io.vertx.core.Handler

interface LogCountIndicatorService {
    fun getOccurredCount(logPattern: String, handler: Handler<AsyncResult<Int>>)
}
