(function () {
    "use strict";

    var appKey = "OmniHealthApp";
    var username;
    var password;

    function authorization() {
        ClientServerInteraction.WinRT.ServerHelper.authorizeUser("pogr.yuo@gmail.com", "02034242").done(function (user) {
            var tmp = 0;
        });
        WinJS.Navigation.navigate("/pages/sessions/sessions.html");
    }

    function authSubmit() {
        document.getElementById("progressRing").style.visibility = "visible";
        username = document.getElementById("loginField").value;
        password = document.getElementById("passwordField").value;
        var passwordVault = new Windows.Security.Credentials.PasswordVault();
        passwordVault.add(new Windows.Security.Credentials.PasswordCredential(appKey, username, password));
        authorization();
    }

    function authReset() {
        document.getElementById("loginField").value = "";
        document.getElementById("passwordField").value = "";
    }
    
    WinJS.UI.Pages.define("/pages/home/home.html", {
        // Эта функция вызывается каждый раз, когда пользователь переходит на данную страницу. Она
        // заполняет элементы страницы данными приложения.

        ready: function (element, options) {
            // TODO: Инициализируйте страницу здесь.
            //var tmp = new HrmMath.Data.SessionData();
            //var tmp1 = tmp.evaluate(new HrmMath.Evaluation.HRV.RSAI());
            var passwordVault = new Windows.Security.Credentials.PasswordVault();
            try {
                var credential = passwordVault.retrieve(appKey, passwordVault.findAllByResource(appKey).getAt(0).userName);
                username = credential.userName;
                password = credential.password;
                authorization();
            }
            catch (ex) {
                var output = document.getElementById('page');
                WinJS.Resources.processAll(output);
                document.getElementById('signInButton').onclick = authSubmit;
                document.getElementById('cancelButton').onclick = authReset;
            }
        }
    });
})();
