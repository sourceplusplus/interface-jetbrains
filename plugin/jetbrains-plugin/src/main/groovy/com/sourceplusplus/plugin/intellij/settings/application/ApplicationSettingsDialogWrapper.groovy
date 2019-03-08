package com.sourceplusplus.plugin.intellij.settings.application

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.config.SourceAgentConfig
import com.sourceplusplus.api.model.config.SourcePluginConfig
import org.jetbrains.annotations.Nullable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*

/**
 * todo: description
 *
 * @version 0.1.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class ApplicationSettingsDialogWrapper extends DialogWrapper {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private final ApplicationSettingsDialog applicationSettings = new ApplicationSettingsDialog()
    private final Project project
    private final SourceCoreClient coreClient
    private boolean okayAction

    ApplicationSettingsDialogWrapper(Project project, SourceCoreClient coreClient) {
        super(Objects.requireNonNull(project))
        this.project = project
        this.coreClient = coreClient
        init()
        setTitle("Create/Assign Source++ Application")
        setResizable(false)

        coreClient.getApplications({
            if (it.succeeded()) {
                applicationSettings.addExistingApplication(it.result() as SourceApplication[])
            } else {
                log.error("Failed to get applications", it.cause())
            }
        })
    }

    boolean getOkayAction() {
        return okayAction
    }

    @Nullable
    @Override
    JComponent createCenterPanel() {
        return applicationSettings.getContentPane()
    }

    @Override
    protected void doOKAction() {
        if (applicationSettings.getExistingApplication() != null) {
            //assign existing application
            SourcePluginConfig.current.appUuid = applicationSettings.getExistingApplication().appUuid()
            log.info("Application UUID updated: " + SourcePluginConfig.current.appUuid)
        } else {
            //create new application
            def createRequest = SourceApplication.builder().isCreateRequest(true)
                    .appName(applicationSettings.getApplicationName())
            if (!applicationSettings.applicationDomain.isEmpty()) {
                def config = new SourceAgentConfig()
                config.packages = applicationSettings.getApplicationDomain().split(",")
                createRequest.agentConfig(config)
            }

            def newApplication = coreClient.createApplication(createRequest.build())
            SourcePluginConfig.current.appUuid = newApplication.appUuid()
            log.info("Application UUID updated: " + SourcePluginConfig.current.appUuid)
        }

        okayAction = true
        super.doOKAction()
    }

    @Override
    void doCancelAction() {
        super.doCancelAction()
    }
}
