(function () {
    'use strict';

    var measurements = { intervals: [], rates: [], timestamps: [] };
    var session = null;
    var startTime;
    var deviceId;

    function addValue(newValue) {
        measurements.intervals[measurements.intervals.length] = newValue.interval;
        measurements.rates[measurements.rates.length] = newValue.rate;
        measurements.timestamps[measurements.timestamps.length] = newValue.timestamp;
    }

    function getMeasurements() {
        return measurements;
    }

    WinJS.Namespace.define('MeasurementData', {
        getMeasurements: getMeasurements,
        addValue: addValue,
        session: session,
        startTime: startTime,
        deviceId: deviceId,
    });
})();