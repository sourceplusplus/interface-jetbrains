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
let regressionSeries = {
    name: 'Predicted Regression',
    type: 'line',
    color: "#000",
    hoverAnimation: false,
    symbol: 'square',
    symbolSize: 8,
    showSymbol: true,
    areaStyle: {},
    data: []
};

var overviewChart = null;
function loadChart() {
    overviewChart = echarts.init(document.getElementById('overview_chart'));
    window.onresize = function () {
        console.log("Resizing overview chart");
        overviewChart.resize();
    };
    overviewChart.setOption(overviewChartOptions);
}

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
    series: [series0, series1, series2, series3, series4, regressionSeries]
};

function clearActivity() {
    console.log("Clearing activity");

    series0.data = [];
    series1.data = [];
    series2.data = [];
    series3.data = [];
    series4.data = [];
    regressionSeries.data = [];
    overviewChart.setOption({
        series: []
    });
    overviewChart.resize();
}

function displayCard(card) { //todo-chess-equality: [card: BarTrendCard]
    console.log('Displaying card');

    document.getElementById('card_' + card.meta.toLowerCase() + '_header').textContent = card.header;
}

function updateChart(chartData) { //todo-chess-equality: [chartData: SplintChart]
    console.log('Updating chart');

    let cards = ["throughput_average", "responsetime_average", "servicelevelagreement_average"];
    for (let i = 0; i < cards.length; i++) {
        $('#card_' + cards[i] + '_header').removeClass('spp_red_color');
        $('#card_' + cards[i] + '_header_label').removeClass('spp_red_color');
    }
    $('#card_' + chartData.metricType.toLowerCase() + '_header').addClass('spp_red_color');
    $('#card_' + chartData.metricType.toLowerCase() + '_header_label').addClass('spp_red_color');
    if (chartData.metricType.toLowerCase() === cards[0]) {
        tooltipMeasurement = "/min";
    } else if (chartData.metricType.toLowerCase() === cards[1]) {
        tooltipMeasurement = "ms";
    } else if (chartData.metricType.toLowerCase() === cards[2]) {
        tooltipMeasurement = "%";
    }

    for (let i = 0; i < chartData.seriesData.length; i++) {
        let seriesData = chartData.seriesData[i];
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

        if (seriesData.seriesIndex === 0) {
            series0.data = list;
        } else if (seriesData.seriesIndex === 1) {
            series1.data = list;
        } else if (seriesData.seriesIndex === 2) {
            series2.data = list;
        } else if (seriesData.seriesIndex === 3) {
            series3.data = list;
        } else if (seriesData.seriesIndex === 4) {
            series4.data = list;
        } else if (seriesData.series_index === 5) {
            regressionSeries.data = list;
        }
    }
    overviewChart.setOption({
        series: [series0, series1, series2, series3, series4, regressionSeries]
    })
}