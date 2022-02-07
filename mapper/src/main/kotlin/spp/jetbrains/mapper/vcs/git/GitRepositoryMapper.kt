/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.mapper.vcs.git

import com.google.common.base.Preconditions
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
import spp.jetbrains.mapper.extend.SourceCodeTokenizer
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
