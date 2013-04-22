(function () {
    "use strict";

    WinJS.UI.Pages.define("/pages/error/error.html", {
        ready: function (element, options) {
            WinJS.Resources.processAll();
            document.getElementById('errorText').textContent = WinJS.Resources.getString(options.error).value;
            document.getElementById('tryAgain').addEventListener('click', function () {
                WinJS.Navigation.navigate(options.sender);
            });
        },

        unload: function () {

        },

        updateLayout: function (element, viewState, lastViewState) {

        }
    });
})();
