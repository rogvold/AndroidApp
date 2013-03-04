// Основные сведения о шаблоне элементов управления страницами см. в следующей документации:
// http://go.microsoft.com/fwlink/?LinkId=232511
(function () {
    "use strict";
    var session;

    function drawRsai(value) {
        /*var canvasElement = document.getElementById('canvasRsai');
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
        canvasContext.strokeRect(0, 0, canvasElement.width, canvasElement.height);*/
        var opts = {
            lines: 11, // The number of lines to draw
            angle: 0.09, // The length of each line
            lineWidth: 0.33, // The line thickness
            pointer: {
                length: 0.62, // The radius of the inner circle
                strokeWidth: 0.042, // The rotation offset
                color: '#000000' // Fill color
            },
            colorStart: '#00FF00',   // Colors
            colorStop: '#FF0000',    // just experiment with them
            strokeColor: '#E0E0E0',   // to see which ones work best for you
            generateGradient: true
        };
        var target = document.getElementById('rsaiCanvas'); // your canvas element
        var gauge = new Gauge(target).setOptions(opts); // create sexy gauge!
        gauge.maxValue = 10; // set max gauge value
        gauge.animationSpeed = 32; // set animation speed (32 is default value)
        gauge.set(value); // set actual value
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
            var chart = new Chart.renderer();
            chart.draw('chartCanvas');
            chart.updateChartData(session);
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
