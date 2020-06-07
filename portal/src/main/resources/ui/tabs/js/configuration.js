$('#overview_link').attr('href', "overview.html" + mainGetQuery);
$('#sidebar_overview_link').attr('href', "overview.html" + mainGetQuery);

$('#traces_link_latest').attr('href', "traces.html" + mainGetQuery + '&order_type=latest_traces');
$('#traces_link_slowest').attr('href', "traces.html" + mainGetQuery + '&order_type=slowest_traces');
$('#traces_link_failed').attr('href', "traces.html" + mainGetQuery + '&order_type=failed_traces');
$('#sidebar_traces_link_latest').attr('href', "traces.html" + mainGetQuery + '&order_type=latest_traces');
$('#sidebar_traces_link_slowest').attr('href', "traces.html" + mainGetQuery + '&order_type=slowest_traces');
$('#sidebar_traces_link_failed').attr('href', "traces.html" + mainGetQuery + '&order_type=failed_traces');

$('#configuration_link').attr('href', "configuration.html" + mainGetQuery);
$('#sidebar_configuration_link').attr('href', "configuration.html" + mainGetQuery);

eb.onopen = function () {
    portalConnected();
    if (requiresRegistration) {
        return;
    }

    eb.registerHandler(portalUuid + '-DisplayArtifactConfiguration', function (error, message) {
        updateArtifactConfigurationTable(message.body);
    });

    eb.publish('ConfigurationTabOpened', {'portal_uuid': portalUuid});
};

function toggledEntryMethod(entryMethod) {
    eb.send('UpdateArtifactEntryMethod', {'portal_uuid': portalUuid, "entry_method": entryMethod});
}

function toggledForceSubscription(forceSubscribe) {
    eb.send('UpdateArtifactForceSubscribe', {'portal_uuid': portalUuid, "force_subscribe": forceSubscribe});
}