(function () {
    "use strict";

    var list = new WinJS.Binding.List();
    var availableDevices = null;

    function sendSession() {
        var newSession = new ClientServerInteraction.WinRT.Session();
        newSession.startTimestamp = MeasurementData.startTime;
        newSession.deviceId = MeasurementData.deviceId;
        newSession.deviceName = MeasurementData.deviceId;
        var measurements = MeasurementData.getMeasurements();
        newSession.rates = measurements.rates;
        newSession.intervals = measurements.intervals;
        newSession.userId = AuthData.user.idString;
        var inputs = document.getElementsByTagName('input');

        for (var i = 0; i < inputs.length; i++) {

            if (inputs[i].getAttribute('name') == 'activity') {
                if (inputs[i].checked) {
                    if (inputs[i].getAttribute('id') == 'sleep') {
                        newSession.activity = 1;
                    }
                    if (inputs[i].getAttribute('id') == 'rest') {
                        newSession.activity = 2;
                    }
                    if (inputs[i].getAttribute('id') == 'work') {
                        newSession.activity = 3;
                    }
                    if (inputs[i].getAttribute('id') == 'training') {
                        newSession.activity = 4;
                    }
                }
            }
        }
        newSession.healthState = document.getElementById('stateRating').winControl.userRating;
        ClientServerInteraction.WinRT.ServerHelper.addSession(newSession, AuthData.user.idString).done(function (session) {
            AuthData.user.sessions.push(session.idString);
            WinJS.Navigation.navigate("/pages/session/session.html", { session: session });
        });
    }

    function timestampToDateString(timestamp) {
        var date = new Date(timestamp);
        return date.toLocaleDateString() + " " + date.toLocaleTimeString();
    }

    function getCurrentTimestamp() {
        return new Date().getTime();
    }

    function deviceSelected(args) {
        document.getElementById('saveButton').addEventListener('click', sendSession);
        HeartRateMeasurement.initializeHeartRateDevicesAsync(availableDevices.getAt(args.detail.itemIndex));
    }

    function selectionChanged () {
        var stateSelected = true;
        var activitySelected = false;
        var activities = document.getElementsByName('activity');

        for (var i = 0; i < activities.length; i++) {
            if (activities.item(i).checked) {
                activitySelected = true;
            }
        }

        if (document.getElementById('stateRating').winControl.userRating > 0) {
            stateSelected = true;
        }
        if (stateSelected && activitySelected) {
            document.getElementById('saveButton').disabled = false;
        }
    }

    WinJS.UI.Pages.define("/pages/new/new.html", {
        // Эта функция вызывается каждый раз, когда пользователь переходит на данную страницу. Она
        // заполняет элементы страницы данными приложения.

        ready: function (element, options) {
            // TODO: Инициализируйте страницу здесь.
            document.getElementById('backButton').style.visibility = "visible";
            document.getElementById('chooseDeviceLabel').style.visibility = "visible";
            document.getElementById('deviceNameLabel').style.visibility = "hidden";
            document.getElementById('saveButton').addEventListener('click', sendSession);
            document.getElementById('saveButton').disabled = true;
            document.getElementById('deviceList').style.visibility = "visible";

            //var inputs = document.getElementsByTagName('input');
            var activities = document.getElementsByName('activity');

            for (var i = 0; i < activities.length; i++) {
                activities.item(i).addEventListener('change', selectionChanged);
            }

            document.getElementById('stateRating').addEventListener('change', selectionChanged);

            /*for (var i = 0; i < inputs.length; i++) {

                if (inputs[i].getAttribute('name') == 'activity') {
                    if (inputs[i].checked) {
                        if (inputs[i].getAttribute('id') == 'sleep') {
                            newSession.activity = 1;
                        }
                        if (inputs[i].getAttribute('id') == 'rest') {
                            newSession.activity = 2;
                        }
                        if (inputs[i].getAttribute('id') == 'work') {
                            newSession.activity = 3;
                        }
                        if (inputs[i].getAttribute('id') == 'training') {
                            newSession.activity = 4;
                        }
                    }
                }
            }*/

            document.getElementById('backButton').addEventListener('click', function (args) {
                WinJS.Navigation.navigate("/pages/home/home.html");
            });
            WinJS.Resources.processAll();

            var devs = [];
            devs.push({
                name: "Demo HRM",
                id: "DemoDevId"
            });
            availableDevices = new WinJS.Binding.List(devs);
            var listView = element.querySelector(".devicelist").winControl;
            listView.itemDataSource = availableDevices.dataSource;
            listView.itemTemplate = element.querySelector(".itemtemplate");
            listView.layout = new WinJS.UI.ListLayout();
            listView.oniteminvoked = deviceSelected;
        }
    });
})();
