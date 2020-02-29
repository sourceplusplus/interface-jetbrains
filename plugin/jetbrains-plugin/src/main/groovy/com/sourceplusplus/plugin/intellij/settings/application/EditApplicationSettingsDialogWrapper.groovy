package com.sourceplusplus.plugin.intellij.settings.application

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.config.SourceAgentConfig
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import org.jetbrains.annotations.Nullable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*

/**
 * todo: description
 *
 * @version 0.2.3
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class EditApplicationSettingsDialogWrapper extends DialogWrapper {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private final EditApplicationSettingsDialog editApplicationSettings = new EditApplicationSettingsDialog()
    private final Project project
    private boolean okayAction

    EditApplicationSettingsDialogWrapper(Project project) {
        super(Objects.requireNonNull(project))
        this.project = project
        init()
        setTitle("Edit Application Settings")
        setResizable(false)

        SourcePluginConfig.current.activeEnvironment.coreClient.getApplication(SourcePluginConfig.current.activeEnvironment.appUuid, {
            if (it.succeeded()) {
                editApplicationSettings.setCurrentProject(it.result().get())
            } else {
                log.error("Failed to get application", it.cause())
            }
        })
    }

    boolean getOkayAction() {
        return okayAction
    }

    String getApplicationName() {
        return editApplicationSettings.getApplicationName()
    }

    @Nullable
    @Override
    JComponent createCenterPanel() {
        return editApplicationSettings.getContentPane()
    }

    @Override
    protected void doOKAction() {
        def sourceApplication = SourceApplication.builder()
                .appUuid(SourcePluginConfig.current.activeEnvironment.appUuid)
                .isUpdateRequest(true)
        def agentConfig = new SourceAgentConfig()
        sourceApplication.agentConfig(agentConfig)
        agentConfig.spanLimitPerSegment = editApplicationSettings.getMaxSpanCount()
        if (editApplicationSettings.getApplicationName()) {
            sourceApplication.appName(editApplicationSettings.getApplicationName())
        }
        if (editApplicationSettings.getApplicationDomain()) {
            agentConfig.packages = editApplicationSettings.getApplicationDomain().split(",")
        }

        SourcePluginConfig.current.activeEnvironment.coreClient.updateApplication(sourceApplication.build(), {
            if (it.failed()) {
                log.error("Failed to update application", it.cause())
            }
        })
        okayAction = true
        super.doOKAction()
    }

    @Override
    void doCancelAction() {
        super.doCancelAction()
    }
}
