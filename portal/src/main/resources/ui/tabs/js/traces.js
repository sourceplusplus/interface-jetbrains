console.log("Traces tab started");

eb.onopen = function () {
    portalConnected();
    if (requiresRegistration) {
        return;
    }

    eb.registerHandler(portalUuid + '-DisplayTraces', function (error, message) {
        displayTraces(message.body);
    });
    eb.registerHandler(portalUuid + '-DisplayInnerTraceStack', function (error, message) {
        displayInnerTraces(message.body);
    });
    eb.registerHandler(portalUuid + '-DisplayTraceStack', function (error, message) {
        displayTraceStack(message.body);
    });
    eb.registerHandler(portalUuid + '-DisplaySpanInfo', function (error, message) {
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