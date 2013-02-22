(function () {
    "use strict";

    function authorization() {
        document.getElementById("progressRing").style.visibility = "visible";
        WinJS.Navigation.navigate("/pages/sessions/sessions.html");
    }

    function reset() {
        document.getElementById("loginField").value = "";
        document.getElementById("passwordField").value = "";
    }
    
    WinJS.UI.Pages.define("/pages/home/home.html", {
        // Эта функция вызывается каждый раз, когда пользователь переходит на данную страницу. Она
        // заполняет элементы страницы данными приложения.

        ready: function (element, options) {
            // TODO: Инициализируйте страницу здесь.
            var output = document.getElementById('page');
            WinJS.Resources.processAll(output);
            document.getElementById('signInButton').onclick = authorization;
            document.getElementById('cancelButton').onclick = reset;
        }
    });
})();
