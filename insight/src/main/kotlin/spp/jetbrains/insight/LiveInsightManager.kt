/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.insight

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import spp.jetbrains.insight.contributor.FunctionDurationContributor
import spp.jetbrains.marker.source.mark.api.ClassSourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.safeLaunch

/**
 * When remote insights are unavailable, we fall back on local insights. Local insights require the
 * developer to continuously perform insight calculations on their own machine and do not have
 * the ability to share insights with other developers. Local insights also cannot trigger automatic
 * insight instrumentation to perform more precise calculations.
 */
class LiveInsightManager(
    private val project: Project,
    private val remoteInsightsAvailable: Boolean
) : CoroutineVerticle(), SourceMarkEventListener, Disposable {

    private val log = logger<LiveInsightManager>()

    //    private lateinit var insightService: LiveInsightService
//    private lateinit var workspace: InsightWorkspace
    private val contributors = listOf(
        FunctionDurationContributor(remoteInsightsAvailable)
    )

    override suspend fun start() {
        if (remoteInsightsAvailable) {
            startRemoteInsights()
        }
    }

    private suspend fun startRemoteInsights() {
//        val analyzer = SourceAnalyzer(object : InsightStorage {
//            override suspend fun <T> getInsightValue(type: InsightType, vararg args: PsiElement): T? {
//                if (type == InsightType.FUNCTION_DURATION) {
//                    val methodCall = args[0].reference
//                    if (methodCall is PsiMethodCallExpression) {
//                        val method = (args[0] as PsiMethodCallExpression).resolveMethod()
//                        if (method != null && method.nameIdentifier != null) {
////                            val resolvedMark = method.nameIdentifier!!.getUserData(SourceKey.GuideMark)
////                            val duration = TODO() //resolvedMark?.getMethodDurationInsight()
////                            if (duration != null) {
////                                return duration as T
////                            } else {
////                                //query insight service
////                                TODO()
////                            }
//                        }
//                    }
//                }
//                //                val insights = insightService.getArtifactInsights(
//                //                    workspace.id,
//                //                    mark.artifactQualifiedName,
//                //                    JsonArray().add(type.name)
//                //                ).await()
//                //                println(insights)
//                return 200L as T
//            }
//        })
//
//        insightService = UserData.liveInsightService(project)!!
//        if (insightService.getWorkspaces().await().isEmpty()) {
//            workspace = insightService.createWorkspace().await()
//        } else {
//            workspace = insightService.getWorkspaces().await().first()
//        }
//        log.debug("Created workspace: ${workspace.id}")
    }

    override fun handleEvent(event: SourceMarkEvent) {
        when (event.eventCode) {
            SourceMarkEventCode.MARK_ADDED -> {
                if (event.sourceMark.isClassMark) {
                    if (remoteInsightsAvailable) {
                        vertx.safeLaunch { downloadInsights(event.sourceMark as ClassSourceMark) }
                    }
                }
            }
        }

        contributors.forEach { it.handleEvent(event) }
    }

    private suspend fun downloadInsights(classMark: ClassSourceMark) {
//        //get head commit
//        val change = ChangeListManager.getInstance(classMark.project)
//            .getChange(classMark.sourceFileMarker.psiFile.virtualFile)
//        println(change)
//
//        //upload source code
//        insightService.uploadSourceCode(
//            workspace.id,
//            JsonObject()
//                .put("file_path", classMark.sourceFileMarker.psiFile.virtualFile.path)
//                .put("file_content", Buffer.buffer(classMark.sourceFileMarker.psiFile.text))
//        ).await()
//
//        //trigger scan
//        insightService.scanWorkspace(workspace.id).await()
//
//        //download insights
//        classMark.sourceFileMarker.getGuideMarks().filter { it.isMethodMark }.forEach {
//            val insights = insightService.getArtifactInsights(
//                workspace.id,
//                it.artifactQualifiedName,
//                JsonArray().add(InsightType.FUNCTION_DURATION.name)
//            ).await()
//            it.putUserData(
//                SourceMarkerKeys.FUNCTION_DURATION,
//                InsightValue.of(
//                    InsightType.FUNCTION_DURATION,
//                    insights.getString(InsightType.FUNCTION_DURATION.name).toLong()
//                )
//            )
//            println(insights)
//        }
    }

    override suspend fun stop() = Disposer.dispose(this)
    override fun dispose() = Unit
}
