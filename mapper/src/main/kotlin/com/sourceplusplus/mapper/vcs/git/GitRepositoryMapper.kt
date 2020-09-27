package com.sourceplusplus.mapper.vcs.git

import com.google.common.base.Preconditions
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.sourceplusplus.marker.MarkerUtils.Companion.getFullyQualifiedName
import jp.ac.titech.c.se.stein.PorcelainAPI
import jp.ac.titech.c.se.stein.core.Context
import jp.ac.titech.c.se.stein.core.EntrySet
import jp.ac.titech.c.se.stein.core.EntrySet.Entry
import jp.ac.titech.c.se.stein.core.EntrySet.EntryList
import jp.ac.titech.c.se.stein.core.RepositoryRewriter
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Repository
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern

/**
 * Based off FinerGit.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class GitRepositoryMapper(private val project: Project) : RepositoryRewriter() {
    //todo: ability to add to existing mapped repo

    companion object {
        private val log = LoggerFactory.getLogger(GitRepositoryMapper::class.java)
        private val supportedFileTypes = hashSetOf("java", "groovy", "kotlin", "scala")
        val originalCommitIdPattern = Pattern.compile("<OriginalCommitID:(.+)>")!!
    }

    init {
        isPathSensitive = true
    }

    lateinit var sourceRepo: Repository
    lateinit var targetRepo: Repository
    lateinit var targetSourceDirectory: File
    lateinit var targetGit: Git

    fun initialize(sourceRepo: Repository) {
        val tempDir = File("/tmp/tmp-repo-${UUID.randomUUID()}/.git")
        Preconditions.checkArgument(tempDir.mkdirs())
        val fileRepo = FileRepository(tempDir)
        fileRepo.create()

        initialize(sourceRepo, fileRepo)
    }

    fun reinitialize() {
        super.initialize(sourceRepo, targetRepo)
        rewrite(Context.init())
        PorcelainAPI(targetRepo).use {
            it.resetHard()
            it.clean()
        }
    }

    override fun initialize(sourceRepo: Repository, targetRepo: Repository) {
        this.sourceRepo = sourceRepo
        this.targetRepo = targetRepo
        targetSourceDirectory = targetRepo.directory.parentFile
        targetGit = Git.wrap(targetRepo)

        super.initialize(sourceRepo, targetRepo)
        rewrite(Context.init())
        PorcelainAPI(targetRepo).use {
            it.resetHard()
            it.clean()
        }
    }

    override fun rewriteCommitMessage(message: String, c: Context): String {
        return "<OriginalCommitID:${c.commitId}>"
    }

    override fun rewriteEntry(entry: Entry, c: Context): EntrySet {
        if (entry.isTree) {
            return super.rewriteEntry(entry, c)
        }

        val fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(entry.name)
        if (!supportedFileTypes.contains(fileType.name.toLowerCase())) {
            return Entry.EMPTY //unsupported file type
        }

        val fileSource = String(source.readBlob(entry.id, c), StandardCharsets.UTF_8)
        val psiFile = PsiFileFactory.getInstance(project).createFileFromText(entry.name, fileType, fileSource)
        log.debug("Parsing file: $psiFile")

        val result = EntryList()
        val uFile = psiFile.toUElement() as UFile
        uFile.classes.forEach { uClass ->
            uClass.methods.forEach {
                val tokenStr = StringBuilder()
                it.javaPsi.accept(object : JavaRecursiveElementVisitor() {
                    override fun visitElement(element: PsiElement) {
                        if (element is LeafPsiElement && element.text.isNotBlank()) {
                            tokenStr.append(element.text).append("\n")
                        }
                        super.visitElement(element)
                    }
                })

                val newId = target.writeBlob(tokenStr.toString().toByteArray(StandardCharsets.UTF_8), c)
                val name = "${getFullyQualifiedName(it)}.m${uFile.lang.associatedFileType!!.defaultExtension}"
                result.add(Entry(entry.mode, name, newId, entry.directory))
            }
        }

        return result
    }
} //git log --all --full-history -- <path-to-file>
