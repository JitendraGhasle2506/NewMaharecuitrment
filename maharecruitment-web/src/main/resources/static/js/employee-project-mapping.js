(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        const form = document.getElementById("employeeProjectBulkMappingForm");
        const projectPicker = document.getElementById("bulkEmployeeProjectPicker");
        const selectAll = document.getElementById("employeeProjectSelectAll");
        const selectCompatible = document.getElementById("employeeProjectSelectCompatible");
        const clearSelection = document.getElementById("employeeProjectClearSelection");
        const selectedCount = document.getElementById("employeeProjectSelectedCount");
        const scopeHint = document.getElementById("employeeProjectScopeHint");

        if (!form || !projectPicker) {
            return;
        }

        function checks() {
            return Array.from(form.querySelectorAll(".employee-project-row-check"));
        }

        function selectedScope() {
            const option = projectPicker.options[projectPicker.selectedIndex];
            return option ? option.dataset.scope || "" : "";
        }

        function compatibleChecks() {
            const scope = selectedScope();
            return checks().filter(function (checkbox) {
                const row = checkbox.closest(".employee-project-row");
                return scope && row && row.dataset.employeeScope === scope;
            });
        }

        function updateSelectionState() {
            const compatible = compatibleChecks();
            const selected = checks().filter(function (checkbox) { return checkbox.checked; });
            if (selectedCount) {
                selectedCount.textContent = selected.length + " selected";
            }
            if (selectAll) {
                selectAll.disabled = compatible.length === 0;
                selectAll.checked = compatible.length > 0 && selected.length === compatible.length;
                selectAll.indeterminate = selected.length > 0 && selected.length < compatible.length;
            }
        }

        function applyScope() {
            const scope = selectedScope();
            checks().forEach(function (checkbox) {
                const row = checkbox.closest(".employee-project-row");
                const compatible = Boolean(scope && row && row.dataset.employeeScope === scope);
                checkbox.disabled = !compatible;
                if (!compatible) {
                    checkbox.checked = false;
                }
                if (row) {
                    row.classList.toggle("is-incompatible", Boolean(scope) && !compatible);
                }
            });
            if (scopeHint) {
                scopeHint.textContent = scope
                    ? "Only " + scope.toLowerCase() + " employees can be selected for this project."
                    : "Select a project to enable compatible employees.";
            }
            updateSelectionState();
        }

        checks().forEach(function (checkbox) {
            checkbox.addEventListener("change", updateSelectionState);
        });
        projectPicker.addEventListener("change", applyScope);

        if (selectAll) {
            selectAll.addEventListener("change", function () {
                compatibleChecks().forEach(function (checkbox) {
                    checkbox.checked = selectAll.checked;
                });
                updateSelectionState();
            });
        }
        if (selectCompatible) {
            selectCompatible.addEventListener("click", function () {
                compatibleChecks().forEach(function (checkbox) { checkbox.checked = true; });
                updateSelectionState();
            });
        }
        if (clearSelection) {
            clearSelection.addEventListener("click", function () {
                checks().forEach(function (checkbox) { checkbox.checked = false; });
                updateSelectionState();
            });
        }

        form.addEventListener("submit", function (event) {
            if (!projectPicker.value || checks().every(function (checkbox) { return !checkbox.checked; })) {
                event.preventDefault();
                if (!projectPicker.value) {
                    projectPicker.reportValidity();
                } else {
                    scopeHint.textContent = "Select at least one compatible employee.";
                }
            }
        });

        applyScope();
    });
})();
