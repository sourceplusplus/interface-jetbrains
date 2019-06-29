$('#overview_link').attr('href', "overview.html" + mainGetQuery);
$('#sidebar_overview_link').attr('href', "overview.html" + mainGetQuery);

$('#traces_link_latest').attr('href', "traces.html" + mainGetQuery + '&order_type=latest_traces');
$('#traces_link_slowest').attr('href', "traces.html" + mainGetQuery + '&order_type=slowest_traces');
$('#sidebar_traces_link_latest').attr('href', "traces.html" + mainGetQuery + '&order_type=latest_traces');
$('#sidebar_traces_link_slowest').attr('href', "traces.html" + mainGetQuery + '&order_type=slowest_traces');

$('#configuration_link').attr('href', "configuration.html" + mainGetQuery);
$('#sidebar_configuration_link').attr('href', "configuration.html" + mainGetQuery);

var viewingInnerTrace = false;
eb.onopen = function () {
    portalConnected();
    if (requiresRegistration) {
        return;
    }

    eb.registerHandler(portalUuid + '-DisplayTraces', function (error, message) {
        displayTraces(message.body);
    });
    eb.registerHandler(portalUuid + '-DisplayInnerTraceStack', function (error, message) {
        displayInnerTraces(message);
    });
    eb.registerHandler(portalUuid + '-DisplayTraceStack', function (error, message) {
        eb.send('PortalLogger', 'Displaying trace stack: ' + JSON.stringify(message));
        console.log('Displaying trace stack: ' + JSON.stringify(message));
        displayTraceStack(message.body);
    });
    eb.registerHandler(portalUuid + '-DisplaySpanInfo', function (error, message) {
        eb.send('PortalLogger', 'Displaying trace span info: ' + JSON.stringify(message));
        console.log('Displaying trace span info: ' + JSON.stringify(message));
        displaySpanInfo(message.body);
    });

    eb.publish('TracesTabOpened', {'portal_uuid': portalUuid, 'trace_order_type': traceOrderType});
};

function clickedDisplaySpanInfo(appUuid, rootArtifactQualifiedName, traceId, segmentId, spanId) {
    eb.send('ClickedDisplaySpanInfo', {
        'portal_uuid': portalUuid,
        'app_uuid': appUuid, 'artifact_qualified_name': rootArtifactQualifiedName,
        'trace_id': traceId, 'segment_id': segmentId, 'span_id': spanId
    });
}

function clickedDisplayTraceStack(appUuid, artifactQualifiedName, globalTraceId) {
    eb.send('ClickedDisplayTraceStack', {
        'portal_uuid': portalUuid,
        'app_uuid': appUuid,
        'artifact_qualified_name': artifactQualifiedName,
        'trace_id': globalTraceId
    });
}

function clickedBackToTraces() {
    eb.send('ClickedDisplayTraces', {
        'portal_uuid': portalUuid
    });
}

function clickedBackToTraceStack() {
    eb.send('ClickedDisplayTraceStack', {
        'portal_uuid': portalUuid
    });
}