(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        var form = document.getElementById("employeeFilterForm");
        if (!form) {
            return;
        }

        form.querySelectorAll("[data-auto-submit]").forEach(function (control) {
            control.addEventListener("change", function () {
                submitFilters(form);
            });
        });

        form.addEventListener("submit", function () {
            excludeEmptyValues(form);
        });
    });

    function submitFilters(form) {
        excludeEmptyValues(form);
        if (typeof form.requestSubmit === "function") {
            form.requestSubmit();
            return;
        }
        form.submit();
    }

    function excludeEmptyValues(form) {
        form.querySelectorAll("input[name], select[name]").forEach(function (control) {
            if (control.value.trim() === "") {
                control.disabled = true;
            }
        });
    }
}());
