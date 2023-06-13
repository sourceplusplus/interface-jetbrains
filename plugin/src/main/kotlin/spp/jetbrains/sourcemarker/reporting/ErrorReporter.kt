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
package spp.jetbrains.sourcemarker.reporting

import com.intellij.diagnostic.AbstractMessage
import com.intellij.diagnostic.LogMessage
import com.intellij.diagnostic.ReportMessages
import com.intellij.ide.DataManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.plugins.PluginUtil
import com.intellij.idea.IdeaLogger
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import org.eclipse.egit.github.core.Issue
import org.eclipse.egit.github.core.RepositoryId
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.IssueService
import spp.jetbrains.PluginBundle
import java.awt.Component
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

/**
 * Allows SourceMarker bugs to be anonymously reported to GitHub.
 * Adapted from: https://github.com/JuliaEditorSupport/julia-intellij
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
private object AnonymousFeedback {

    private val log = logger<AnonymousFeedback>()
    private const val gitRepoUser = "sourceplusplus"
    private const val gitRepo = "sourceplusplus"

    /**
     * Makes a connection to GitHub. Checks if there is an issue that is a duplicate and based on this, creates either a
     * new issue or comments on the duplicate (if the user provided additional information).
     *
     * @param environmentDetails Information collected by [getKeyValuePairs]
     * @return The report info that is then used in [GitHubErrorReporter] to show the user a balloon with the link
     * of the created issue.
     */
    fun sendFeedback(environmentDetails: MutableMap<String, String>): SubmittedReportInfo {
        try {
            val client = GitHubClient("github.sourceplus.plus")
            val repoID = RepositoryId(gitRepoUser, gitRepo)
            val issueService = IssueService(client)
            var newGitHubIssue = createNewGibHubIssue(environmentDetails)
            val duplicate = findFirstDuplicate(newGitHubIssue.title, issueService, repoID)
            var isNewIssue = true
            if (duplicate != null) {
                issueService.createComment(repoID, duplicate.number, generateGitHubIssueBody(environmentDetails))
                newGitHubIssue = duplicate
                isNewIssue = false
            } else {
                newGitHubIssue = issueService.createIssue(repoID, newGitHubIssue)
            }
            return SubmittedReportInfo(
                newGitHubIssue.htmlUrl, PluginBundle.message(
                    if (isNewIssue) "git.issue.text" else "git.issue.duplicate.text",
                    newGitHubIssue.htmlUrl,
                    newGitHubIssue.number.toLong()
                ),
                if (isNewIssue) SubmissionStatus.NEW_ISSUE else SubmissionStatus.DUPLICATE
            )
        } catch (e: Exception) {
            log.error("Failed to submit feedback", e)
            return SubmittedReportInfo(
                null,
                PluginBundle.message("report.error.connection.failure"),
                SubmissionStatus.FAILED
            )
        }
    }

    private fun findFirstDuplicate(uniqueTitle: String, service: IssueService, repo: RepositoryId): Issue? {
        val searchParameters = HashMap<String, String>(2)
        searchParameters[IssueService.FILTER_STATE] = IssueService.STATE_OPEN
        return service.pageIssues(repo, searchParameters).flatten().firstOrNull { it.title == uniqueTitle }
    }

    private fun createNewGibHubIssue(details: MutableMap<String, String>) = Issue().apply {
        val errorMessage = details.remove("error.message")?.takeIf(String::isNotBlank) ?: "Unspecified error"
        title = PluginBundle.message("git.issue.title", details.remove("error.hash").orEmpty(), errorMessage)
        details["title"] = title
        body = generateGitHubIssueBody(details)
    }

    private fun generateGitHubIssueBody(details: MutableMap<String, String>) =
        buildString {
            val errorDescription = details.remove("error.description").orEmpty()
            val stackTrace = details.remove("error.stacktrace")?.takeIf(String::isNotBlank) ?: "invalid stacktrace"
            if (errorDescription.isNotEmpty()) append(errorDescription).appendLine("\n\n----------------------\n")
            for ((key, value) in details) append("- ").append(key).append(": ").appendLine(value)
            appendLine("<details><summary>Full StackTrace</summary>")
                .appendLine("<pre><code>")
                .appendLine(stackTrace)
                .appendLine("</code></pre>\n</details>")
        }
}

/**
 * todo: description.
 */
