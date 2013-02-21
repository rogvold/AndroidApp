(function () {
    "use strict";

    var hrmService = null;
    var uiVisible = false;
    var hrmInitialized = false;
    var hrmRequested = false;

    function getDeviceReadingsAsync() {
        try {
            // The way the Async pattern is implemented is that when ReadHeartRateMeasurement
            // function returns, the Wpd Automation platform will invoke the onReadHeartRateMeasurementComplete
            // handler, set to retrievedReading below
            hrmService.ReadHeartRateMeasurement();
        } catch (ex) {
            
        }
    }

    function retrievedReading(result) {
        try {
            var intervals = [];

            for (var i = result.Size; i > 0; i--) {
                intervals.push((result.Intervals[2 * i - 1] & 0xFF) << 8 | result.Intervals[2 * i - 2])
            }

            var measuredValue = {
                // Create a Date object from the numerical timestamp provided to us by the driver
                timestamp: new Date(result.TimeStamp * 1000),
                value: result.Rate,
                intervals: intervals,

                // Override the default toString function
                toString: function () {
                    return this.value + ' bpm @ ' + this.intervals.join() + ' ' +this.timestamp;
                }
            };

            // Dispatch the retrieved measurement to update the application data and the associated view
            MeasurementData.addValue(
                0,
                measuredValue);

            // Query the driver for more data
            getDeviceReadingsAsync();
            var devs = MeasurementData.getDevices();
            document.getElementById("Hrm").textContent = devs[0].data[devs[0].data.length - 1];
        } catch (exception) {
            
        }
    }

    function initializeHeartRateDevicesAsync(id) {

        Windows.UI.WebUI.WebUIApplication.addEventListener('suspending', applicationSuspended);
        Windows.UI.WebUI.WebUIApplication.addEventListener('resuming', applicationActivated);

        // Initialize Heart Rate Devices
        Windows.Devices.Enumeration.DeviceInformation.findAllAsync("System.Devices.InterfaceClassGuid:=\"{0000180D-0000-1000-8000-00805f9b34fb}\"", null).
            done(function (devices) {
                // If devices were found, proceed with initialization
                if (devices.length > 0) {
                    try {
                        // Use WPD Automation to initialize the device objects
                        var deviceFactory = new ActiveXObject("PortableDeviceAutomation.Factory");

                        // For the purpose of this sample we will initialize the first device
                        deviceFactory.getDeviceFromIdAsync(devices[id * 2].id, function (device) {

                            // Initialize the Heart Rate Monitor service
                            hrmService = device.services[0];

                            hrmService.onApplicationActivatedComplete = function () { };

                            hrmService.onApplicationSuspendedComplete = function () { };

                            var devs = MeasurementData.getDevices();
                            var devId = 0;
                            devs[devId] = {
                                devId: devId,
                                name: devices[id * 2].name,
                                description: devices[id * 2].id,
                                data: []
                            };

                            // Rather than setting the handler for the complete method every time
                            // by using the traditional Promise based Async pattern
                            // we use a Wpd Automation feature to set the complete function only once
                            hrmService.onReadHeartRateMeasurementComplete = retrievedReading;
                            hrmInitialized = true;
                            getDeviceReadingsAsync();
                        }, function (errorCode) {
                            
                        });

                    } catch (exception) {
                        
                    }
                } else {
                    
                }
            });
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
    });
})();
