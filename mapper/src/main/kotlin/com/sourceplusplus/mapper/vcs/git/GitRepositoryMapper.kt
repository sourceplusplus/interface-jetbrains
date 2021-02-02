package com.sourceplusplus.mapper.vcs.git

import com.google.common.base.Preconditions
import com.sourceplusplus.mapper.extend.SourceCodeTokenizer
import jp.ac.titech.c.se.stein.PorcelainAPI
import jp.ac.titech.c.se.stein.core.Context
import jp.ac.titech.c.se.stein.core.EntrySet
import jp.ac.titech.c.se.stein.core.EntrySet.Entry
import jp.ac.titech.c.se.stein.core.EntrySet.EntryList
import jp.ac.titech.c.se.stein.core.RepositoryRewriter
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Repository
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern

/**
 * Based off FinerGit.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class GitRepositoryMapper(private val sourceCodeTokenizer: SourceCodeTokenizer) : RepositoryRewriter() {

    companion object {
        private val log = LoggerFactory.getLogger(GitRepositoryMapper::class.java)
        private val supportedFileTypes = hashSetOf("java", "groovy", "kt")
        val originalCommitIdPattern = Pattern.compile("<OriginalCommitID:(.+)>")!!
    }

    init {
        isPathSensitive = true
    }

    lateinit var sourceRepo: Repository
    lateinit var targetRepo: Repository
    lateinit var targetSourceDirectory: File
    lateinit var targetGit: Git
    val cacheThing = mutableMapOf<String, String>()

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

        val fileType = entry.name.substringAfterLast(".")
        if (!supportedFileTypes.contains(fileType.toLowerCase())) {
            return Entry.EMPTY //unsupported file type
        }
        log.trace("Parsing file: ${entry.name}")

        val fileSource = String(source.readBlob(entry.id, c), StandardCharsets.UTF_8)
        val result = EntryList()
        sourceCodeTokenizer.getMethods(entry.name, fileSource).forEach {
            val newId = target.writeBlob(it.tokens.joinToString("\n").toByteArray(StandardCharsets.UTF_8), c)
            val name = "${it.artifactQualifiedName.identifier}.m${fileType}"
            result.add(Entry(entry.mode, name, newId, entry.directory))
            cacheThing[it.artifactQualifiedName.identifier] = name //todo: consider directory
        }
        return result
    }
}
