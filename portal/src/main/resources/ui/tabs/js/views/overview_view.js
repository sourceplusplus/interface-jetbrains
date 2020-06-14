let series0 = {
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
let series1 = {
    name: '95th percentile',
    type: 'line',
    color: "#e1483b",
    showSymbol: false,
    hoverAnimation: false,
    areaStyle: {},
    data: []
};
let series2 = {
    name: '90th percentile',
    type: 'line',
    color: "#e1483b",
    showSymbol: false,
    hoverAnimation: false,
    areaStyle: {},
    data: []
};
let series3 = {
    name: '75th percentile',
    type: 'line',
    color: "#e1483b",
    showSymbol: false,
    hoverAnimation: false,
    areaStyle: {},
    data: []
};
let series4 = {
    name: '50th percentile',
    type: 'line',
    color: "#e1483b",
    showSymbol: false,
    hoverAnimation: false,
    areaStyle: {},
    data: []
};

let overviewChart = echarts.init(document.getElementById('overview_chart'));
window.onresize = function () {
    console.log("Resizing overview chart");
    overviewChart.resize();
};

var tooltipMeasurement = "ms";
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
            return moment(params.value[0]).format('LTS') + ' : ' + params.value[1] + tooltipMeasurement;
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

function clearOverview() {
    console.log("Clearing overview");

    overviewChart.setOption({
        series: []
    })
}

function displayCard(card) {
    console.log('Displaying card');

    document.getElementById('card_' + card.meta.toLowerCase() + '_header').textContent = card.header;
}

function updateChart(chartData) {
    console.log('Updating chart');

    let cards = ["throughput_average", "responsetime_average", "servicelevelagreement_average"];
    for (let i = 0; i < cards.length; i++) {
        $('#card_' + cards[i] + '_header').removeClass('spp_red_color');
        $('#card_' + cards[i] + '_header_label').removeClass('spp_red_color');
    }
    $('#card_' + chartData.metric_type.toLowerCase() + '_header').addClass('spp_red_color');
    $('#card_' + chartData.metric_type.toLowerCase() + '_header_label').addClass('spp_red_color');
    if (chartData.metric_type.toLowerCase() === cards[0]) {
        tooltipMeasurement = "/min";
    } else if (chartData.metric_type.toLowerCase() === cards[1]) {
        tooltipMeasurement = "ms";
    } else if (chartData.metric_type.toLowerCase() === cards[2]) {
        tooltipMeasurement = "%";
    }

    for (let i = 0; i < chartData.series_data.length; i++) {
        let seriesData = chartData.series_data[i];
        let list = [];
        for (let z = 0; z < seriesData.values.length; z++) {
            let value = seriesData.values[z];
            let time = moment.unix(seriesData.times[z]).valueOf();

            list.push({
                value: [time, value],
                itemStyle: {
                    normal: {
                        color: '#182d34',
                    }
                }
            });
        }

        if (seriesData.series_index === 0) {
            series0.data = list;
        } else if (seriesData.series_index === 1) {
            series1.data = list;
        } else if (seriesData.series_index === 2) {
            series2.data = list;
        } else if (seriesData.series_index === 3) {
            series3.data = list;
        } else if (seriesData.series_index === 4) {
            series4.data = list;
        }
    }
    overviewChart.setOption({
        series: [series0, series1, series2, series3, series4]
    })
}