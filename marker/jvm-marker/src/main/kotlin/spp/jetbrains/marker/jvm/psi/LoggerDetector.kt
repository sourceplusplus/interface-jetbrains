/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.marker.jvm.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.inlay.InlayMark

/**
 * Detects the presence of log statements within methods and saves log patterns.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LoggerDetector(val vertx: Vertx) {

    companion object {
        private val log = LoggerFactory.getLogger(LoggerDetector::class.java)
        val LOGGER_STATEMENTS = SourceKey<List<DetectedLogger>>("LOGGER_STATEMENTS")

        private val LOGGER_CLASSES = setOf(
            "org.apache.logging.log4j.spi.AbstractLogger",
            "ch.qos.logback.classic.Logger",
            "org.slf4j.Logger"
        )
        private val LOGGER_METHODS = setOf(
            "trace", "debug", "info", "warn", "error"
        )
    }

    fun addLiveLog(editor: Editor, inlayMark: InlayMark, logPattern: String, lineLocation: Int) {
        //todo: better way to handle logger detector with inlay marks
        ApplicationManager.getApplication().runReadAction {
            val methodSourceMark = findMethodSourceMark(editor, inlayMark.sourceFileMarker, lineLocation)
            if (methodSourceMark != null) {
                runBlocking {
                    getOrFindLoggerStatements(methodSourceMark)
                }
                val loggerStatements = methodSourceMark.getUserData(LOGGER_STATEMENTS)!! as MutableList<DetectedLogger>
                loggerStatements.add(DetectedLogger(logPattern, "live", lineLocation))
            } else {
                val loggerStatements = inlayMark.getUserData(LOGGER_STATEMENTS) as MutableList?
                if (loggerStatements == null) {
                    inlayMark.putUserData(
                        LOGGER_STATEMENTS,
                        mutableListOf(DetectedLogger(logPattern, "live", lineLocation))
                    )
                } else {
                    loggerStatements.add(DetectedLogger(logPattern, "live", lineLocation))
                }
            }
        }
    }

    suspend fun getOrFindLoggerStatements(sourceMark: MethodSourceMark): List<DetectedLogger> {
        val loggerStatements = sourceMark.getUserData(LOGGER_STATEMENTS)
        return if (loggerStatements != null) {
            log.trace("Found logger statements: $loggerStatements")
            loggerStatements
        } else {
            val uMethod = sourceMark.getPsiMethod().toUElement() as UMethod?
            if (uMethod != null) {
                val foundLoggerStatements = getOrFindLoggerStatements(uMethod).await()
                sourceMark.putUserData(LOGGER_STATEMENTS, foundLoggerStatements)
                foundLoggerStatements
            } else { //todo: python functions don't have UMethod
                emptyList()
            }
        }
    }

    fun getOrFindLoggerStatements(uMethod: UMethod): Future<List<DetectedLogger>> {
        val promise = Promise.promise<List<DetectedLogger>>()
        GlobalScope.launch(vertx.dispatcher()) {
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
                            val logTemplate = (expression.argumentList.expressions.firstOrNull()?.run {
                                (this as? PsiLiteral)?.value as? String
                            })
                            if (logTemplate != null) {
                                loggerStatements.add(
                                    DetectedLogger(logTemplate, methodName, getLineNumber(expression) + 1)
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

    private fun getLineNumber(element: PsiElement, start: Boolean = true): Int {
        val document = element.containingFile.viewProvider.document
            ?: PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
        val index = if (start) element.startOffset else element.endOffset
        if (index > (document?.textLength ?: 0)) return 0
        return document?.getLineNumber(index) ?: 0
    }

    private fun findMethodSourceMark(editor: Editor, fileMarker: SourceFileMarker, line: Int): MethodSourceMark? {
        return fileMarker.getSourceMarks().find {
            if (it is MethodSourceMark) {
                if (it.configuration.activateOnKeyboardShortcut) {
                    //+1 on end offset so match is made even right after method end
                    val incTextRange = TextRange(
                        it.getPsiMethod().textRange.startOffset,
                        it.getPsiMethod().textRange.endOffset + 1
                    )
                    incTextRange.contains(editor.logicalPositionToOffset(LogicalPosition(line - 1, 0)))
                } else {
                    false
                }
            } else {
                false
            }
        } as MethodSourceMark?
    }

    /**
     * todo: description.
     *
     * @since 0.2.1
     * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
     */
    data class DetectedLogger(
        val logPattern: String,
        val level: String,
        val lineLocation: Int
    )
}
