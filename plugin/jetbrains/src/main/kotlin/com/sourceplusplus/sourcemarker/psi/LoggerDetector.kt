package com.sourceplusplus.sourcemarker.psi

import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import com.sourceplusplus.sourcemarker.psi.logger.LogbackLogger
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.uast.UMethod
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.1.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LoggerDetector {

    companion object {
        private val log = LoggerFactory.getLogger(LoggerDetector::class.java)
        private val LOGGER_STATEMENTS = SourceKey<List<String>>("LOGGER_STATEMENTS")
    }

    private val detectorSet = setOf(
        LogbackLogger()
    )

    suspend fun getOrFindLoggerStatements(sourceMark: MethodSourceMark): List<String> {
        val loggerStatements = sourceMark.getUserData(LOGGER_STATEMENTS)
        return if (loggerStatements != null) {
            log.trace("Found logger statements: $loggerStatements")
            loggerStatements
        } else {
            val loggerStatements = getOrFindLoggerStatements(sourceMark.getPsiMethod()).await()
            sourceMark.putUserData(LOGGER_STATEMENTS, loggerStatements)
            loggerStatements
        }
    }

    fun getOrFindLoggerStatements(uMethod: UMethod): Future<List<String>> {
        val promise = Promise.promise<List<String>>()
        GlobalScope.launch(SourceMarkerPlugin.vertx.dispatcher()) {
            val loggerStatements = mutableListOf<String>()
            detectorSet.forEach {
                try {
                    loggerStatements.addAll(it.determineLoggerStatements(uMethod).await())
                } catch (throwable: Throwable) {
                    promise.fail(throwable)
                }
            }
            promise.tryComplete(loggerStatements)
        }
        return promise.future()
    }

    /**
     * todo: description.
     */
    interface LoggerStatementDeterminer {
        fun determineLoggerStatements(uMethod: UMethod): Future<List<String>>
    }
}
