(function () {
    "use strict";

    var appKey = "OmniHealthApp";

    function authorization(email, password) {
        ClientServerInteraction.WinRT.ServerHelper.authorizeUser(email, password).done(function (user) {
            if (user[0] == null && user[1] == null) {
                return WinJS.Navigation.navigate("/pages/error/error.html", { sender: WinJS.Navigation.location, error: Errors.notExist });
            }
            else if (user[0] == null && user[1] != null) {
                return WinJS.Navigation.navigate("/pages/error/error.html", { sender: WinJS.Navigation.location, error: Errors.notConnected });
            }
            AuthData.user = user[0];
            return WinJS.Navigation.navigate(Application.navigator.home);
        });

    }

    function authSubmit() {
        document.getElementById("progressRing").style.visibility = "visible";
        var email = document.getElementById("loginField").value;
        var password = document.getElementById("passwordField").value;
        var passwordVault = new Windows.Security.Credentials.PasswordVault();
        if (password != null && password != "" && email != null && email != "") { //TODO: bad syntax in "if" body. Notify user about empty fields
            passwordVault.add(new Windows.Security.Credentials.PasswordCredential(appKey, email, password));
            authorization(email, password);
        }
    }

    WinJS.UI.Pages.define("/pages/auth/auth.html", {
        // Эта функция вызывается каждый раз, когда пользователь переходит на данную страницу. Она
        // заполняет элементы страницы данными приложения.

        ready: function (element, options) {
            // TODO: Инициализируйте страницу здесь.
            //var tmp = new HrmMath.Data.SessionData();
            //var tmp1 = tmp.evaluate(new HrmMath.Evaluation.HRV.RSAI());
            //TODO: check if user idString already exist and redirect to the next page  
            WinJS.Resources.processAll();
            document.getElementById('backButton').disabled = true;
            document.getElementById("progressRing").style.visibility = "hidden";
            document.getElementById('signInButton').onclick = authSubmit;
        }
    });
})();
