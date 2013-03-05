// Основные сведения о шаблоне элементов управления страницами см. в следующей документации:
// http://go.microsoft.com/fwlink/?LinkId=232511
(function () {
    "use strict";

    WinJS.UI.Pages.define("/pages/blank/blank.html", {
        // Эта функция вызывается каждый раз, когда пользователь переходит на данную страницу. Она
        // заполняет элементы страницы данными приложения.
        ready: function (element, options) {
            try {
                var passwordVault = new Windows.Security.Credentials.PasswordVault();
                var appKey = "OmniHealthDemoApp";
                //passwordVault.add(new Windows.Security.Credentials.PasswordCredential(appKey, "pogr.yuo@gmail.com", "02034242"));
                var credential = passwordVault.retrieve(appKey, passwordVault.findAllByResource(appKey).getAt(0).userName);
                var username = credential.userName;
                var password = credential.password;
                ClientServerInteraction.WinRT.ServerHelper.authorizeUser(username, password).done(function (user) {
                    if (user[0] == null && user[1] == null) {
                        return WinJS.Navigation.navigate("/pages/error/error.html", { sender: WinJS.Navigation.location, error: Errors.notExist });
                    }
                    else if (user[0] == null && user[1] != null) {
                        return WinJS.Navigation.navigate("/pages/error/error.html", { sender: WinJS.Navigation.location, error: Errors.notConnected });
                    }
                    AuthData.user = user[0];
                    return setTimeout(function () {

                        WinJS.Navigation.navigate("/pages/home/home.html", null);
                    }, 0);
                });
            }
            catch (ex) {
                return setTimeout(function () {
                    WinJS.Navigation.navigate("/pages/auth/auth.html", null);
                }, 0);
            }
        },

        unload: function () {
            // TODO: Ответ на переходы с этой страницы.
        },

        updateLayout: function (element, viewState, lastViewState) {
            /// <param name="element" domElement="true" />

            // TODO: Ответ на изменения в состоянии viewState.
        }
    });
})();
