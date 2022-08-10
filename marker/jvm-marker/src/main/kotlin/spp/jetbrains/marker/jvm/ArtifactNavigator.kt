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
package spp.jetbrains.marker.jvm

import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.FilenameIndex.getFilesByName
import com.intellij.psi.search.GlobalSearchScope.allScope
import com.intellij.util.PsiNavigateUtil
import io.vertx.core.*
import io.vertx.kotlin.coroutines.await
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import spp.jetbrains.marker.source.JVMMarkerUtils
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.exception.LiveStackTraceElement
import spp.protocol.artifact.exception.sourceAsFilename
import spp.protocol.artifact.exception.sourceAsLineNumber

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ArtifactNavigator {

    private val log = logger<ArtifactNavigator>()

    //todo: remove method from method names and support navigating to classes?

    fun navigateTo(project: Project, element: LiveStackTraceElement) {
        ApplicationManager.getApplication().runReadAction {
            val foundFiles = getFilesByName(project, element.sourceAsFilename()!!, allScope(project))
            if (foundFiles.isNotEmpty()) {
                val file = foundFiles[0]
                val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file)!!
                val offset = document.getLineStartOffset(element.sourceAsLineNumber()!! - 1)

                ApplicationManager.getApplication().invokeLater {
                    PsiNavigationSupport.getInstance().createNavigatable(project, file.virtualFile, offset)
                        .navigate(true)
                }
            }
        }
    }

    suspend fun navigateTo(
        vertx: Vertx,
        artifactQualifiedName: ArtifactQualifiedName,
        handler: Handler<AsyncResult<Boolean>>
    ) {
        val artifactPsi = ArtifactSearch.findArtifact(vertx, artifactQualifiedName)
        if (artifactPsi != null) {
            ApplicationManager.getApplication().invokeLater {
                PsiNavigateUtil.navigate(artifactPsi)
                log.info("Navigated to artifact: $artifactQualifiedName")
                handler.handle(Future.succeededFuture(true))
            }
        } else {
            log.warn("Could not find artifact: $artifactQualifiedName")
            handler.handle(Future.succeededFuture(false))
        }
    }

    suspend fun canNavigateTo(project: Project, artifactQualifiedName: ArtifactQualifiedName): Boolean {
        val promise = Promise.promise<Boolean>()
        ApplicationManager.getApplication().invokeLater {
            promise.complete(canNavigateToMethod(project, artifactQualifiedName))
        }
        return promise.future().await()
    }

    fun canNavigateToMethod(project: Project, artifactQualifiedName: ArtifactQualifiedName): Boolean {
        val classQualifiedName = JVMMarkerUtils.getQualifiedClassName(artifactQualifiedName)
        val psiClass = JavaPsiFacade.getInstance(project).findClass(classQualifiedName.identifier, allScope(project))
        for (theMethod in psiClass!!.methods) {
            val uMethod = theMethod.toUElement() as UMethod
            val qualifiedName = JVMMarkerUtils.getFullyQualifiedName(uMethod)
            if (qualifiedName == artifactQualifiedName) {
                return true
            }
        }
        return false
    }
}
