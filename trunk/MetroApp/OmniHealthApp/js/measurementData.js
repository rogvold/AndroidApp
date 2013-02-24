(function () {
    'use strict';

    // The data format for the devices array will be : {name: , description: , datapoints: [] }
    var devices = [];
    var indexes = [];

    function getDevices() {
        return devices;
    }

    function getIndexes() {
        return indexes;
    }

    function addValue(deviceIndex, newValue) {
        devices[deviceIndex].data[devices[deviceIndex].data.length] = newValue;
    }

    WinJS.Namespace.define('MeasurementData', {
        getDevices: getDevices,
        addValue: addValue,
        getIndexes: getIndexes
    });
})();