/*
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

    eb.publish('TracesTabOpened', {'portalUuid': portalUuid, 'traceOrderType': traceOrderType});
};

function clickedDisplaySpanInfo(appUuid, rootArtifactQualifiedName, traceId, segmentId, spanId) {
    eb.send('ClickedDisplaySpanInfo', {
        'portalUuid': portalUuid,
        'appUuid': appUuid, 'artifactQualifiedName': rootArtifactQualifiedName,
        'traceId': traceId, 'segmentId': segmentId, 'spanId': spanId
    });
}

function clickedDisplayTraceStack(appUuid, artifactQualifiedName, globalTraceId) {
    eb.send('ClickedDisplayTraceStack', {
        'portalUuid': portalUuid,
        'appUuid': appUuid,
        'artifactQualifiedName': artifactQualifiedName,
        'traceId': globalTraceId
    });
}

function clickedBackToTraces() {
    eb.send('ClickedDisplayTraces', {
        'portalUuid': portalUuid
    });
}

function clickedBackToTraceStack() {
    eb.send('ClickedDisplayTraceStack', {
        'portalUuid': portalUuid
    });
}
*/
