$('#overview_link').attr('href', "overview.html" + mainGetQuery);
$('#sidebar_overview_link').attr('href', "overview.html" + mainGetQuery);

$('#traces_link_latest').attr('href', "traces.html" + mainGetQuery + '&order_type=latest_traces');
$('#traces_link_slowest').attr('href', "traces.html" + mainGetQuery + '&order_type=slowest_traces');
$('#sidebar_traces_link_latest').attr('href', "traces.html" + mainGetQuery + '&order_type=latest_traces');
$('#sidebar_traces_link_slowest').attr('href', "traces.html" + mainGetQuery + '&order_type=slowest_traces');

$('#configuration_link').attr('href', "configuration.html" + mainGetQuery);
$('#sidebar_configuration_link').attr('href', "configuration.html" + mainGetQuery);

var series0 = {
    name: '99th percentile',
    type: 'line',
    color: "#e1483b",
    hoverAnimation: false,
    symbol: 'circle',
    symbolSize: 8,
    showSymbol: true,
    areaStyle: {},
    data: []
};

var series1 = {
    name: '95th percentile',
    type: 'line',
    color: "#e1483b",
    showSymbol: false,
    hoverAnimation: false,
    areaStyle: {},
    data: []
};

var series2 = {
    name: '90th percentile',
    type: 'line',
    color: "#e1483b",
    showSymbol: false,
    hoverAnimation: false,
    areaStyle: {},
    data: []
};

var series3 = {
    name: '75th percentile',
    type: 'line',
    color: "#e1483b",
    showSymbol: false,
    hoverAnimation: false,
    areaStyle: {},
    data: []
};

var series4 = {
    name: '50th percentile',
    type: 'line',
    color: "#e1483b",
    showSymbol: false,
    hoverAnimation: false,
    areaStyle: {},
    data: []
};

var overviewChart = echarts.init(document.getElementById('overview_chart'));
window.onresize = function () {
    overviewChart.resize();
};

var labelColor = (darkMode) ? "grey" : "black";
var overviewChartOptions = {
    grid: {
        top: 20,
        bottom: 30,
        left: 55,
        right: 0,
    },
    tooltip: {
        trigger: 'axis',
        formatter: function (params) {
            params = params[0];
            return moment(params.value[0]).format('LTS') + ' : ' + params.value[1] + "ms";
        },
        axisPointer: {
            animation: false
        }
    },
    xAxis: {
        type: 'time',
        splitLine: {
            show: true
        },
        axisLabel: {
            formatter: function (value) {
                return moment(value).format('LT');
            },
            color: labelColor
        }
    },
    yAxis: {
        type: 'value',
        boundaryGap: [0, '100%'],
        splitLine: {
            show: false
        },
        axisLabel: {
            color: labelColor
        }
    },
    series: [series0, series1, series2, series3, series4]
};
overviewChart.setOption(overviewChartOptions);

var currentMetricType = "Throughput_Average";
var currentTimeFrame = "LAST_15_MINUTES";
eb.onopen = function () {
    portalConnected();
    if (requiresRegistration) {
        return;
    }
    clickedViewAverageResponseTimeChart(); //default = avg resp time

    var clearOverviewHandler = portalUuid + '-ClearOverview';
    var displayCardHandler = portalUuid + '-DisplayCard';
    var updateChartHandler = portalUuid + '-UpdateChart';
    var displayStatsHandler = portalUuid + '-DisplayStats';
    eb.registerHandler(clearOverviewHandler, function (error, message) {
        console.log("Clearing overview");

        overviewChart.setOption({
            series: []
        })
    });
    eb.registerHandler(displayCardHandler, function (error, message) {
        //eb.send('PortalLogger', 'Displaying card: ' + JSON.stringify(message));
        // console.log('Displaying card: ' + JSON.stringify(message));
        var card = message.body;
        if (card.time_frame != currentTimeFrame) {
            // console.log("Ignoring card for time frame: " + card.time_frame
            //     + " - Current time frame: " + currentTimeFrame);
            return
        }

        document.getElementById('card_' + card.meta.toLowerCase() + '_header').textContent = card.header;
    });
    eb.registerHandler(updateChartHandler, function (error, message) {
        //eb.send('PortalLogger', 'Updating chart: ' + JSON.stringify(message));
        // console.log('Updating chart: ' + JSON.stringify(message));
        var chartData = message.body;
        if (chartData.time_frame != currentTimeFrame) {
            return
        }
        var cards = ["throughput_average", "responsetime_average", "servicelevelagreement_average"];
        for (var i = 0; i < cards.length; i++) {
            $('#card_' + cards[i] + '_header').removeClass('spp_red_color');
            $('#card_' + cards[i] + '_header_label').removeClass('spp_red_color');
        }
        $('#card_' + chartData.metric_type.toLowerCase() + '_header').addClass('spp_red_color');
        $('#card_' + chartData.metric_type.toLowerCase() + '_header_label').addClass('spp_red_color');

        for (var i = 0; i < chartData.series_data.length; i++) {
            var seriesData = chartData.series_data[i];
            var list = [];
            for (var z = 0; z < seriesData.values.length; z++) {
                var value = seriesData.values[z];
                var time = moment.unix(seriesData.times[z]).valueOf();

                list.push({
                    value: [time, value],
                    itemStyle: {
                        normal: {
                            color: '#182d34',
                        }
                    }
                });
            }
        }
        if (seriesData.series_index == 0) {
            series0.data = list;
        } else if (seriesData.series_index == 1) {
            series1.data = list;
        } else if (seriesData.series_index == 2) {
            series2.data = list;
        } else if (seriesData.series_index == 3) {
            series3.data = list;
        } else if (seriesData.series_index == 4) {
            series4.data = list;
        }
        overviewChart.setOption({
            series: [series0, series1, series2, series3, series4]
        })
    });
    eb.registerHandler(displayStatsHandler, function (error, message) {
        //eb.send('PortalLogger', 'Displaying stats: ' + JSON.stringify(message));
        // console.log('Displaying stats: ' + JSON.stringify(message));
        var stats = message.body;
        if (stats.time_frame != currentTimeFrame) {
            // console.log("Ignoring stats for time frame: " + stats.time_frame
            //     + " - Current time frame: " + currentTimeFrame);
            return
        }
    });

    var timeFrame = localStorage.getItem('spp.metric_time_frame');
    if (timeFrame !== null) {
        updateTime(timeFrame);
        console.log('Set initial time frame to: ' + timeFrame);
        eb.send('PortalLogger', 'Set initial time frame to: ' + timeFrame);
    }

    eb.publish('OverviewTabOpened', {'portal_uuid': portalUuid});
};

function updateTime(interval) {
    currentTimeFrame = interval.toUpperCase();
    localStorage.setItem('spp.metric_time_frame', interval);
    eb.send('SetMetricTimeFrame', {'portal_uuid': portalUuid, 'metric_time_frame': interval});
}

function clickedViewAverageThroughputChart() {
    currentMetricType = "Throughput_Average";
    eb.send('SetActiveChartMetric', {'portal_uuid': portalUuid, 'metric_type': currentMetricType});
}

function clickedViewAverageResponseTimeChart() {
    currentMetricType = "ResponseTime_Average";
    eb.send('SetActiveChartMetric', {'portal_uuid': portalUuid, 'metric_type': currentMetricType});
}

function clickedViewAverageSLAChart() {
    currentMetricType = "ServiceLevelAgreement_Average";
    eb.send('SetActiveChartMetric', {'portal_uuid': portalUuid, 'metric_type': currentMetricType});
}