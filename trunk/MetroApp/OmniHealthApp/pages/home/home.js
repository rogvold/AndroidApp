(function () {
    "use strict";
    
    WinJS.UI.Pages.define("/pages/home/home.html", {
        // Эта функция вызывается каждый раз, когда пользователь переходит на данную страницу. Она
        // заполняет элементы страницы данными приложения.

        ready: function (element, options) {
            // TODO: Инициализируйте страницу здесь.
            //var tmp = 
            Windows.Devices.Enumeration.DeviceInformation.findAllAsync("System.Devices.InterfaceClassGuid:=\"{0000180D-0000-1000-8000-00805f9b34fb}\"", null).
            done(function (devices) {

            });

            var output = document.getElementById('page');
            WinJS.Resources.processAll(output);

            if (!HeartRateMeasurement.hrmInitialized) {
                // initializeHeartRateDevices will set the value of hrmInitialized depending 
                // on whether devices were successfully initialized or not.
                HeartRateMeasurement.initializeHeartRateDevicesAsync(0);
            }
        }
    });
})();
