(function () {
    "use strict";

    var appKey = "OmniHealthApp";

    function authorization(email, password) {
        ClientServerInteraction.WinRT.ServerHelper.authorizeUser(email, password).done(function (user) {
            if (user == null) {
                //TODO: notify user about authorization fail
                return;
            }
            AuthData.user = user;
            WinJS.Navigation.navigate("/pages/sessions/sessions.html");
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
            //TODO: check if user idString already exist and redirect to the next page  
            var output = document.getElementById('page');
            WinJS.Resources.processAll(output);
            try {
                var passwordVault = new Windows.Security.Credentials.PasswordVault();
                var credential = passwordVault.retrieve(appKey, passwordVault.findAllByResource(appKey).getAt(0).userName);
                authorization(credential.userName, credential.password);
            }
            catch (ex) {

            }
            document.getElementById('signInButton').onclick = authSubmit;
            document.getElementById('cancelButton').onclick = authReset;
        }
    });
})();
