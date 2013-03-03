// Основные сведения о шаблоне элементов управления страницами см. в следующей документации:
// http://go.microsoft.com/fwlink/?LinkId=232511
(function () {
    "use strict";
    var session;

    function drawRsai(value) {
        var canvasElement = document.getElementById('canvasRsai');
        var canvasContext = canvasElement.getContext('2d');
        canvasElement.width = canvasElement.offsetWidth;
        canvasElement.height = canvasElement.offsetHeight;
        var lingrad = canvasContext.createLinearGradient(0, 0, 0, canvasElement.height);
        var colors = ['#ff0000', '#ff4100', '#ff9700', '#ffe500', '#ffff00', '#ffff00', '#ffff00', '#ffff00', '#b3ff00', '#4cff00', '#00ff00'];
        for (var i = 0; i <= 10; i++) {
            lingrad.addColorStop(0.1 * i, colors[10 - i]);
        }

        canvasContext.fillStyle = lingrad;
        canvasContext.fillRect(0, (value / 11) * canvasElement.height, canvasElement.width, (1 - value / 11) * canvasElement.height);

        canvasContext.strokeStyle = '#000000';
        canvasContext.strokeRect(0, 0, canvasElement.width, canvasElement.height);
    }

    WinJS.UI.Pages.define("/pages/session/session.html", {
        // Эта функция вызывается каждый раз, когда пользователь переходит на данную страницу. Она
        // заполняет элементы страницы данными приложения.
        ready: function (element, options) {
            document.getElementById('backButton').addEventListener('click', function (args) {
                WinJS.Navigation.navigate("/pages/home/home.html");
            });
            var session = AuthData.sessions[options.sessionIndex];
            drawRsai(session.rsai[0]);
            var dataChart = new Chart.renderer();
            dataChart.plot("chartCanvas", session.rates);
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
