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
package spp.jetbrains.marker.service

import com.intellij.codeInsight.actions.VcsFacadeImpl
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListChange
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vcs.ex.LocalRange
import com.intellij.openapi.vcs.ex.PartialLocalLineStatusTracker
import com.intellij.openapi.vcs.ex.Range
import com.intellij.openapi.vcs.ex.createRanges
import com.intellij.openapi.vcs.impl.LineStatusTrackerManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.util.Function
import com.intellij.util.SmartList
import com.intellij.util.concurrency.FutureResult
import com.intellij.util.containers.ContainerUtil
import spp.jetbrains.artifact.service.ArtifactTypeService
import java.util.*

/**
 * Responsible for determining the changes/versions of source code artifacts.
 *
 * @since 0.7.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ArtifactVersionService {

    private val log = logger<ArtifactVersionService>()

    fun getChangedFunctions(psiFile: PsiFile): List<PsiNameIdentifierOwner> {
        log.info("Getting changed functions for file: $psiFile")
        val project = psiFile.project
        val ref = FutureResult<List<PsiNameIdentifierOwner>>()
        ChangeListManager.getInstance(project).invokeAfterUpdate(false) {
            val changes = ChangeListManager.getInstance(project)
                .getChangesIn(psiFile.virtualFile).toTypedArray()
            try {
                val changedFunctions = ReadAction.compute(ThrowableComputable {
                    getChangedElements(project, changes) {
                        if (DumbService.isDumb(project) || project.isDisposed || !it.isValid) {
                            return@getChangedElements emptyList()
                        }
                        val index = ProjectFileIndex.getInstance(project)
                        if (!index.isInSource(it)) {
                            return@getChangedElements emptyList()
                        }

                        val physicalFunctions: List<PsiNameIdentifierOwner> = SmartList()
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

    /**
     * Taken from [VcsFacadeImpl.getChangedElements]
     */
    @Suppress("LoopWithTooManyJumpStatements") // copied code
    private fun <T : PsiElement> getChangedElements(
        project: Project,
        changes: Array<Change>,
        elementsConvertor: Function<in VirtualFile, out List<T>>
    ): List<T> {
        val result: MutableList<T> = SmartList()
        for (change in changes) {
            if (change.type == Change.Type.DELETED) continue
            if (change.afterRevision !is CurrentContentRevision) continue
            val file = (change.afterRevision as CurrentContentRevision?)?.virtualFile ?: continue
            val document = FileDocumentManager.getInstance().getDocument(file) ?: continue
            val apply = elementsConvertor.`fun`(file)
            val elements = if (apply == null) null else ContainerUtil.skipNulls(apply)
            if (ContainerUtil.isEmpty(elements)) continue
            val changedLines = getChangedLines(project, document, change)
            if (changedLines != null) {
                for (element in elements!!) {
                    if (isElementChanged(element, document, changedLines)) {
                        result.add(element)
                    }
                }
            } else {
                result.addAll(elements!!)
            }
        }
        return result
    }

    /**
     * Taken from [VcsFacadeImpl.getChangedLines]
     */
    private fun getChangedLines(project: Project, document: Document, change: Change): BitSet? {
        if (change.type == Change.Type.NEW) return null
        val ranges = getChangedRanges(project, document, change) ?: return null
        val changedLines = BitSet()
        for (range in ranges) {
            if (!range.hasLines()) {
                if (range.hasVcsLines()) {
                    changedLines[0.coerceAtLeast(range.line1 - 1)] = range.line1 + 1
                }
            } else {
                changedLines[range.line1] = range.line2
            }
        }
        return changedLines
    }

    /**
     * Taken from [VcsFacadeImpl.getChangedRanges]
     */
    private fun getChangedRanges(project: Project, document: Document, change: Change): List<Range>? {
        val tracker = LineStatusTrackerManager.getInstance(project).getLineStatusTracker(document)
        return if (tracker != null) {
            if (change is ChangeListChange && tracker is PartialLocalLineStatusTracker) {
                val changeListId = change.changeListId
                val ranges = tracker.getRanges()
                if (ranges != null) {
                    ContainerUtil.filter(ranges) { range: LocalRange -> range.changelistId == changeListId }
                } else {
                    null
                }
            } else {
                tracker.getRanges()
            }
        } else {
            val contentFromVcs = getRevisionedContentFrom(change)
            if (contentFromVcs != null) {
                getRanges(document, contentFromVcs)
            } else {
                null
            }
        }
    }

    /**
     * Taken from [VcsFacadeImpl.isElementChanged]
     */
    private fun isElementChanged(element: PsiElement, document: Document, changedLines: BitSet): Boolean {
        val textRange = element.textRange
        val startLine = document.getLineNumber(textRange.startOffset)
        val endLine = if (textRange.isEmpty) startLine + 1 else document.getLineNumber(textRange.endOffset - 1) + 1
        val nextSetBit = changedLines.nextSetBit(startLine)
        return nextSetBit != -1 && nextSetBit < endLine
    }

    /**
     * Taken from [VcsFacadeImpl.getRevisionedContentFrom]
     */
    private fun getRevisionedContentFrom(change: Change): String? {
        val revision = change.beforeRevision ?: return null
        return try {
            revision.content
        } catch (e: VcsException) {
            log.warn("Can't get content for: " + change.virtualFile, e)
            null
        }
    }

    /**
     * Taken from [VcsFacadeImpl.getRanges]
     */
    private fun getRanges(
        document: Document,
        contentFromVcs: CharSequence
    ): List<Range> {
        return createRanges(
            document.immutableCharSequence,
            StringUtilRt.convertLineSeparators(contentFromVcs, "\n")
        )
    }
}
