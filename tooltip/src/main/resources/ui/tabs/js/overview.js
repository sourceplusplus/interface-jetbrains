if (!pluginAvailable) {
    $('#traces_link_latest').attr('href', "traces.html" + window.location.search);
    $('#traces_link_slowest').attr('href', "traces.html" + window.location.search);
    $('#sidebar_traces_link_latest').attr('href', "traces.html" + window.location.search);
    $('#sidebar_traces_link_slowest').attr('href', "traces.html" + window.location.search);
}

//am4core.useTheme(am4themes_animated);

// Create chart instance
var chart = am4core.create("chartdiv", am4charts.XYChart);
//chart.padding(0, 0, 0, 0);
chart.hiddenState.properties.opacity = 0;
chart.zoomOutButton.disabled = true;

// Increase contrast by taking evey second color
chart.colors.step = 2;

// Create axes
var dateAxis = chart.xAxes.push(new am4charts.DateAxis());
dateAxis.renderer.minGridDistance = 50;
dateAxis.renderer.inside = true;
dateAxis.renderer.axisFills.template.disabled = true;
dateAxis.renderer.ticks.template.disabled = true;
dateAxis.cursorTooltipEnabled = false;

var valueAxis = chart.yAxes.push(new am4charts.ValueAxis());

// Create series
function createAxisAndSeries(series, field, name, opposite, bullet) {
    series.dataFields.valueY = field;
    series.dataFields.dateX = "date";
    series.strokeWidth = 2;
    series.yAxis = valueAxis;
    series.name = name;
    series.fill = "#e1483b";

    //series.tooltipText = "{valueY}[/]";
    series.tooltip.background.cornerRadius = 20;
    series.tooltip.background.strokeOpacity = 0;
    series.tooltip.pointerOrientation = "vertical";
    series.tooltip.label.minWidth = 40;
    series.tooltip.label.minHeight = 40;
    series.tooltip.label.textAlign = "middle";
    series.tooltip.label.textValign = "middle";
    series.tensionX = 0.8;

    series.fillOpacity = 1;
    var gradient = new am4core.LinearGradient();
    gradient.addColor(chart.colors.getIndex(0), 0.2);
    gradient.addColor(chart.colors.getIndex(0), 0);
    //series.fill = am4core.color("#e1483b");
    series.stroke = am4core.color("#182d34");

    var interfaceColors = new am4core.InterfaceColorSet();

    // var bullet = series.bullets.push(new am4charts.CircleBullet());
    // bullet.circle.strokeWidth = 2;
    // bullet.circle.radius = 4;
    // bullet.circle.fill = am4core.color("#182d34");
    //
    // var bullethover = bullet.states.create("hover");
    // bullethover.properties.scale = 1.3;

    valueAxis.renderer.line.strokeOpacity = 1;
    valueAxis.renderer.line.strokeWidth = 2;
    valueAxis.renderer.line.stroke = series.stroke;
    valueAxis.renderer.labels.template.fill = series.stroke;
    valueAxis.renderer.opposite = opposite;
    valueAxis.renderer.grid.template.disabled = true;
    valueAxis.cursorTooltipEnabled = true;

    // bullet at the front of the line
    var bullet = series.createChild(am4charts.CircleBullet);
    bullet.circle.radius = 3;
    bullet.fillOpacity = 1;
    bullet.fill = am4core.color("#182d34");
    bullet.isMeasured = false;

    series.events.on("validated", function () {
        if (series.dataItems.last) {
            bullet.moveTo(series.dataItems.last.point);
            bullet.validatePosition();
        }
    });
}

var series0 = chart.series.push(new am4charts.LineSeries());
createAxisAndSeries(series0, "0", "Visits", false, "circle");

var series1 = chart.series.push(new am4charts.LineSeries());
createAxisAndSeries(series1, "1", "Hits", false, "rectangle");

var series2 = chart.series.push(new am4charts.LineSeries());
createAxisAndSeries(series2, "2", "Hits", false, "rectangle");

var series3 = chart.series.push(new am4charts.LineSeries());
createAxisAndSeries(series3, "3", "Hits", false, "rectangle");

var series4 = chart.series.push(new am4charts.LineSeries());
createAxisAndSeries(series4, "4", "Hits", false, "rectangle");


// Add legend
//chart.legend = new am4charts.Legend();

