(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        const form = document.getElementById("employeeProjectBulkMappingForm");
        const projectPicker = document.getElementById("bulkEmployeeProjectPicker");
        const bulkProjectId = document.getElementById("employeeProjectBulkProjectId");
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

        function selectedProject() {
            const option = projectPicker.options[projectPicker.selectedIndex];
            if (!option || !option.value) {
                return null;
            }
            return {
                scope: option.dataset.scope || "",
                departmentId: option.dataset.departmentId || "",
                subDepartmentId: option.dataset.subDepartmentId || ""
            };
        }

        function isCompatible(row, project) {
            if (!row || !project || !project.scope) {
                return false;
            }
            if (row.dataset.employeeScope !== project.scope) {
                return false;
            }
            if (project.subDepartmentId) {
                return row.dataset.employeeSubDepartmentId === project.subDepartmentId;
            }
            return Boolean(project.departmentId)
                && row.dataset.employeeDepartmentId === project.departmentId;
        }

        function compatibleChecks() {
            const project = selectedProject();
            return checks().filter(function (checkbox) {
                const row = checkbox.closest(".employee-project-row");
                return isCompatible(row, project);
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
            const project = selectedProject();
            if (bulkProjectId) {
                bulkProjectId.value = projectPicker.value || "";
            }
            checks().forEach(function (checkbox) {
                const row = checkbox.closest(".employee-project-row");
                const compatible = isCompatible(row, project);
                checkbox.disabled = !compatible;
                if (!compatible) {
                    checkbox.checked = false;
                }
                if (row) {
                    row.classList.toggle("is-incompatible", Boolean(project) && !compatible);
                }
            });
            if (scopeHint) {
                scopeHint.textContent = project
                    ? "Only employees from the matching department or subdepartment can be selected for this project."
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
                    scopeHint.textContent = "Select a project before assigning employees.";
                    projectPicker.focus();
                } else {
                    scopeHint.textContent = "Select at least one compatible employee.";
                }
            }
        });

        applyScope();
    });
})();
