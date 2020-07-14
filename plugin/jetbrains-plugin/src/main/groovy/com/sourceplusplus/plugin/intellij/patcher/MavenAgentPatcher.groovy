package com.sourceplusplus.plugin.intellij.patcher

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import groovy.util.logging.Slf4j

/**
 * Adds the Source++ Agent to any Maven-based project executions.
 *
 * @version 0.3.2
 * @since 0.2.4
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class MavenAgentPatcher extends JavaProgramPatcher implements SourceAgentPatcher {

    @Override
    void patchJavaParameters(Executor executor, RunProfile configuration, JavaParameters javaParameters) {
        patchAgent()

        if (agentFile != null) {
            javaParameters.getVMParametersList()?.add("-javaagent:$agentFile.absolutePath")
            log.info("Attached Source++ Agent to executing program")
        }
    }
}
