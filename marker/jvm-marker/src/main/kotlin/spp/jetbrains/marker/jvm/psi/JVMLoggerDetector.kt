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
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.UserData
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.info.LoggerDetector
import spp.jetbrains.marker.source.info.LoggerDetector.Companion.DETECTED_LOGGER
import spp.jetbrains.marker.source.info.LoggerDetector.Companion.LOGGER_STATEMENTS
import spp.jetbrains.marker.source.info.LoggerDetector.DetectedLogger
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.safeLaunch

/**
 * Detects the presence of log statements within methods and saves log patterns.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMLoggerDetector(val project: Project) : LoggerDetector {

    companion object {
        private val log = logger<JVMLoggerDetector>()

        private val LOGGER_CLASSES = setOf(
            "org.apache.logging.log4j.spi.AbstractLogger",
            "ch.qos.logback.classic.Logger",
            "org.slf4j.Logger"
        )
        private val LOGGER_METHODS = setOf(
            "trace", "debug", "info", "warn", "error"
        )
    }

    private val vertx: Vertx = UserData.vertx(project)

    override fun addLiveLog(editor: Editor, inlayMark: InlayMark, logPattern: String, lineLocation: Int) {
        //todo: better way to handle logger detector with inlay marks
        ApplicationManager.getApplication().runReadAction {
            val guideMark = findMethodGuideMark(editor, inlayMark.sourceFileMarker, lineLocation)
            if (guideMark != null) {
                safeRunBlocking {
                    getOrFindLoggerStatements(guideMark)
                }

                val detectedLogger = DetectedLogger(logPattern, "live", lineLocation)
                inlayMark.putUserData(DETECTED_LOGGER, detectedLogger)

                val loggerStatements = guideMark.getUserData(LOGGER_STATEMENTS)!! as MutableList<DetectedLogger>
                loggerStatements.add(detectedLogger)
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

    override suspend fun getOrFindLoggerStatements(guideMark: MethodGuideMark): List<DetectedLogger> {
        val loggerStatements = guideMark.getUserData(LOGGER_STATEMENTS)
        return if (loggerStatements != null) {
            log.trace("Found logger statements: $loggerStatements")
            loggerStatements
        } else {
            val uMethod = ApplicationManager.getApplication().runReadAction(Computable {
                guideMark.getPsiMethod().toUElementOfType<UMethod>()
            })
            if (uMethod != null) {
                val foundLoggerStatements = getOrFindLoggerStatements(uMethod, guideMark.sourceFileMarker).await()
                guideMark.putUserData(LOGGER_STATEMENTS, foundLoggerStatements)
                foundLoggerStatements
            } else {
                emptyList()
            }
        }
    }

    fun getOrFindLoggerStatements(
        uMethod: UMethod,
        fileMarker: SourceFileMarker
    ): Future<List<DetectedLogger>> {
        val promise = Promise.promise<List<DetectedLogger>>()
        vertx.safeLaunch {
            val loggerStatements = mutableListOf<DetectedLogger>()
            try {
                loggerStatements.addAll(determineLoggerStatements(uMethod, fileMarker).await())
            } catch (throwable: Throwable) {
                promise.fail(throwable)
            }
            promise.tryComplete(loggerStatements)
        }
        return promise.future()
    }

    private fun determineLoggerStatements(
        uMethod: UMethod,
        fileMarker: SourceFileMarker
    ): Future<List<DetectedLogger>> {
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
                                log.debug("Found log statement: $logTemplate")
                                val detectedLogger = DetectedLogger(
                                    logTemplate, methodName, getLineNumber(expression) + 1
                                )

                                //create expression guide mark for the log statement
                                val guideMark = fileMarker.createExpressionSourceMark(
                                    expression, SourceMark.Type.GUIDE
                                )
                                if (!fileMarker.containsSourceMark(guideMark)) {
                                    guideMark.putUserData(DETECTED_LOGGER, detectedLogger)
                                    guideMark.apply(true)
                                } else {
                                    fileMarker.getSourceMark(guideMark.artifactQualifiedName, SourceMark.Type.GUIDE)
                                        ?.putUserData(DETECTED_LOGGER, detectedLogger)
                                }

                                //add to method guide mark list
                                loggerStatements.add(detectedLogger)
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

    private fun findMethodGuideMark(editor: Editor, fileMarker: SourceFileMarker, line: Int): MethodGuideMark? {
        return fileMarker.getGuideMarks().find {
            if (it is MethodGuideMark) {
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
        } as MethodGuideMark?
    }
}
