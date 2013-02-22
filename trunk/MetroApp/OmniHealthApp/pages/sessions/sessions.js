(function () {
    "use strict";

    var list = new WinJS.Binding.List();
    var previousSessions = list.createSorted(function descendingCompare(first, second) {
        if (first == second)
            return 0;
        else if (first < second)
            return 1;
        else
            return -1;
    });

    function createNewSession () {
        WinJS.Navigation.navigate("/pages/new/new.html");
    }

    WinJS.UI.Pages.define("/pages/sessions/sessions.html", {
        // Эта функция вызывается каждый раз, когда пользователь переходит на данную страницу. Она
        // заполняет элементы страницы данными приложения.

        ready: function (element, options) {
            // TODO: Инициализируйте страницу здесь.
            document.getElementById('newSessionButton').onclick = createNewSession;
            var listView = element.querySelector(".itemslist").winControl;
            listView.itemDataSource = previousSessions.dataSource;
            listView.itemTemplate = element.querySelector(".itemtemplate");
            listView.layout = new WinJS.UI.GridLayout();
            //listView.oniteminvoked = this._itemInvoked.bind(this);
            listView.element.focus();
        }
    });
})();