class GitHubErrorReporter : ErrorReportSubmitter() {
    override fun getReportActionText() = PluginBundle.message("report.error.to.plugin.vendor")
    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        // TODO improve
        val event = events.firstOrNull { it.throwable != null } ?: return false
        return doSubmit(event, parentComponent, consumer, additionalInfo)
    }

    private fun doSubmit(
        event: IdeaLoggingEvent,
        parent: Component,
        callback: Consumer<in SubmittedReportInfo>,
        description: String?
    ): Boolean {
        val dataContext = DataManager.getInstance().getDataContext(parent)
        val errorMessage = if (event.data is LogMessage) {
            (event.data as LogMessage).throwable.message
        } else if (event.throwable != null) {
            event.throwable.message
        } else {
            event.message
        }
        val bean = GitHubErrorBean(
            event.throwable,
            IdeaLogger.ourLastActionId,
            description ?: "<No description>",
            errorMessage ?: ""
        )
        PluginUtil.getInstance().findPluginId(event.throwable)?.let { pluginId ->
            PluginManagerCore.getPlugin(pluginId)?.let { ideaPluginDescriptor ->
                if (!ideaPluginDescriptor.isBundled) {
                    bean.pluginName = ideaPluginDescriptor.name
                    bean.pluginVersion = ideaPluginDescriptor.version
                }
            }
        }

        (event.data as? AbstractMessage)?.let { bean.attachments = it.includedAttachments }
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        val reportValues = getKeyValuePairs(
            bean,
            ApplicationInfoEx.getInstanceEx(),
            ApplicationNamesInfo.getInstance()
        )
        val notifyingCallback = CallbackWithNotification(callback, project)
        val task = AnonymousFeedbackTask(
            project, PluginBundle.message(
                "report.error.progress.dialog.text"
            ), true, reportValues, notifyingCallback
        )
        if (project == null) task.run(EmptyProgressIndicator()) else ProgressManager.getInstance().run(task)
        return true
    }

    internal class CallbackWithNotification(
        private val consumer: Consumer<in SubmittedReportInfo>,
        private val project: Project?
    ) : Consumer<SubmittedReportInfo> {
        override fun consume(reportInfo: SubmittedReportInfo) {
            consumer.consume(reportInfo)
            if (reportInfo.status == SubmissionStatus.FAILED) {
                ReportMessages.GROUP.createNotification(
                    ReportMessages.getErrorReport(),
                    reportInfo.linkText, NotificationType.ERROR, null
                ).setImportant(false).notify(project)
            } else {
                ReportMessages.GROUP.createNotification(
                    ReportMessages.getErrorReport(), reportInfo.linkText,
                    NotificationType.INFORMATION, NotificationListener.URL_OPENING_LISTENER
                ).setImportant(false).notify(project)
            }
        }
    }
}

/**
 * Extends the standard class to provide the hash of the thrown exception stack trace.
 */
class GitHubErrorBean(
    throwable: Throwable,
    val lastAction: String?,
    val description: String,
    val message: String
) {
    val exceptionHash: String
    val stackTrace: String

    init {
        val trace = throwable.stackTrace
        exceptionHash = Arrays.hashCode(trace).toString()
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        stackTrace = sw.toString()
    }

    var pluginName = ""
    var pluginVersion = ""
    var attachments: List<Attachment> = emptyList()
}

/**
 * todo: description.
 */
private class AnonymousFeedbackTask(
    project: Project?,
    title: String,
    canBeCancelled: Boolean,
    private val params: MutableMap<String, String>,
    private val callback: Consumer<SubmittedReportInfo>
) : Task.Backgroundable(project, title, canBeCancelled) {
    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true
        callback.consume(AnonymousFeedback.sendFeedback(params))
    }
}

/**
 * Collects information about the running IDEA and the error
 */
private fun getKeyValuePairs(
    error: GitHubErrorBean,
    appInfo: ApplicationInfoEx,
    namesInfo: ApplicationNamesInfo
): MutableMap<String, String> {
    PluginManagerCore.getPlugin(
        PluginId.findId("com.sourceplusplus.plugin.intellij", "com.sourceplusplus.plugin.intellij")
    )?.run {
        if (error.pluginName.isBlank()) error.pluginName = name
        if (error.pluginVersion.isBlank()) error.pluginVersion = version
    }
    val params = mutableMapOf(
        "error.description" to error.description,
        "Plugin Name" to error.pluginName,
        "Plugin Version" to error.pluginVersion,
        "OS Name" to SystemInfo.OS_NAME,
        "Java Version" to SystemInfo.JAVA_VERSION,
        "App Name" to namesInfo.productName,
        "App Full Name" to namesInfo.fullProductName,
        "App Version name" to appInfo.versionName,
        "Is EAP" to java.lang.Boolean.toString(appInfo.isEAP),
        "App Build" to appInfo.build.asString(),
        "App Version" to appInfo.fullVersion,
        "Last Action" to (error.lastAction ?: "Unknown"),
        "error.message" to error.message,
        "error.stacktrace" to error.stackTrace,
        "error.hash" to error.exceptionHash
    )
    for (attachment in error.attachments) params["Attachment ${attachment.name}"] = attachment.encodedBytes
    return params
}
