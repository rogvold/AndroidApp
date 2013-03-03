(function () {
    'use strict';

    var measurements = [];
    var session = null;
    var startTime;
    var deviceId;

    function addValue(newValue) {
        measurements[measurements.length] = newValue;
    }

    function getMeasurements() {
        return measurements;
    }

    WinJS.Namespace.define('MeasurementData', {
        measurements: measurements,
        getMeasurements: getMeasurements,
        addValue: addValue,
        session: session,
        startTime: startTime,
        deviceId: deviceId,
    });
})();