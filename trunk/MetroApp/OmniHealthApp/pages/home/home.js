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

        if (AuthData.sessions.length == 0) {
            ClientServerInteraction.WinRT.ServerHelper.getSessions(sessionIds.slice(sessionIds.length - sessionsCount, sessionIds.length)).done(function (sessions) {
                previousSessions.splice(0, previousSessions.length);
                for (var i = sessions.length - 1; i >= 0; i--) {
                    var session = sessions[i];
                    var newSession = [];
                    session['info'] = "info";
                    for (var key in session) {
                        if (session[key] != null)
                            newSession[key] = session[key];
                    }
                    newSession["date"] = timestampToDateString(newSession["startTimestamp"]);
                    previousSessions.push(newSession);
                    AuthData.sessions.push(newSession);
                }
                var listView = elem.querySelector(".itemslist").winControl;
                listView.itemDataSource = previousSessions.dataSource;
                listView.itemTemplate = elem.querySelector(".itemtemplate");
                listView.oniteminvoked = itemInvoked;;
                listView.layout = new WinJS.UI.GridLayout();
                listView.element.focus();
            });
        } else {
            previousSessions.splice(0, previousSessions.length);
            for (var i = 0; i < AuthData.sessions.length; i++) {
                previousSessions.push(AuthData.sessions[i]);
            }
            var listView = elem.querySelector(".itemslist").winControl;
            listView.itemDataSource = previousSessions.dataSource;
            listView.itemTemplate = elem.querySelector(".itemtemplate");
            listView.oniteminvoked = itemInvoked;;
            listView.layout = new WinJS.UI.GridLayout();
            listView.element.focus();
        }
    }

    function itemInvoked(args) {
        var session = previousSessions.getAt(args.detail.itemIndex);
        WinJS.Navigation.navigate("/pages/session/session.html", { session: session });
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
