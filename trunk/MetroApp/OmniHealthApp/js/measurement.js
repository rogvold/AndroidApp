﻿(function () {
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
    var sessionTime = 5000;
    var chart;

    function getDeviceReadingsAsync() {
        try {
            // The way the Async pattern is implemented is that when ReadHeartRateMeasurement
            // function returns, the Wpd Automation platform will invoke the onReadHeartRateMeasurementComplete
            // handler, set to retrievedReading below
            hrmService.ReadHeartRateMeasurement();
        } catch (ex) {

        }
    }

    function idToName(id) {
        var pattern = /&([^_&]*)[^&]*&([^_&]*).*d0(\d+)\D/g;
        var match = pattern.exec(id);
        return match[1] + " " + match[2] + " " + match[3];
    }

    function retrievedReading(result) {
        try {
            var intervals = [];

            for (var i = result.Size; i > 0; i--) {
                intervals.push((result.Intervals[2 * i - 1] & 0xFF) << 8 | result.Intervals[2 * i - 2])
            }

            if (result.timestamp * 1000 > startTime && currentTime < endTime) {
                // Dispatch the retrieved measurement to update the application data and the associated view
                for (var key in intervals) {
                    var interval = intervals[key];
                    MeasurementData.addValue({
                        interval: interval,
                        rate: result.Rate,
                        timestamp: currentTime
                    });

                    chart.updateChartData({ timestamps: [currentTime], intervals: [interval] });
                    currentTime += interval;
                }
                // Query the driver for more data
                getDeviceReadingsAsync();
                /*var dataChart = new Chart.renderer();
                dataChart.plot("chartCanvasHRM", MeasurementData.getMeasurements());*/
            }
            else if (result.timestamp * 1000 < startTime) {
                getDeviceReadingsAsync();
            }
            else if (currentTime > endTime) {
                finishSession();
            }

        } catch (exception) {

        }
    }

    function startSession() {
        // Rather than setting the handler for the complete method every time
        // by using the traditional Promise based Async pattern
        // we use a Wpd Automation feature to set the complete function only once
        startTime = new Date().getTime();
        MeasurementData.startTime = startTime;
        endTime = startTime + sessionTime;
        currentTime = startTime;
        getDeviceReadingsAsync();
    }

    function initializeHeartRateDevicesAsync(id) {
        Windows.UI.WebUI.WebUIApplication.addEventListener('suspending', applicationSuspended);
        Windows.UI.WebUI.WebUIApplication.addEventListener('resuming', applicationActivated);
        deviceId = id;
        MeasurementData.deviceId = deviceId;
        MeasurementData.measurements = [];

        // Initialize Heart Rate Devices
        try {
            // Use WPD Automation to initialize the device objects
            var deviceFactory = new ActiveXObject("PortableDeviceAutomation.Factory");

            // For the purpose of this sample we will initialize the first device
            deviceFactory.getDeviceFromIdAsync(id, function (device) {

                // Initialize the Heart Rate Monitor service
                hrmService = device.services[0];

                hrmService.onApplicationActivatedComplete = function () { };

                hrmService.onApplicationSuspendedComplete = function () { };

                /*var dataChart = new Chart.renderer();
                dataChart.plot("chartCanvasHRM", null);*/
                hrmService.onReadHeartRateMeasurementComplete = retrievedReading;
                hrmInitialized = true;
                chart = new Chart.renderer();
                chart.draw('chartCanvasHRM');
                startSession();
            }, function (errorCode) {

            });

        } catch (exception) {

        }
    }

    function finishSession() {
        var flyout = document.getElementById('saveSession').winControl;
        flyout.show(saveSession, "right");
    }

    function applicationActivated() {
        try {
            // Invoking a Wpd Service Method to let the driver know that
            // the application has foreground and can receive notifications
            if (hrmService !== null) {
                hrmService.ApplicationActivated();
            }
        } catch (exception) {

        }
    }

    function applicationSuspended() {
        try {
            // Invoking a Wpd Service Method to let the driver know that
            // the application has been suspended and won't receive notifications
            if (hrmService !== null) {
                hrmService.ApplicationSuspended();
            }
        } catch (exception) {

        }
    }

    WinJS.Namespace.define("HeartRateMeasurement",
    {
        initializeHeartRateDevicesAsync: initializeHeartRateDevicesAsync,
        hrmInitialized: hrmInitialized,
        startSession: startSession,
        user: user,
        idToName: idToName,
    });
})();
