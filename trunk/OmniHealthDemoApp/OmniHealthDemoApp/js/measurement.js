(function () {
    "use strict";

    var hrmService = null;
    var uiVisible = false;
    var hrmInitialized = false;
    var hrmRequested = false;
    var startTime = null;
    var currentTime = null;
    var endTime = null;
    var user = null;
    var deviceId = null;
    var sessionTime = 120000;
    var chart;
    var timer;

    function retrievedReading(result) {
        if (result.timestamp * 1000 > startTime && currentTime < endTime) {
            MeasurementData.addValue({
                interval: result.interval,
                rate: result.rate,
                timestamp: currentTime
            });

            chart.updateChartData({ timestamps: [currentTime], intervals: [result.interval] });
            currentTime += result.interval;
        }
        else if (currentTime > endTime) {
            finishSession();

        }
    }

    function startSession() {
        startTime = new Date().getTime();
        MeasurementData.startTime = startTime;
        endTime = startTime + sessionTime;
        currentTime = startTime;
        timer = setInterval(function () {
            var timestamp = new Date().getTime();
            var interval = Math.round(Math.random() * (1000 - 700) + 700);
            var rate = Math.round(60000 / interval);
            retrievedReading({ timestamp: timestamp, interval: interval, rate: rate });
        }, 1000);
    }

    function initializeHeartRateDevicesAsync(id) {
        deviceId = id;
        MeasurementData.deviceId = deviceId;
        MeasurementData.measurements = [];
        chart = new Chart.renderer();
        chart.draw("chartCanvasHRM");
        startSession();
    }

    function finishSession() {
        clearInterval(timer);
        var flyout = document.getElementById('saveSession').winControl;
        flyout.show(saveSession, "right");
    }

    WinJS.Namespace.define("HeartRateMeasurement",
    {
        initializeHeartRateDevicesAsync: initializeHeartRateDevicesAsync,
        hrmInitialized: hrmInitialized,
        startSession: startSession,
        user: user,
    });
})();
