// Основные сведения о шаблоне элементов управления страницами см. в следующей документации:
// http://go.microsoft.com/fwlink/?LinkId=232511
(function () {
    "use strict";

    WinJS.UI.Pages.define("/pages/error/error.html", {
        // Эта функция вызывается каждый раз, когда пользователь переходит на данную страницу. Она
        // заполняет элементы страницы данными приложения.
        ready: function (element, options) {
            // TODO: Инициализируйте страницу здесь.
            WinJS.Resources.processAll();
            document.getElementById('errorText').textContent = WinJS.Resources.getString(options.error).value;
            document.getElementById('tryAgain').addEventListener('click', function () {
                WinJS.Navigation.navigate(options.sender);
            });
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
