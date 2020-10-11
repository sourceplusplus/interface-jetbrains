console.log("Configuration tab started");

eb.onopen = function () {
    portalConnected();
    if (requiresRegistration) {
        return;
    }

    eb.registerHandler(portalUuid + '-DisplayArtifactConfiguration', function (error, message) {
        updateArtifactConfigurationTable(message.body);
    });

    eb.publish('ConfigurationTabOpened', {'portalUuid': portalUuid});
};

function toggledEntryMethod(entryMethod) {
    eb.send('UpdateArtifactEntryMethod', {'portalUuid': portalUuid, "entry_method": entryMethod});
}

function toggledAutoSubscribe(autoSubscribe) {
    eb.send('UpdateArtifactAutoSubscribe', {'portalUuid': portalUuid, "auto_subscribe": autoSubscribe});
}