package com.sourceplusplus.plugin.intellij.patcher

import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import groovy.util.logging.Slf4j
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager
import org.jetbrains.plugins.gradle.service.task.GradleTaskManagerExtension
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings

/**
 * Adds the Source++ Agent to any Gradle-based project executions.
 *
 * @version 0.2.6
 * @since 0.2.4
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class GradleAgentPatcher implements GradleTaskManagerExtension, SourceAgentPatcher {

    @Override
    boolean executeTasks(@NotNull ExternalSystemTaskId id,
                         @NotNull List<String> taskNames,
                         @NotNull String projectPath,
                         @Nullable GradleExecutionSettings settings,
                         @Nullable String jvmParametersSetup,
                         @NotNull ExternalSystemTaskNotificationListener listener) throws ExternalSystemException {
        patchAgent()

        if (agentFile != null) {
            def initScript = settings.getUserData(GradleTaskManager.INIT_SCRIPT_KEY).toString()
            if (!initScript.contains('main = mainClass\n' +
                    '            \n' +
                    '            if(_workingDir) workingDir = _workingDir')) {
                //https://youtrack.jetbrains.com/issue/IDEA-234593
                def newInitScript = ""
                initScript.eachLine {
                    if (!it.startsWith("jvmArgs")) {
                        newInitScript += it + "\n"
                    }
                }
                initScript = newInitScript
            }

            initScript = initScript.replace('if(_workingDir) workingDir = _workingDir',
                    "jvmArgs '-javaagent:" + agentFile + "'\n\n" +
                            "            if(_workingDir) workingDir = _workingDir")
            settings.putUserData(GradleTaskManager.INIT_SCRIPT_KEY, initScript)
            log.info("Attached Source++ Agent to executing program")
        }

        List<String> vmOptions = settings != null ? settings.getJvmArguments() : Collections.emptyList()
        List<String> arguments = settings != null ? settings.getArguments() : Collections.emptyList()
        return executeTasks(id, taskNames, projectPath, settings, vmOptions, arguments, jvmParametersSetup, listener)
    }

    @Override
    boolean cancelTask(@NotNull ExternalSystemTaskId id, @NotNull ExternalSystemTaskNotificationListener listener) throws ExternalSystemException {
        return false
    }
}
