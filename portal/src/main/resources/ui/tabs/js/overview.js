console.log("Overview tab started");

var currentMetricType = "Throughput_Average";
var currentTimeFrame = "LAST_5_MINUTES";

console.log("Connecting portal");
eb.onopen = function () {
    portalConnected();
    if (requiresRegistration) {
        return;
    }
    clickedViewAverageResponseTimeChart(); //default = avg resp time

    eb.registerHandler(portalUuid + '-ClearOverview', function (error, message) {
        clearOverview();
    });
    eb.registerHandler(portalUuid + '-DisplayCard', function (error, message) {
        displayCard(message.body);
    });
    eb.registerHandler(portalUuid + '-UpdateChart', function (error, message) {
        updateChart(message.body);
    });

    var timeFrame = localStorage.getItem('spp.metric_time_frame');
    if (timeFrame == null) {
        localStorage.setItem('spp.metric_time_frame', timeFrame = currentTimeFrame);
    }
    updateTime(timeFrame);
    portalLog('Set initial time frame to: ' + timeFrame);

    eb.publish('OverviewTabOpened', {'portal_uuid': portalUuid});
};

function updateTime(interval) {
    console.log("Update time: " + interval);
    currentTimeFrame = interval.toUpperCase();
    localStorage.setItem('spp.metric_time_frame', interval);
    eb.send('SetMetricTimeFrame', {'portal_uuid': portalUuid, 'metric_time_frame': interval});

    $('#last_5_minutes_time').removeClass('active');
    $('#last_15_minutes_time').removeClass('active');
    $('#last_30_minutes_time').removeClass('active');
    $('#last_hour_time').removeClass('active');
    $('#last_3_hours_time').removeClass('active');

    $('#' + interval.toLowerCase() + '_time').addClass('active');
}

function clickedViewAverageThroughputChart() {
    console.log("Clicked view average throughput");
    currentMetricType = "Throughput_Average";
    eb.send('SetActiveChartMetric', {'portal_uuid': portalUuid, 'metric_type': currentMetricType});
}

function clickedViewAverageResponseTimeChart() {
    console.log("Clicked view average response time");
    currentMetricType = "ResponseTime_Average";
    eb.send('SetActiveChartMetric', {'portal_uuid': portalUuid, 'metric_type': currentMetricType});
}

function clickedViewAverageSLAChart() {
    console.log("Clicked view average SLA");
    currentMetricType = "ServiceLevelAgreement_Average";
    eb.send('SetActiveChartMetric', {'portal_uuid': portalUuid, 'metric_type': currentMetricType});
}