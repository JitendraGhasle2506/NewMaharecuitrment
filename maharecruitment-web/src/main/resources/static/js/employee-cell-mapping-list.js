(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        const form = document.getElementById("employeeCellBulkMappingForm");
        const cellPicker = document.getElementById("bulkEmployeeCellPicker");
        const selectAll = document.getElementById("employeeCellSelectAll");
        const selectVisible = document.getElementById("employeeCellSelectVisible");
        const clearSelection = document.getElementById("employeeCellClearSelection");
        const selectedCount = document.getElementById("employeeCellSelectedCount");

        if (!form || !cellPicker) {
            return;
        }

        function rowChecks() {
            return Array.from(form.querySelectorAll(".employee-cell-row-check"));
        }

        function selectedChecks() {
            return rowChecks().filter(function (checkbox) {
                return checkbox.checked;
            });
        }

        function updateSelectionState() {
            const rows = rowChecks();
            const selected = selectedChecks();
            if (selectedCount) {
                selectedCount.textContent = selected.length + (selected.length === 1 ? " selected" : " selected");
            }
            if (selectAll) {
                selectAll.checked = rows.length > 0 && selected.length === rows.length;
                selectAll.indeterminate = selected.length > 0 && selected.length < rows.length;
            }
        }

        function setAllRows(checked) {
            rowChecks().forEach(function (checkbox) {
                checkbox.checked = checked;
            });
            updateSelectionState();
        }

        rowChecks().forEach(function (checkbox) {
            checkbox.addEventListener("change", updateSelectionState);
        });

        if (selectAll) {
            selectAll.addEventListener("change", function () {
                setAllRows(selectAll.checked);
            });
        }

        if (selectVisible) {
            selectVisible.addEventListener("click", function () {
                setAllRows(true);
            });
        }

        if (clearSelection) {
            clearSelection.addEventListener("click", function () {
                setAllRows(false);
            });
        }

        form.addEventListener("submit", function (event) {
            if (!cellPicker.value) {
                event.preventDefault();
                cellPicker.setCustomValidity("Select a cell before assigning employees.");
                cellPicker.reportValidity();
                cellPicker.setCustomValidity("");
                return;
            }
            if (selectedChecks().length === 0) {
                event.preventDefault();
                const firstCheckbox = rowChecks()[0];
                if (firstCheckbox) {
                    firstCheckbox.setCustomValidity("Select at least one employee to assign.");
                    firstCheckbox.reportValidity();
                    firstCheckbox.setCustomValidity("");
                }
            }
        });

        updateSelectionState();
    });
})();
