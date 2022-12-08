/*
 * Source++, the continuous feedback platform for developers.
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
package spp.jetbrains.marker.jvm.detect

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.*
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.plainContent
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import spp.jetbrains.marker.service.ArtifactTypeService
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
        val psiMethod = ApplicationManager.getApplication().runReadAction(Computable {
            guideMark.getPsiMethod()
        })
        determineLoggerStatements(psiMethod, guideMark.sourceFileMarker)
        return guideMark.getChildren().mapNotNull { it.getUserData(DETECTED_LOGGER) }
    }

    fun determineLoggerStatements(
        function: PsiNameIdentifierOwner,
        fileMarker: SourceFileMarker
    ): List<DetectedLogger> {
        val loggerStatements = mutableListOf<DetectedLogger>()
        ApplicationManager.getApplication().runReadAction {
            function.acceptChildren(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is PsiMethodCallExpression) {
                        handleJavaCall(element, loggerStatements, fileMarker)
                    } else if (ArtifactTypeService.isKotlin(element) && element is KtCallExpression) {
                        handleKotlinCall(element, loggerStatements, fileMarker)
                    } else if (ArtifactTypeService.isGroovy(element) && element is GrMethodCallExpression) {
                        handleGroovyCall(element, loggerStatements, fileMarker)
                    }
                    super.visitElement(element)
                }
            })
        }
        return loggerStatements
    }

    private fun handleJavaCall(
        element: PsiMethodCallExpression,
        loggerStatements: MutableList<DetectedLogger>,
        fileMarker: SourceFileMarker
    ) {
        val loggerClass = element.resolveMethod()?.containingClass?.qualifiedName
        if (loggerClass != null && LOGGER_CLASSES.contains(loggerClass)) {
            val methodName = element.resolveMethod()?.name
            if (methodName != null && LOGGER_METHODS.contains(methodName)) {
                val logTemplate = element.argumentList.expressions.firstOrNull()?.run {
                    (this as? PsiLiteral)?.value as? String
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

    private fun handleGroovyCall(
        element: GrMethodCallExpression,
        loggerStatements: MutableList<DetectedLogger>,
        fileMarker: SourceFileMarker
    ) {
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

    private fun handleKotlinCall(
        element: KtCallExpression,
        loggerStatements: MutableList<DetectedLogger>,
        fileMarker: SourceFileMarker
    ) {
        val loggerClass = element.getResolvedCall(element.analyze())
            ?.resultingDescriptor?.fqNameSafe?.parent()?.asString()
        if (loggerClass != null && LOGGER_CLASSES.contains(loggerClass)) {
            val methodName = element.getResolvedCall(element.analyze())
                ?.resultingDescriptor?.name?.asString()
            if (methodName != null && LOGGER_METHODS.contains(methodName)) {
                val logTemplate = element.valueArguments.firstOrNull()?.getArgumentExpression().run {
                    (this as? KtStringTemplateExpression)?.plainContent
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

    private fun getLineNumber(element: PsiElement, start: Boolean = true): Int {
        val document = element.containingFile.viewProvider.document
            ?: PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
        val index = if (start) element.startOffset else element.endOffset
        if (index > (document?.textLength ?: 0)) return 0
        return document?.getLineNumber(index) ?: 0
    }
}
