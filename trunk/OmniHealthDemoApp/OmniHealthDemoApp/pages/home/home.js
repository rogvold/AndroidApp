(function () {
    "use strict";

    var elem;
    var sessionsCount = 10;
    var loadedSessionsCount;
    var canLoadMore = false;

    var previousSessions = null;

    function initializeListView() {
        var user = AuthData.user;
        var sessionIds = AuthData.user.sessions;

        if (AuthData.sessions.length == 0) {
            var request;
            if (sessionIds.length < sessionsCount) {
                request = sessionIds;
            } else {
                request = sessionIds.slice(sessionIds.length - sessionsCount, sessionIds.length);
                canLoadMore = true;
            }
            ClientServerInteraction.WinRT.ServerHelper.getSessions(request).done(function (sessions) {
                loadedSessionsCount = sessions.length;
                if (previousSessions)
                    previousSessions.splice(0, previousSessions.length);
                var newSes = [];
                newSes["date"] = "New session";
                newSes["image"] = "/images/add.png";
                AuthData.sessions.push(newSes);
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
                    var timestamps = [];
                    var intervals = [];
                    var rates = [];
                    var startTimestamp = session["startTimestamp"];
                    for (var j = 0; j < session.intervals.length; j++) {
                        timestamps.push(startTimestamp);
                        intervals.push(session.intervals[j]);
                        rates.push(session.rates[j]);
                        startTimestamp += session.intervals[j];
                    }
                    newSession["timestamps"] = timestamps;
                    newSession["intervals"] = intervals;
                    newSession["rates"] = rates;
                    AuthData.sessions.push(newSession);
                }
                if (canLoadMore) {
                    var moreSes = [];
                    moreSes["date"] = "Load more...";
                    moreSes["image"] = "/images/add.png";
                    AuthData.sessions.push(moreSes);
                }

                previousSessions = new WinJS.Binding.List(AuthData.sessions);
                var listView = elem.querySelector(".itemslist").winControl;
                listView.itemDataSource = previousSessions.dataSource;
                listView.itemTemplate = elem.querySelector(".itemtemplate");
                listView.oniteminvoked = itemInvoked;
                listView.layout = new WinJS.UI.GridLayout();
            });
        } else {
            if (previousSessions)
                previousSessions.splice(0, previousSessions.length);
            previousSessions = new WinJS.Binding.List(AuthData.sessions);
            var listView = elem.querySelector(".itemslist").winControl;
            listView.itemDataSource = previousSessions.dataSource;
            listView.itemTemplate = elem.querySelector(".itemtemplate");
            listView.oniteminvoked = itemInvoked;
            listView.layout = new WinJS.UI.GridLayout();
        }
    }

    function itemInvoked(args) {
        if (AuthData.sessions[args.detail.itemIndex].date == "Load more...") {
            loadMoreSessions();
        }
        else if (AuthData.sessions[args.detail.itemIndex].date == "New session") {
            WinJS.Navigation.navigate("/pages/new/new.html");
        }
        else {
            var session = previousSessions.getAt(args.detail.itemIndex);
            WinJS.Navigation.navigate("/pages/session/session.html", { sessionIndex: args.detail.itemIndex });
        }
    }

    function loadMoreSessions() {
        var sessionIds = AuthData.user.sessions.slice(0, AuthData.user.sessions.length - loadedSessionsCount);
        var request;
        if (sessionIds.length < sessionsCount) {
            canLoadMore = false;
            request = sessionIds;
        } else {
            canLoadMore = true;
            request = sessionIds.slice(sessionIds.length - sessionsCount, sessionIds.length);
        }
        AuthData.sessions.pop();
        previousSessions.pop();
        ClientServerInteraction.WinRT.ServerHelper.getSessions(request).done(function (sessions) {
            loadedSessionsCount += sessions.length;
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
                var timestamps = [];
                var intervals = [];
                var rates = [];
                var startTimestamp = session["startTimestamp"];
                for (var j = 0; j < session.intervals.length; j++) {
                    timestamps.push(startTimestamp);
                    intervals.push(session.intervals[j]);
                    rates.push(session.rates[j]);
                    startTimestamp += session.intervals[j];
                }
                newSession["timestamps"] = timestamps;
                newSession["intervals"] = intervals;
                newSession["rates"] = rates;
                previousSessions.push(newSession);
                AuthData.sessions.push(newSession);
            }
            if (canLoadMore) {
                var moreSes = [];
                moreSes["date"] = "Load more...";
                moreSes["image"] = "/images/add.png";
                previousSessions.push(moreSes);
                AuthData.sessions.push(moreSes);
            }

            var listView = elem.querySelector(".itemslist").winControl;
            listView.itemDataSource = previousSessions.dataSource;
            listView.itemTemplate = elem.querySelector(".itemtemplate");
            listView.oniteminvoked = itemInvoked;;
            listView.layout = new WinJS.UI.GridLayout();
        });
    }
    
    function timestampToDateString(timestamp) {
        var date = new Date(timestamp);
        return date.toLocaleDateString() + " " + date.toLocaleTimeString();
    }

    function logout() {
        var passwordVault = new Windows.Security.Credentials.PasswordVault();
        var appKey = "OmniHealthApp";
        var credential = passwordVault.retrieve(appKey, passwordVault.findAllByResource(appKey).getAt(0).userName);
        passwordVault.remove(credential);
        WinJS.Navigation.navigate("/pages/auth/auth.html");
    }

    WinJS.UI.Pages.define("/pages/home/home.html", {
        ready: function (element, options) {
            WinJS.Resources.processAll();
            document.getElementById("logoutButton").onclick = logout;
            elem = element;
            var listView = elem.querySelector(".itemslist").winControl;
            var tmpData = new WinJS.Binding.List(data);
            listView.itemDataSource = tmpData.dataSource;
            listView.itemTemplate = elem.querySelector(".itemtemplate");
            listView.oniteminvoked = itemInvoked;;
            listView.layout = new WinJS.UI.GridLayout();
            //initializeListView();
        }
    });
})();
