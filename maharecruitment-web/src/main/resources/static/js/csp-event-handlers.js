(function () {
    "use strict";

    function contextPath() {
        var meta = document.querySelector('meta[name="app-context-path"]');
        var value = meta ? meta.getAttribute("content") : "/";
        if (!value || value === "/") {
            return "/";
        }
        return value.endsWith("/") ? value : value + "/";
    }

    function openManagedDocument(trigger) {
        var path = trigger.getAttribute("data-path");
        if (!path) {
            return;
        }

        try {
            var encodedPath = encodeURIComponent(window.btoa(path));
            var documentWindow = window.open(
                contextPath() + "documents/view?path=" + encodedPath,
                "_blank",
                "noopener,noreferrer"
            );
            if (documentWindow) {
                documentWindow.opener = null;
            }
        } catch (error) {
            window.alert("Unable to open the uploaded document.");
        }
    }

    document.addEventListener("click", function (event) {
        var trigger = event.target.closest("[data-managed-document]");
        if (!trigger) {
            return;
        }

        event.preventDefault();
        openManagedDocument(trigger);
    });
}());
