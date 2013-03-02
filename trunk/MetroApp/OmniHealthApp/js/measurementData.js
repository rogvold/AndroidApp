(function () {
    'use strict';

    var measurements = [];
    var session = null;
    var startTime;
    var deviceId;

    function addValue(newValue) {
        measurements[measurements.length] = newValue;
    }

    WinJS.Namespace.define('MeasurementData', {
        measurements: measurements,
        addValue: addValue,
        session: session,
        startTime: startTime,
        deviceId: deviceId,
    });
})();