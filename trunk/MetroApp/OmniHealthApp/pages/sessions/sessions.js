(function () {
    "use strict";

    function createNewSession () {
        WinJS.Navigation.navigate("/pages/new/new.html");
    }

    WinJS.UI.Pages.define("/pages/sessions/sessions.html", {
        // Эта функция вызывается каждый раз, когда пользователь переходит на данную страницу. Она
        // заполняет элементы страницы данными приложения.

        ready: function (element, options) {
            // TODO: Инициализируйте страницу здесь.
            document.getElementById('newSessionButton').onclick = createNewSession;
        }
    });
})();
