(function () {
    "use strict";

    var appKey = "OmniHealthDemoApp";

    function authorization(email, password) {
        ClientServerInteraction.WinRT.ServerHelper.authorizeUser(email, password).done(function (user) {
            if (user[0] == null && user[1] == null) {
                return WinJS.Navigation.navigate("/pages/error/error.html", { sender: WinJS.Navigation.location, error: Errors.notExist });
            }
            else if (user[0] == null && user[1] != null) {
                return WinJS.Navigation.navigate("/pages/error/error.html", { sender: WinJS.Navigation.location, error: Errors.notConnected });
            }
            AuthData.user = user[0];
            var passwordVault = new Windows.Security.Credentials.PasswordVault();
            passwordVault.add(new Windows.Security.Credentials.PasswordCredential(appKey, email, password));
            return WinJS.Navigation.navigate(Application.navigator.home);
        });

    }

    function authSubmit() {
        document.getElementById("progressRing").style.visibility = "visible";
        var email = document.getElementById("loginField").value;
        var password = document.getElementById("passwordField").value;
        authorization(email, password);
    }

    function checkData(args) {
        var email = document.getElementById("loginField").value;
        var password = document.getElementById("passwordField").value;
        if (password != null && password != "" && email != null && email != "") {
            document.getElementById("signInButton").disabled = false;
        }
        else {
            document.getElementById("signInButton").disabled = true;
        }
    }

    WinJS.UI.Pages.define("/pages/auth/auth.html", {
        ready: function (element, options) {
            WinJS.Resources.processAll();
            document.getElementById('backButton').disabled = true;
            document.getElementById("signInButton").disabled = true;
            document.getElementById("progressRing").style.visibility = "hidden";
            document.getElementById("loginField").onkeyup = checkData;
            document.getElementById("passwordField").onkeyup = checkData;
            document.getElementById('signInButton').onclick = authSubmit;
        }
    });
})();
