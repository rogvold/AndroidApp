(function () {
    'use strict';

    var renderer = WinJS.Class.define(function () { }, {
        chart: undefined,
        chartData: [],

        // generate some random data, quite different range
        updateChartData: function (data) {
            var timestamps;
            if (data.timestamps) {
                timestamps = data.timestamps;
            } else {
                timestamps = [];
                var startTimestamp = data.startTimestamp;
                for (var j = 0; j < data.intervals.length; j++) {
                    timestamps.push(startTimestamp);
                    startTimestamp += data.intervals[j];
                }
            }
            for (var i = 0; i < timestamps.length; i++) {
                this.chartData.push({
                    date: timestamps[i],
                    visits: data.intervals[i]
                });
            }
            this.chart.validateData();
        },

        draw: function (container) {
            // SERIAL CHART
            if (this.chartData.length > 0) {
                this.chartData = [];
            }
            this.chart = new AmCharts.AmSerialChart();
            this.chart.fontFamily = "Segoe UI";
            this.chart.pathToImages = "/amcharts/amcharts/images/";
            this.chart.autoMarginOffset = 3;
            this.chart.marginRight = 15;
            this.chart.zoomOutButton = {
                backgroundColor: '#000000',
                backgroundAlpha: 0.15
            };
            this.chart.dataProvider = this.chartData;
            this.chart.categoryField = "date";

            // data updated event will be fired when chart is displayed,
            // also when data will be updated. We'll use it to set some
            // initial zoom
            this.chart.addListener("dataUpdated", this.zoomChart);

            // AXES
            // Category
            var categoryAxis = this.chart.categoryAxis;
            categoryAxis.parseDates = true; // in order char to understand dates, we should set parseDates to true
            categoryAxis.minPeriod = "fff"; // as we have data with minute interval, we have to set "mm" here.             
            categoryAxis.gridAlpha = 0.07;
            categoryAxis.showLastLabel = false;
            categoryAxis.axisColor = "#DADADA";

            // Value
            var valueAxis = new AmCharts.ValueAxis();
            valueAxis.gridAlpha = 0.07;
            this.chart.addValueAxis(valueAxis);

            // GRAPH
            var graph = new AmCharts.AmGraph();
            graph.type = "line"; // try to change it to "column"
            graph.title = "red line";
            graph.valueField = "visits";
            graph.lineAlpha = 1;
            graph.lineColor = "#000000";
            graph.fillAlphas = 0.4; // setting fillAlphas to > 0 value makes it area graph
            this.chart.addGraph(graph);

            // CURSOR
            var chartCursor = new AmCharts.ChartCursor();
            chartCursor.cursorPosition = "mouse";
            chartCursor.categoryBalloonDateFormat = "JJ:NN:SS, DD MMMM";
            this.chart.addChartCursor(chartCursor);

            // SCROLLBAR
            var chartScrollbar = new AmCharts.ChartScrollbar();

            this.chart.addChartScrollbar(chartScrollbar);

            // WRITE
            this.chart.write(container);
        },


        // this method is called when chart is inited as we listen for "dataUpdated" event


        zoomChart: function (args) {
            // different zoom methods can be used - zoomToIndexes, zoomToDates, zoomToCategoryValues
            args.chart.zoomToIndexes(0, args.chart.chartData.length - 1);
        }
    });

    WinJS.Namespace.define('Chart', {
        renderer: renderer
    });
})();
