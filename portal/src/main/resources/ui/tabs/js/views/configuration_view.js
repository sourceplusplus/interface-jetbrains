function setupUI() {
    if (hideOverviewTab) {
        $('#overview_link').css('display', 'none');
        $('#sidebar_overview_link').css('display', 'none');
    }

    $('#entry_method_toggle').change(function (e) {
        toggledEntryMethod(e.target.checked === true);
    });
    $('#auto_subscribe_toggle').change(function (e) {
        toggledAutoSubscribe(e.target.checked === true);
    });
}
setupUI();

function updateArtifactConfigurationTable(artifact) {
    $('#artifact_qualified_name').text(artifact.artifact_qualified_name);
    $('#artifact_create_date').text(moment.unix(artifact.create_date).format('LLLL'));
    $('#artifact_last_updated').text(moment.unix(artifact.last_updated).format('LLLL'));

    if (artifact.config.endpoint) {
        $('#entry_method_toggle').checkbox("set checked");
    } else {
        $('#entry_method_toggle').checkbox("set unchecked");
    }

    if (artifact.config.subscribe_automatically) {
        $('#auto_subscribe_toggle').checkbox("set checked");
    } else {
        $('#auto_subscribe_toggle').checkbox("set unchecked");
    }

    if (artifact.config.endpoint_name) {
        $('#artifact_endpoint').text(artifact.config.endpoint_name);
    } else if (artifact.config.endpoint_ids != null && artifact.config.endpoint_ids.length > 0) {
        $('#artifact_endpoint').text('true');
    } else {
        $('#artifact_endpoint').text('false');
    }
}