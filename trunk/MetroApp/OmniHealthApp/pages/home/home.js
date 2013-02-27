(function () {
    "use strict";

    var elem;
    var sessionsCount = 10;

    var list = new WinJS.Binding.List();
    var previousSessions = list.createSorted(function descendingCompare(first, second) {
        if (first == second)
            return 0;
        else if (first < second)
            return 1;
        else
            return -1;
    });

    function createNewSession() {
        WinJS.Navigation.navigate("/pages/new/new.html");
    }

    function initializeListView() {
        var user = AuthData.user;
        var sessionIds = AuthData.user.sessions;

        ClientServerInteraction.WinRT.ServerHelper.getSessions(sessionIds.slice(0, sessionsCount + 1)).done(function (sessions) {
            for (var i = 0; i < sessions.length; i++) {
                var session = sessions[i];
                var newSession = [];
                for (var key in session) {
                    if (session[key] != null)
                        newSession[key] = session[key];
                }
                newSession["date"] = timestampToDateString(newSession["startTimestamp"]);
                previousSessions.push(newSession);
            }
            var listView = elem.querySelector(".itemslist").winControl;
            listView.itemDataSource = previousSessions.dataSource;
            listView.itemTemplate = elem.querySelector(".itemtemplate");
            listView.layout = new WinJS.UI.GridLayout();
            listView.element.focus();
        });
    }
    
    function timestampToDateString(timestamp) {
        var date = new Date(timestamp);
        return date.toLocaleDateString() + " " + date.toLocaleTimeString();
    }

    WinJS.UI.Pages.define("/pages/home/home.html", {
        ready: function (element, options) {
            WinJS.Resources.processAll();
            document.getElementById('newSessionButton').onclick = createNewSession;
            elem = element;
            initializeListView();
        }
    });
})();
