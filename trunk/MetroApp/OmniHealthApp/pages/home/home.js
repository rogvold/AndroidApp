(function () {
    "use strict";

    var elem;
    var sessionsCount = 10;

    var previousSessions = null;

    function createNewSession() {
        WinJS.Navigation.navigate("/pages/new/new.html");
    }

    function initializeListView() {
        var user = AuthData.user;
        var sessionIds = AuthData.user.sessions;
        var ses = [];
        ses["date"] = "New session";
        ses["image"] = "/images/add.png";
        AuthData.sessions.push(ses);

        if (AuthData.sessions.length == 1) {
            ClientServerInteraction.WinRT.ServerHelper.getSessions(sessionIds.slice(sessionIds.length - sessionsCount, sessionIds.length)).done(function (sessions) {
                if (previousSessions)
                    previousSessions.splice(0, previousSessions.length);
                for (var i = sessions.length - 1; i >= 0; i--) {
                    var session = sessions[i];
                    var newSession = [];
                    session['info'] = "info";
                    for (var key in session) {
                        if (session[key] != null)
                            newSession[key] = session[key];
                    }
                    if (session["activity"] == 1) {
                        newSession["image"] = "/images/sleep.png";
                    }
                    if (session["activity"] == 2) {
                        newSession["image"] = "/images/rest.png";
                    }
                    if (session["activity"] == 3) {
                        newSession["image"] = "/images/work.png";
                    }
                    if (session["activity"] == 4) {
                        newSession["image"] = "/images/training.png";
                    }
                    newSession["date"] = timestampToDateString(newSession["startTimestamp"]);
                    AuthData.sessions.push(newSession);
                }
                previousSessions = new WinJS.Binding.List(AuthData.sessions);
                var listView = elem.querySelector(".itemslist").winControl;
                listView.itemDataSource = previousSessions.dataSource;
                listView.itemTemplate = elem.querySelector(".itemtemplate");
                listView.oniteminvoked = itemInvoked;;
                listView.layout = new WinJS.UI.GridLayout();
                listView.element.focus();
            });
        } else {
            if (previousSessions)
                previousSessions.splice(0, previousSessions.length);
            previousSessions = new WinJS.Binding.List(AuthData.sessions);
            var listView = elem.querySelector(".itemslist").winControl;
            listView.itemDataSource = previousSessions.dataSource;
            listView.itemTemplate = elem.querySelector(".itemtemplate");
            listView.oniteminvoked = itemInvoked;;
            listView.layout = new WinJS.UI.GridLayout();
            listView.element.focus();
        }
    }

    function itemInvoked(args) {
        if (args.detail.itemIndex != 0) {
            var session = previousSessions.getAt(args.detail.itemIndex);
            WinJS.Navigation.navigate("/pages/session/session.html", { session: session });
        }
        else {
            createNewSession();
        }

    }
    
    function timestampToDateString(timestamp) {
        var date = new Date(timestamp);
        return date.toLocaleDateString() + " " + date.toLocaleTimeString();
    }

    WinJS.UI.Pages.define("/pages/home/home.html", {
        ready: function (element, options) {
            WinJS.Resources.processAll();
            elem = element;
            initializeListView();
        }
    });
})();
