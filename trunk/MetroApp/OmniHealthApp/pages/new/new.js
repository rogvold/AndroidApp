(function () {
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


    function sendSession() {
        var intervalsList = [];
        var devs = MeasurementData.getDevices();
        for (var key in MeasurementData.getDevices()[0].data) {
            var interval = MeasurementData.getDevices()[0].data[key].value;
            intervalsList.push(interval);
        }
        //var sessionData = new HrmMath.Data.SessionData(MeasurementData.getDevices()[0].description, intervalsList);
        //var filteredList = HrmMath.Util.Filter.filtrate(sessionData);
        //var filteredSessionData = new HrmMath.Data.SessionData(MeasurementData.getDevices()[0].description, filteredList);

        //var rsai = filteredSessionData.evaluate(new HrmMath.Evaluation.HRV.RSAI());
        //document.getElementById('rsai').textContent = rsai[0] + ' ' + rsai[1];
        var newSession = new ClientServerInteraction.WinRT.Session();
        newSession.startTimestamp = MeasurementData.startTime;
        newSession.deviceId = MeasurementData.deviceId;
        newSession.deviceName = HeartRateMeasurement.idToName(MeasurementData.deviceId);
        newSession.rates = intervalsList;
        newSession.userId = AuthData.user.idString;
        ClientServerInteraction.WinRT.ServerHelper.addSession(newSession, AuthData.user.idString).done(function (session) {
            AuthData.user.sessions.push(session.idString);
            var newArray = [];
            var newSession = [];
            session.info = "info";
            for (var key in session) {
                if (session[key] != null)
                    newSession[key] = session[key];
            }
            newSession["date"] = timestampToDateString(newSession["startTimestamp"]);
            newArray.push(newSession);
            AuthData.sessions.pop();
            for (var i = 0; i < AuthData.sessions.length; i++) {
                newArray.push(AuthData.sessions[i]);
            }
            AuthData.sessions = newArray;
            WinJS.Navigation.navigate("/pages/session/session.html", { session: newSession });
        });
    }

    function timestampToDateString(timestamp) {
        var date = new Date(timestamp);
        return date.toLocaleDateString() + " " + date.toLocaleTimeString();
    }

    function getCurrentTimestamp() {
        return new Date().getTime();
    }

    WinJS.UI.Pages.define("/pages/new/new.html", {
        // Эта функция вызывается каждый раз, когда пользователь переходит на данную страницу. Она
        // заполняет элементы страницы данными приложения.

        ready: function (element, options) {
            // TODO: Инициализируйте страницу здесь.
            WinJS.UI.processAll();
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
            var dataChart = new Chart.renderer();
            dataChart.plot("chartCanvasHRM", null);
        },

        _itemInvoked: function (args) {
            var id = availableDevices.getAt(args.detail.itemIndex).id;
            document.getElementById('saveSession').addEventListener('click', sendSession);
            HeartRateMeasurement.initializeHeartRateDevicesAsync(id);
        }
    });
})();
