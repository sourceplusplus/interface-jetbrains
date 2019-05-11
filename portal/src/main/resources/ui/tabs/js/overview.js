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
            }
        }
    },
    yAxis: {
        type: 'value',
        boundaryGap: [0, '100%'],
        splitLine: {
            show: false
        }
    },
    series: [series0, series1, series2, series3, series4]
};
overviewChart.setOption(overviewChartOptions);

var currentTimeFrame = "LAST_15_MINUTES";
eb.onopen = function () {
    portalConnected();
    if (requiresRegistration) {
        return;
    }

    var clearOverviewHandler = portalUuid + '-ClearOverview';
    var displayCardHandler = portalUuid + '-DisplayCard';
    var updateChartHandler = portalUuid + '-UpdateChart';
    var displayStatsHandler = portalUuid + '-DisplayStats';
    eb.registerHandler(clearOverviewHandler, function (error, message) {
        console.log("Clearing overview");
        $('#quick_stats_min').text("n/a");
        $('#quick_stats_max').text("n/a");
        $('#quick_stats_p99').text("n/a");
        $('#quick_stats_p95').text("n/a");
        $('#quick_stats_p90').text("n/a");
        $('#quick_stats_p75').text("n/a");
        $('#quick_stats_p50').text("n/a");

        var cards = ["card_throughput_average", "card_responsetime_average", "card_servicelevelagreement_average"];
        for (var i = 0; i < cards.length; i++) {
            var name = cards[i];
            document.getElementById(name + '_header').textContent = "n/a";
            for (var z = 1; z <= 15; z++) {
                $('#' + name + '_bar_' + z).css('height', '0%');
            }
        }

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

        for (var i = 0; i < 15; i++) {
            $('#card_' + card.meta.toLowerCase() + '_bar_' + (i + 1)).css('height', card.bar_graph_data[i] + '%');
        }
    });
    eb.registerHandler(updateChartHandler, function (error, message) {
        //eb.send('PortalLogger', 'Updating chart: ' + JSON.stringify(message));
        // console.log('Updating chart: ' + JSON.stringify(message));
        var chartData = message.body;
        if (chartData.time_frame != currentTimeFrame) {
            return
        }

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

        if (stats.min) $('#quick_stats_min').text(stats.min);
        if (stats.max) $('#quick_stats_max').text(stats.max);
        if (stats.p99) $('#quick_stats_p99').text(stats.p99);
        if (stats.p95) $('#quick_stats_p95').text(stats.p95);
        if (stats.p90) $('#quick_stats_p90').text(stats.p90);
        if (stats.p75) $('#quick_stats_p75').text(stats.p75);
        if (stats.p50) $('#quick_stats_p50').text(stats.p50);
    });

    var timeFrame = localStorage.getItem('spp.metric_time_frame');
    if (timeFrame !== null) {
        updateTime(timeFrame);
        console.log('Set initial time frame to: ' + timeFrame);
        eb.send('PortalLogger', 'Set initial time frame to: ' + timeFrame);
    }

    eb.send('OverviewTabOpened', {'portal_uuid': portalUuid});
};

function updateTime(interval) {
    currentTimeFrame = interval.toUpperCase();
    localStorage.setItem('spp.metric_time_frame', interval);
    eb.send('SetMetricTimeFrame', {'portal_uuid': portalUuid, 'metric_time_frame': interval});

    if (interval === 'last_5_minutes') {
        $('#current_metric_time_frame').text('LAST 5 MINUTES');
    } else if (interval === 'last_15_minutes') {
        $('#current_metric_time_frame').text('LAST 15 MINUTES');
    } else if (interval === 'last_30_minutes') {
        $('#current_metric_time_frame').text('LAST 30 MINUTES');
    } else if (interval === 'last_hour') {
        $('#current_metric_time_frame').text('LAST HOUR');
    } else if (interval === 'last_3_hours') {
        $('#current_metric_time_frame').text('LAST 3 HOURS');
    }
}