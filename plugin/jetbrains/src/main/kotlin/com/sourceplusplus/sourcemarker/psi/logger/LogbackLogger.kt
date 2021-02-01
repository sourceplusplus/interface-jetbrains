package com.sourceplusplus.sourcemarker.psi.logger

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import com.sourceplusplus.sourcemarker.psi.LoggerDetector
import io.vertx.core.Future
import io.vertx.core.Promise
import org.jetbrains.plugins.groovy.lang.psi.impl.stringValue
import org.jetbrains.uast.UMethod

/**
 * todo: description.
 *
 * @since 0.1.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LogbackLogger : LoggerDetector.LoggerStatementDeterminer {

    companion object {
        private val LOGGER_CLASSES = setOf(
            "ch.qos.logback.classic.Logger",
            "org.slf4j.Logger"
        )
        private val LOGGER_METHODS = setOf(
            "trace", "debug", "info", "warn", "error"
        )
    }

    override fun determineLoggerStatements(uMethod: UMethod): Future<List<String>> {
        val promise = Promise.promise<List<String>>()
        val loggerStatements = mutableListOf<String>()
        ApplicationManager.getApplication().runReadAction {
            uMethod.javaPsi.accept(object : JavaRecursiveElementVisitor() {
                override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                    val methodName = expression.methodExpression.referenceName
                    if (methodName != null && LOGGER_METHODS.contains(methodName)) {
                        val resolvedMethod = expression.resolveMethod()
                        if (resolvedMethod != null && LOGGER_CLASSES.contains(resolvedMethod.containingClass?.qualifiedName.orEmpty())) {
                            loggerStatements.add(expression.argumentList.expressions[0].stringValue()!!)
                        }
                    }
                }
            })
            promise.complete(loggerStatements)
        }
        return promise.future()
    }
}
