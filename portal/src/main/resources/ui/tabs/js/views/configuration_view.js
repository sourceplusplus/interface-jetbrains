function updateArtifactConfigurationTable(artifact) {
    $('#artifact_qualified_name').text(artifact.artifact_qualified_name);
    $('#artifact_create_date').text(moment.unix(artifact.create_date).format('LLLL'));
    $('#artifact_last_updated').text(moment.unix(artifact.last_updated).format('LLLL'));

    if (artifact.config && artifact.config.endpoint_name) {
        $('#artifact_endpoint').text(artifact.config.endpoint_name + " (id: " + artifact.config.endpoint_ids[0] + ")");
    } else if (artifact.config && artifact.config.endpoint_ids) {
        $('#artifact_endpoint').text("id: " + artifact.config.endpoint_ids[0]);
    }

    if (artifact.config && (artifact.config.force_subscribe || artifact.config.subscribe_automatically)) {
        $('#artifact_auto_subscribe').text('true');

        if (artifact.config.force_subscribe) {
            $('#force_subscribe_toggle').checkbox("set checked");
        }
    } else {
        $('#artifact_auto_subscribe').text('false');
        $('#force_subscribe_toggle').checkbox("set unchecked");
    }
}