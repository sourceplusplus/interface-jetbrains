if (!pluginAvailable) {
    $('#traces_link').attr('href', "traces.html" + window.location.search);
}

Highcharts.setOptions({
    global: {
        useUTC: false
    }
});
var chartUi = Highcharts.chart('container', {
    chart: {
        type: 'spline',
        animation: false
    },
    credits: {
        enabled: false
    },
    title: {
        text: null
    },
    xAxis: {
        type: 'datetime',
        tickPixelInterval: 150
    },
    yAxis: {
        title: {
            text: null
        },
        plotLines: [{
            value: 0,
            width: 1,
            color: '#808080'
        }]
    },
    tooltip: {
        headerFormat: '<b>{series.name}</b><br/>',
        pointFormat: '{point.x:%Y-%m-%d %H:%M:%S}<br/>{point.y:.2f}'
    },
    legend: {
        enabled: false
    },
    exporting: {
        enabled: false
    },
    series: [{
        color: '#e1483b',
        name: 'Response time (p99)',
        data: []
    }, {
        //color: '#e1483b',
        name: 'Response time (p95)',
        data: []
    }, {
        //color: '#e1483b',
        name: 'Response time (p90)',
        data: []
    }, {
        //color: '#e1483b',
        name: 'Response time (p75)',
        data: []
    }, {
        //color: '#e1483b',
        name: 'Response time (p50)',
        data: []
    }]
});
chartUi.setSize(450, 125); //todo: not hardcode this

var currentTimeFrame = "LAST_15_MINUTES";
eb.onopen = function () {
    var displayCardHandler = 'DisplayCard';
    var updateChartHandler = 'UpdateChart';
    var displayStatsHandler = 'DisplayStats';
    if (!pluginAvailable) {
        displayCardHandler = appUuid + "-" + subscribedArtifactQualifiedName + "-" + displayCardHandler;
        updateChartHandler = appUuid + "-" + subscribedArtifactQualifiedName + "-" + updateChartHandler;
        displayStatsHandler = appUuid + "-" + subscribedArtifactQualifiedName + "-" + displayStatsHandler;
    }
    eb.registerHandler(displayCardHandler, function (error, message) {
        //eb.send('TooltipLogger', 'Displaying card: ' + JSON.stringify(message));
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
        //eb.send('TooltipLogger', 'Updating chart: ' + JSON.stringify(message));
        // console.log('Updating chart: ' + JSON.stringify(message));
        var chart = message.body;
        if (chart.time_frame != currentTimeFrame) {
            // console.log("Ignoring chart for time frame: " + chart.time_frame
            //     + " - Current time frame: " + currentTimeFrame);
            return
        }

        for (var i = 0; i < chart.series_data.length; i++) {
            var seriesData = chart.series_data[i];
            var series = chartUi.series[seriesData.series_index];

            var list = [];
            for (var z = 0; z < seriesData.values.length; z++) {
                var value = seriesData.values[z];
                var time = moment.unix(seriesData.times[z]).valueOf();
                list.push([time, value])
            }
            series.setData(list);
        }
        chartUi.redraw();
    });
    eb.registerHandler(displayStatsHandler, function (error, message) {
        //eb.send('TooltipLogger', 'Displaying stats: ' + JSON.stringify(message));
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
        eb.send('TooltipLogger', 'Set initial time frame to: ' + timeFrame);
    }

    eb.send('OverviewTabOpened', {});
};

$('#dropdown').dropdown();

$('#speed_stats').show();
$('#frequency_stats').hide();

$('#speed_tab').click(function () {
    $('#speed_stats').show();
    $('#frequency_stats').hide();
    $('#frequency_tab').removeClass('active');
    $('#speed_tab').addClass('active');
});

$('#frequency_tab').click(function () {
    $('#frequency_stats').show();
    $('#speed_stats').hide();
    $('#speed_tab').removeClass('active');
    $('#frequency_tab').addClass('active');
});

function updateTime(interval) {
    currentTimeFrame = interval.toUpperCase();
    localStorage.setItem('spp.metric_time_frame', interval);
    eb.send('SetMetricTimeFrame', {value: interval});

    if (interval === 'last_15_minutes') {
        $('#current_metric_time_frame').text('LAST 15 MINUTES');
    } else if (interval === 'last_30_minutes') {
        $('#current_metric_time_frame').text('LAST 30 MINUTES');
    } else if (interval === 'last_hour') {
        $('#current_metric_time_frame').text('LAST HOUR');
    }
}