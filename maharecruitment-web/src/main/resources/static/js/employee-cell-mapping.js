(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        const form = document.getElementById("employeeCellMappingForm");
        const picker = document.getElementById("employeeCellPicker");
        const selectedBox = document.getElementById("selectedEmployeeCellBox");

        if (!form || !picker || !selectedBox) {
            return;
        }

        function selectedOption() {
            return picker.options[picker.selectedIndex] || null;
        }

        function renderSelectedCell() {
            const option = selectedOption();
            const hasSelection = Boolean(option && option.value);
            selectedBox.innerHTML = "";
            selectedBox.classList.toggle("is-empty", !hasSelection);

            if (!hasSelection) {
                const emptyState = document.createElement("div");
                emptyState.className = "employee-location-empty";
                const icon = document.createElement("i");
                icon.className = "fa-regular fa-square";
                const copy = document.createElement("span");
                copy.textContent = selectedBox.dataset.emptyLabel || "No cell selected.";
                emptyState.appendChild(icon);
                emptyState.appendChild(copy);
                selectedBox.appendChild(emptyState);
                return;
            }

            const content = document.createElement("div");
            content.className = "employee-cell-selected-content";

            const iconWrap = document.createElement("span");
            iconWrap.className = "employee-cell-selected-icon";
            const icon = document.createElement("i");
            icon.className = "fa-solid fa-table-cells";
            iconWrap.appendChild(icon);

            const copyWrap = document.createElement("div");
            const cellName = document.createElement("strong");
            cellName.textContent = option.dataset.cell || option.textContent.trim();
            const wingName = document.createElement("span");
            wingName.textContent = option.dataset.wing || "";

            copyWrap.appendChild(cellName);
            copyWrap.appendChild(wingName);
            content.appendChild(iconWrap);
            content.appendChild(copyWrap);
            selectedBox.appendChild(content);
        }

        function syncValidity() {
            picker.setCustomValidity(picker.value ? "" : "Select a cell to map this employee.");
            return Boolean(picker.value);
        }

        if (window.jQuery && window.jQuery.fn && window.jQuery.fn.select2) {
            const $picker = window.jQuery(picker);
            $picker.select2({
                theme: "bootstrap-5",
                width: "100%",
                placeholder: picker.dataset.placeholder || "Search and select cell",
                allowClear: true
            });
            $picker.on("change", function () {
                syncValidity();
                renderSelectedCell();
            });
        } else {
            picker.addEventListener("change", function () {
                syncValidity();
                renderSelectedCell();
            });
        }

        form.addEventListener("submit", function (event) {
            if (!syncValidity()) {
                event.preventDefault();
                picker.reportValidity();
            }
        });

        syncValidity();
        renderSelectedCell();
    });
})();
