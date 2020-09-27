console.log("Configuration tab started");

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

function toggledAutoSubscribe(autoSubscribe) {
    eb.send('UpdateArtifactAutoSubscribe', {'portal_uuid': portalUuid, "auto_subscribe": autoSubscribe});
}