(function () {
    "use strict";

    var sessionsCount = 20;
    var loadedSessionsCount;
    var canLoadMore = false;

    var previousSessions = null;

    function initializeList() {
        var user = AuthData.user;
        var sessionIds = AuthData.user.sessions;

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
            Homepage.pushChild({ image: "/images/add.png", text: "New session", id: "new" });
            for (var i = sessions.length - 1; i >= 0; i--) {
                var image;
                if (sessions[i]["activity"] == 1) {
                    image = "/images/sleep.png";
                }
                if (sessions[i]["activity"] == 2) {
                    image = "/images/rest.png";
                }
                if (sessions[i]["activity"] == 3) {
                    image = "/images/work.png";
                }
                if (sessions[i]["activity"] == 4) {
                    image = "/images/training.png";
                }
                Homepage.pushChild({ image: image, text: timestampToDateString(sessions[i].startTimestamp), id: sessions[i].idString });
                AuthData.sessions.push(sessions[i]);
            }
            if (canLoadMore) {
                Homepage.pushChild({ image: "/images/add.png", text: "Load more...", id: "load" });
            }
        });
    }

    function itemInvoked(args) {
        if (args.currentTarget.id == "load") {
            loadMoreSessions();
        }
        else if (args.currentTarget.id == "new") {
            WinJS.Navigation.navigate("/pages/new/new.html");
        }
        else {
            var sessionId = args.currentTarget.id;
            var session;
            for (var key in AuthData.sessions) {
                if (AuthData.sessions[key].idString = sessionId)
                    session = AuthData.sessions[key];
            }
            WinJS.Navigation.navigate("/pages/session/session.html", { session: session });
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
        ClientServerInteraction.WinRT.ServerHelper.getSessions(request).done(function (sessions) {
            Homepage.popChild();
            loadedSessionsCount += sessions.length;
            for (var i = sessions.length - 1; i >= 0; i--) {
                var image;
                if (sessions[i]["activity"] == 1) {
                    image = "/images/sleep.png";
                }
                if (sessions[i]["activity"] == 2) {
                    image = "/images/rest.png";
                }
                if (sessions[i]["activity"] == 3) {
                    image = "/images/work.png";
                }
                if (sessions[i]["activity"] == 4) {
                    image = "/images/training.png";
                }
                Homepage.pushChild({ image: image, text: timestampToDateString(sessions[i].startTimestamp), id: sessions[i].idString });
                AuthData.sessions.push(sessions[i]);
            }
            if (canLoadMore) {
                Homepage.pushChild({ image: "/images/add.png", text: "Load more...", id: "load" });
            }
        });
    }

    function timestampToDateString(timestamp) {
        var date = new Date(timestamp);
        return date.toLocaleDateString() + " " + date.toLocaleTimeString();
    }

    function logout() {
        var passwordVault = new Windows.Security.Credentials.PasswordVault();
        var appKey = "OmniHealthDemoApp";
        var credential = passwordVault.retrieve(appKey, passwordVault.findAllByResource(appKey).getAt(0).userName);
        passwordVault.remove(credential);
        WinJS.Navigation.navigate("/pages/auth/auth.html");
    }
    
    var cTimeout;

    function handle(delta) {
        if (delta < 0)
            ScrollSmoothly(10, 10, 'right');
        else if (delta > 0)
            ScrollSmoothly(10, 10, 'left');
        else
            ;
    }

    function wheel(event) {
        var delta = 0;
        if (!event)
            event = window.event;
        if (event.wheelDelta) {
            delta = event.wheelDelta / 120;
            if (window.opera)
                delta = -delta;
        } else if (event.detail) {
            delta = -event.detail / 3;
        }
        if (delta)
            handle(delta);
        if (event.preventDefault)
            event.preventDefault();
        event.returnValue = false;
    }

    var repeatCount = 0;

    function ScrollSmoothly(scrollPos, repeatTimes, direction) {
        if (repeatCount < repeatTimes)
            if (direction == 'right')
                window.scrollBy(20, 0);
            else
                window.scrollBy(-20, 0);
        else {
            repeatCount = 0;
            clearTimeout(cTimeout);
            return;
        }
        repeatCount++;
        cTimeout = setTimeout(ScrollSmoothly(scrollPos, repeatTimes, direction), 10);
    }

    WinJS.Namespace.define("Homepage", {
        pushChild: function (args) {
            var sessionsList = document.getElementById("session-list");
            var newItem = document.createElement('div');
            newItem.className = "tile double image bg-color-blue outline-color-yellow";
            var content = document.createElement('div');
            content.className = "tile-content";
            var image = document.createElement('img');
            image.src = args.image;
            content.appendChild(image);
            var brand = document.createElement('div');
            brand.className = "brand";
            var name = document.createElement('span');
            name.className = "name";
            name.textContent = args.text;
            brand.appendChild(name);
            newItem.appendChild(content);
            newItem.appendChild(brand);
            newItem.id = args.id;
            newItem.onclick = itemInvoked;
            sessionsList.appendChild(newItem);
        },
        popChild: function () {
            var sessionsList = document.getElementById("session-list");
            sessionsList.removeChild(sessionsList.lastChild);
        }
    });

    WinJS.UI.Pages.define("/pages/home/home.html", {
        ready: function (element, options) {
            WinJS.Resources.processAll();
            WinJS.UI.processAll();
            document.getElementById("logoutButton").onclick = logout;
            initializeList();
            
            if (window.addEventListener)
                window.addEventListener('DOMMouseScroll', wheel, false);
            window.onmousewheel = document.onmousewheel = wheel;
        }
    });
})();
