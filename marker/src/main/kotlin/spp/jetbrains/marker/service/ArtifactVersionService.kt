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
package spp.jetbrains.marker.service

import com.intellij.codeInsight.actions.VcsFacadeImpl
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.psi.*
import com.intellij.util.SmartList
import com.intellij.util.concurrency.FutureResult
import com.intellij.util.containers.ContainerUtil

/**
 * Responsible for determining the changes/versions of source code artifacts.
 *
 * @since 0.7.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ArtifactVersionService {

    private val log = logger<ArtifactVersionService>()

    fun getChangedFunctions(psiFile: PsiFile): List<PsiNamedElement> {
        log.info("Getting changed functions for file: $psiFile")
        val project = psiFile.project
        val ref = FutureResult<List<PsiNamedElement>>()
        ChangeListManager.getInstance(project).invokeAfterUpdate(false) {
            val changes = ChangeListManager.getInstance(project)
                .getChangesIn(psiFile.virtualFile).toTypedArray()
            try {
                val changedFunctions = ReadAction.compute(ThrowableComputable {
                    VcsFacadeImpl.getVcsInstance().getChangedElements(project, changes) {
                        if (DumbService.isDumb(project) || project.isDisposed || !it.isValid) {
                            return@getChangedElements emptyList()
                        }
                        val index = ProjectFileIndex.getInstance(project)
                        if (!index.isInSource(it)) {
                            return@getChangedElements emptyList()
                        }

                        val physicalFunctions: List<PsiNamedElement> = SmartList()
                        PsiManager.getInstance(project).findFile(it)
                            ?.accept(object : PsiRecursiveElementWalkingVisitor() {
                                override fun visitElement(element: PsiElement) {
                                    if (ArtifactTypeService.isFunction(element)) {
                                        ContainerUtil.addAllNotNull(physicalFunctions, element)
                                    }
                                    super.visitElement(element)
                                }
                            })
                        return@getChangedElements physicalFunctions
                    }
                })
                ref.set(changedFunctions)
                log.info("Found ${changedFunctions.size} changed functions for file: $psiFile")
            } catch (e: IndexOutOfBoundsException) {
                //file is still being modified, so we'll just return an empty list
                log.warn("Unable to get changed functions for file: $psiFile", e)
                ref.set(emptyList())
            } catch (e: Exception) {
                log.error("Error getting changed functions for file: $psiFile", e)
                ref.setException(e)
            }
        }

        return ProgressIndicatorUtils.awaitWithCheckCanceled(ref)
    }
}
