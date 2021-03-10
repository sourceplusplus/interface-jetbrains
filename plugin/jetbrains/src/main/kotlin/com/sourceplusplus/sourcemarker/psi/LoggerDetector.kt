package com.sourceplusplus.sourcemarker.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.plugins.groovy.lang.psi.impl.stringValue
import org.jetbrains.uast.UMethod
import org.slf4j.LoggerFactory

/**
 * Detects the presence of log statements within methods and saves log patterns.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LoggerDetector {

    companion object {
        private val log = LoggerFactory.getLogger(LoggerDetector::class.java)
        private val LOGGER_STATEMENTS = SourceKey<List<DetectedLogger>>("LOGGER_STATEMENTS")

        private val LOGGER_CLASSES = setOf(
            "org.apache.logging.log4j.spi.AbstractLogger",
            "ch.qos.logback.classic.Logger",
            "org.slf4j.Logger"
        )
        private val LOGGER_METHODS = setOf(
            "trace", "debug", "info", "warn", "error"
        )
    }

    suspend fun getOrFindLoggerStatements(sourceMark: MethodSourceMark): List<DetectedLogger> {
        val loggerStatements = sourceMark.getUserData(LOGGER_STATEMENTS)
        return if (loggerStatements != null) {
            log.trace("Found logger statements: $loggerStatements")
            loggerStatements
        } else {
            val foundLoggerStatements = getOrFindLoggerStatements(sourceMark.getPsiMethod()).await()
            sourceMark.putUserData(LOGGER_STATEMENTS, foundLoggerStatements)
            foundLoggerStatements
        }
    }

    fun getOrFindLoggerStatements(uMethod: UMethod): Future<List<DetectedLogger>> {
        val promise = Promise.promise<List<DetectedLogger>>()
        GlobalScope.launch(SourceMarkerPlugin.vertx.dispatcher()) {
            val loggerStatements = mutableListOf<DetectedLogger>()
            try {
                loggerStatements.addAll(determineLoggerStatements(uMethod).await())
            } catch (throwable: Throwable) {
                promise.fail(throwable)
            }
            promise.tryComplete(loggerStatements)
        }
        return promise.future()
    }

    private fun determineLoggerStatements(uMethod: UMethod): Future<List<DetectedLogger>> {
        val promise = Promise.promise<List<DetectedLogger>>()
        val loggerStatements = mutableListOf<DetectedLogger>()
        ApplicationManager.getApplication().runReadAction {
            uMethod.javaPsi.accept(object : JavaRecursiveElementVisitor() {
                override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                    val methodName = expression.methodExpression.referenceName
                    if (methodName != null && LOGGER_METHODS.contains(methodName)) {
                        val resolvedMethod = expression.resolveMethod() ?: return
                        if (LOGGER_CLASSES.contains(resolvedMethod.containingClass?.qualifiedName.orEmpty())) {
                            if (expression.argumentList.expressions.firstOrNull()?.stringValue() != null) {
                                val logTemplate = expression.argumentList.expressions.first().stringValue()!!
                                loggerStatements.add(
                                    DetectedLogger(logTemplate, methodName, expression.getLineNumber() + 1)
                                )
                                log.debug("Found log statement: $logTemplate")
                            } else {
                                log.warn("No log template argument available for expression: $expression")
                            }
                        }
                    }
                }
            })
            promise.complete(loggerStatements)
        }
        return promise.future()
    }

    data class DetectedLogger(
        val logPattern: String,
        val level: String,
        val lineLocation: Int
    )
}
