(function () {
    "use strict";
    var session;

    function drawRsai(value) {
        var opts = {
            lines: 12, // The number of lines to draw
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
        gauge.maxValue = 11; // set max gauge value
        gauge.animationSpeed = 32; // set animation speed (32 is default value)
        gauge.set(value + 1); // set actual value
    }

    WinJS.UI.Pages.define("/pages/session/session.html", {
        ready: function (element, options) {
            document.getElementById('backButton').addEventListener('click', function (args) {
                WinJS.Navigation.navigate("/pages/home/home.html");
            });
            $(function () {
                $("#accordion").accordion({
                    heightStyle: "fill"
                });
            });
            $(function () {
                $("#accordion-resizer").resizable({
                    minHeight: 140,
                    minWidth: 200,
                    resize: function () {
                        $("#accordion").accordion("refresh");
                    }
                });
            });
            WinJS.Resources.processAll();
            var session = options.session;
            drawRsai(session.rsai[0]);
            var chart = new Chart.renderer();
            chart.draw('chartCanvas');
            chart.updateChartData(session);
        },

        unload: function () {
            
        },

        updateLayout: function (element, viewState, lastViewState) {
            
        }
    });
})();
