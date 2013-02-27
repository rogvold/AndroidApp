﻿(function () {
    "use strict";

    var list = new WinJS.Binding.List();
    var availableDevices = list.createSorted(function descendingCompare(first, second) {
        if (first == second)
            return 0;
        else if (first < second)
            return 1;
        else
            return -1;
    });

    WinJS.UI.Pages.define("/pages/new/new.html", {
        // Эта функция вызывается каждый раз, когда пользователь переходит на данную страницу. Она
        // заполняет элементы страницы данными приложения.

        ready: function (element, options) {
            // TODO: Инициализируйте страницу здесь.
            availableDevices.splice(0, availableDevices.length);
            Windows.Devices.Enumeration.DeviceInformation.findAllAsync("System.Devices.InterfaceClassGuid:=\"{0000180D-0000-1000-8000-00805f9b34fb}\"", null).
            done(function (devices) {
                for (var i = 0; i < devices.length; i++) {
                    if (i % 2 == 0) {
                        availableDevices.push({
                            name: HeartRateMeasurement.idToName(devices[i].id),
                            id: devices[i].id
                        });
                    }
                }
            });
            var listView = element.querySelector(".itemslist").winControl;
            listView.itemDataSource = availableDevices.dataSource;
            listView.itemTemplate = element.querySelector(".itemtemplate");
            listView.layout = new WinJS.UI.ListLayout();
            listView.oniteminvoked = this._itemInvoked.bind(this);
            listView.element.focus();
        },

        _itemInvoked: function (args) {
            var id = availableDevices.getAt(args.detail.itemIndex).id;
            HeartRateMeasurement.initializeHeartRateDevicesAsync(id);
        }
    });
})();
