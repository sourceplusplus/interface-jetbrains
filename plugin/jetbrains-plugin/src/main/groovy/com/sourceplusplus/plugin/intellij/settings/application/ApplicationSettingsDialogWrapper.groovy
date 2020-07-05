package com.sourceplusplus.plugin.intellij.settings.application

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.sourceplusplus.api.APIException
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.config.SourceAgentConfig
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.intellij.IntelliJStartupActivity
import groovy.util.logging.Slf4j
import io.vertx.core.json.Json
import org.jetbrains.annotations.Nullable

import javax.swing.*

/**
 * Used to create a new application or pick an existing application to associate with the current project.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class ApplicationSettingsDialogWrapper extends DialogWrapper {

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

    ApplicationSettingsDialog getApplicationSettingsDialog() {
        return applicationSettings
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
            SourcePluginConfig.current.activeEnvironment.appUuid = applicationSettings.getExistingApplication().appUuid()
        } else {
            //create new application
            def createRequest = SourceApplication.builder().isCreateRequest(true)
                    .appName(applicationSettings.getApplicationName())
            if (!applicationSettings.applicationDomain.isEmpty()) {
                def config = new SourceAgentConfig()
                config.packages = applicationSettings.getApplicationDomain().split(",")
                createRequest.agentConfig(config)
            }

            try {
                def newApplication = coreClient.createApplication(createRequest.build())
                SourcePluginConfig.current.activeEnvironment.appUuid = newApplication.appUuid()
            } catch (APIException ex) {
                Messages.showMessageDialog(project, ex.message,
                        "Create Application - API Exception", Messages.getErrorIcon())
                return
            }
        }
        SourcePortalConfig.current.addCoreClient(SourcePluginConfig.current.activeEnvironment.appUuid, coreClient)
        if (PluginBootstrap.getSourcePlugin() == null) {
            IntelliJStartupActivity.startSourcePlugin(coreClient)
            SourceMarkerPlugin.INSTANCE.refreshAvailableSourceFileMarkers(true)
        }
        PropertiesComponent.getInstance().setValue(
                "spp_plugin_config", Json.encode(SourcePluginConfig.current))
        IntelliJStartupActivity.currentProject.save()
        log.info("Application UUID updated: " + SourcePluginConfig.current.activeEnvironment.appUuid)

        okayAction = true
        super.doOKAction()
    }

    @Override
    void doCancelAction() {
        super.doCancelAction()
    }
}
