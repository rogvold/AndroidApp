// Основные сведения о шаблоне навигации см. в следующей документации:
// http://go.microsoft.com/fwlink/?LinkId=232506
(function () {
    "use strict";

    WinJS.Binding.optimizeBindingReferences = true;

    var app = WinJS.Application;
    var activation = Windows.ApplicationModel.Activation;
    var nav = WinJS.Navigation;

    app.addEventListener("activated", function (args) {
        if (args.detail.kind === activation.ActivationKind.launch) {
            if (args.detail.previousExecutionState !== activation.ApplicationExecutionState.terminated) {
                // TODO: Это приложение было вновь запущено. Инициализируйте
                // приложение здесь.
            } else {
                // TODO: Это приложение вновь активировано после приостановки.
                // Восстановите состояние приложения здесь.
            }

            if (app.sessionState.history) {
                nav.history = app.sessionState.history;
            }
            args.setPromise(WinJS.UI.processAll().then(function () {
                if (nav.location) {
                    nav.history.current.initialPlaceholder = true;
                    return nav.navigate(nav.location, nav.state);
                } else {
                    try {
                        var passwordVault = new Windows.Security.Credentials.PasswordVault();
                        var appKey = "OmniHealthApp";
                        var credential = passwordVault.retrieve(appKey, passwordVault.findAllByResource(appKey).getAt(0).userName);
                        var username = credential.userName;
                        var password = credential.password;
                        ClientServerInteraction.WinRT.ServerHelper.authorizeUser(username, password).done(function (user) {
                            if (user == null) {
                                return nav.navigate("/pages/error/error.html", { sender: WinJS.Navigation.location, error: Errors.exist });
                            }
                            AuthData.user = user;
                            return nav.navigate(Application.navigator.home);
                        });
                    }
                    catch (ex) {
                        return nav.navigate("/pages/auth/auth.html");
                    }
                }
            }));
        }
    });

    app.oncheckpoint = function (args) {
        // TODO: Это приложение будет приостановлено. Сохраните все состояния,
        // которые должны сохраняться во время приостановки. Если необходимо 
        // завершить асинхронную операцию, прежде чем приложение 
        // будет приостановлено, вызовите args.setPromise().
        app.sessionState.history = nav.history;
    };

    app.start();
})();