// Add cursor
chart.cursor = new am4charts.XYCursor();
chart.cursor.behavior = "none";
// valueAxis.renderer.ticks.template.disabled = true;
//
// var series = chart.series.push(new am4charts.LineSeries());
// series.dataFields.dateX = "date";
// series.dataFields.valueY = "value";
// series.interpolationDuration = 500;
// series.defaultState.transitionDuration = 0;
// series.tensionX = 0.8;
//
// chart.events.on("datavalidated", function () {
//     dateAxis.zoom({start: 1 / 15, end: 1.2}, false, true);
// });
//
// dateAxis.interpolationDuration = 500;
// dateAxis.rangeChangeDuration = 500;
//
// document.addEventListener("visibilitychange", function () {
//     if (document.hidden) {
//         // if (interval) {
//         //    // clearInterval(interval);
//         // }
//     }
//     else {
//         //startInterval();
//     }
// }, false);
//
// // // add data
// // var interval;
// //
// // function startInterval() {
// //     interval = setInterval(function () {
// //         visits =
// //             visits + Math.round((Math.random() < 0.5 ? 1 : -1) * Math.random() * 5);
// //         var lastdataItem = series.dataItems.getIndex(series.dataItems.length - 1);
// //         if (lastdataItem) {
// //             chart.addData(
// //                 {date: new Date(lastdataItem.dateX.getTime() + 1000), value: visits},
// //                 1
// //             );
// //         } else {
// //             chart.addData(
// //                 {date: new Date(), value: visits},
// //                 1
// //             );
// //         }
// //     }, 1000);
// // }
// //
// // startInterval();
//
// // all the below is optional, makes some fancy effects
// // gradient fill of the series
// series.fillOpacity = 1;
// var gradient = new am4core.LinearGradient();
// gradient.addColor(chart.colors.getIndex(0), 0.2);
// gradient.addColor(chart.colors.getIndex(0), 0);
// series.fill = am4core.color("#e1483b");
// series.stroke = am4core.color("#182d34");
//
// // this makes date axis labels to fade out
// // dateAxis.renderer.labels.template.adapter.add("fillOpacity", function (fillOpacity, target) {
// //     var dataItem = target.dataItem;
// //     return dataItem.position;
// // })
//
// // need to set this, otherwise fillOpacity is not changed and not set
// // dateAxis.events.on("validated", function () {
// //     am4core.iter.each(dateAxis.renderer.labels.iterator(), function (label) {
// //         label.fillOpacity = label.fillOpacity;
// //     })
// // })
//
// // this makes date axis labels which are at equal minutes to be rotated
// dateAxis.renderer.labels.template.adapter.add("rotation", function (rotation, target) {
//     var dataItem = target.dataItem;
//     if (dataItem.date && dataItem.date.getTime() == am4core.time.round(new Date(dataItem.date.getTime()), "minute").getTime()) {
//         target.verticalCenter = "middle";
//         target.horizontalCenter = "left";
//         return -90;
//     }
//     else {
//         target.verticalCenter = "bottom";
//         target.horizontalCenter = "middle";
//         return 0;
//     }
// })
//


var currentTimeFrame = "LAST_15_MINUTES";
eb.onopen = function () {
    var clearOverviewHandler = 'ClearOverview';
    var displayCardHandler = 'DisplayCard';
    var updateChartHandler = 'UpdateChart';
    var displayStatsHandler = 'DisplayStats';
    if (!pluginAvailable) {
        clearOverviewHandler = appUuid + "-" + subscribedArtifactQualifiedName + "-" + clearOverviewHandler;
        displayCardHandler = appUuid + "-" + subscribedArtifactQualifiedName + "-" + displayCardHandler;
        updateChartHandler = appUuid + "-" + subscribedArtifactQualifiedName + "-" + updateChartHandler;
        displayStatsHandler = appUuid + "-" + subscribedArtifactQualifiedName + "-" + displayStatsHandler;
    }
    eb.registerHandler(clearOverviewHandler, function (error, message) {
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

        for (var i = 0; i < chartUi.series.length; i++) {
            var series = chartUi.series[i];
            while (series.data.length > 0) {
                series.data[0].remove(true);
            }
        }
        chartUi.redraw();
    });
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
                    date: time,
                    [seriesData.series_index]: value
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
        chart.validateData();
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