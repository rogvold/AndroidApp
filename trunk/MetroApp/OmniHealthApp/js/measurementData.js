(function () {
    'use strict';

    // The data format for the devices array will be : {name: , description: , datapoints: [] }
    var devices = [];
    var session = null;

    function getDevices() {
        return devices;
    }

    function getSession() {
        return session;
    }

    function addValue(deviceIndex, newValue) {
        devices[deviceIndex].data[devices[deviceIndex].data.length] = newValue;
    }

    WinJS.Namespace.define('MeasurementData', {
        getDevices: getDevices,
        addValue: addValue,
        getSession: getSession
    });
})();