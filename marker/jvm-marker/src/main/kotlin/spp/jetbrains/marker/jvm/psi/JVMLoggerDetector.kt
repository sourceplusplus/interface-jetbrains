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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.info.LoggerDetector
import spp.jetbrains.marker.source.info.LoggerDetector.Companion.DETECTED_LOGGER
import spp.jetbrains.marker.source.info.LoggerDetector.DetectedLogger
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark

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

    override fun determineLoggerStatements(guideMark: MethodGuideMark): List<DetectedLogger> {
        if (guideMark.language.id == "Groovy") {
            determineLoggerStatements(guideMark.getPsiMethod() as GrMethod, guideMark.sourceFileMarker)
        } else {
            val uMethod = ApplicationManager.getApplication().runReadAction(Computable {
                guideMark.getPsiMethod().toUElementOfType<UMethod>()
            })
            if (uMethod != null) {
                determineLoggerStatements(uMethod, guideMark.sourceFileMarker)
            }
        }
        return guideMark.getChildren().mapNotNull { it.getUserData(DETECTED_LOGGER) }
    }

    fun determineLoggerStatements(uMethod: UMethod, fileMarker: SourceFileMarker): List<DetectedLogger> {
        val loggerStatements = mutableListOf<DetectedLogger>()
        ApplicationManager.getApplication().runReadAction {
            uMethod.accept(object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    val expression = node.sourcePsi ?: return super.visitCallExpression(node)
                    val loggerClass = node.resolve()?.containingClass?.qualifiedName
                    if (loggerClass != null && LOGGER_CLASSES.contains(loggerClass)) {
                        val methodName = node.methodIdentifier?.name
                        if (methodName != null && LOGGER_METHODS.contains(methodName)) {
                            val logTemplate = node.valueArguments.firstOrNull()?.run {
                                (this as? ULiteralExpression)?.value as? String
                            }

                            if (logTemplate != null) {
                                log.debug("Found log statement: $logTemplate")
                                val detectedLogger = DetectedLogger(
                                    logTemplate, methodName, getLineNumber(expression) + 1
                                )
                                loggerStatements.add(detectedLogger)

                                //create expression guide mark for the log statement
                                val guideMark = fileMarker.createExpressionSourceMark(
                                    expression.parent, SourceMark.Type.GUIDE
                                )
                                if (!fileMarker.containsSourceMark(guideMark)) {
                                    guideMark.putUserData(DETECTED_LOGGER, detectedLogger)
                                    guideMark.apply(true)
                                } else {
                                    fileMarker.getSourceMark(guideMark.artifactQualifiedName, SourceMark.Type.GUIDE)
                                        ?.putUserData(DETECTED_LOGGER, detectedLogger)
                                }
                            } else {
                                log.warn("No log template argument available for expression: $expression")
                            }
                        }
                    }
                    return super.visitCallExpression(node)
                }
            })
        }
        return loggerStatements
    }

    /**
     * Unsure why, but Groovy UAST visitors don't work here. Have to use Groovy PSI.
     */
    fun determineLoggerStatements(grMethod: GrMethod, fileMarker: SourceFileMarker): List<DetectedLogger> {
        val loggerStatements = mutableListOf<DetectedLogger>()
        ApplicationManager.getApplication().runReadAction {
            grMethod.acceptChildren(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is GrMethodCallExpression) {
                        val loggerClass = element.resolveMethod()?.containingClass?.qualifiedName
                        if (loggerClass != null && LOGGER_CLASSES.contains(loggerClass)) {
                            val methodName = element.resolveMethod()?.name
                            if (methodName != null && LOGGER_METHODS.contains(methodName)) {
                                val logTemplate = element.argumentList.expressionArguments.firstOrNull()?.run {
                                    (this as? GrLiteral)?.value as? String
                                }

                                if (logTemplate != null) {
                                    log.debug("Found log statement: $logTemplate")
                                    val detectedLogger = DetectedLogger(
                                        logTemplate, methodName, getLineNumber(element) + 1
                                    )
                                    loggerStatements.add(detectedLogger)

                                    //create expression guide mark for the log statement
                                    val guideMark = fileMarker.createExpressionSourceMark(
                                        element, SourceMark.Type.GUIDE
                                    )
                                    if (!fileMarker.containsSourceMark(guideMark)) {
                                        guideMark.putUserData(DETECTED_LOGGER, detectedLogger)
                                        guideMark.apply(true)
                                    } else {
                                        fileMarker.getSourceMark(guideMark.artifactQualifiedName, SourceMark.Type.GUIDE)
                                            ?.putUserData(DETECTED_LOGGER, detectedLogger)
                                    }
                                } else {
                                    log.warn("No log template argument available for expression: $element")
                                }
                            }
                        }
                    }
                    super.visitElement(element)
                }
            })
        }
        return loggerStatements
    }

    private fun getLineNumber(element: PsiElement, start: Boolean = true): Int {
        val document = element.containingFile.viewProvider.document
            ?: PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
        val index = if (start) element.startOffset else element.endOffset
        if (index > (document?.textLength ?: 0)) return 0
        return document?.getLineNumber(index) ?: 0
    }
}
