(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        const form = document.getElementById("internalVacancyOpeningForm");
        const tableBody = document.querySelector("#internalRequirementTable tbody");
        const typeInputs = Array.from(document.querySelectorAll(".hiring-request-type"));
        const replacementContainer = document.getElementById("replacementEmployeeContainer");
        const employeeCheckboxes = Array.from(document.querySelectorAll(".replacement-employee-checkbox"));
        const dropdownButton = document.getElementById("replacementEmployeeDropdownButton");
        const dropdownLabel = document.getElementById("replacementEmployeeDropdownLabel");
        const countBadge = document.getElementById("replacementEmployeeCountBadge");
        const dropdownMenu = document.getElementById("replacementEmployeeDropdownMenu");
        const searchInput = document.getElementById("replacementEmployeeSearch");
        const clearSearchButton = document.getElementById("replacementEmployeeClearSearch");
        const clearSelectionButton = document.getElementById("replacementEmployeeClearSelection");
        const selectedSummary = document.getElementById("replacementEmployeeSelectedSummary");
        const visibleCount = document.getElementById("replacementEmployeeVisibleCount");
        const noResults = document.getElementById("replacementEmployeeNoResults");
        const employeeOptions = Array.from(document.querySelectorAll(".replacement-employee-option"));
        const clientError = document.getElementById("replacementEmployeeClientError");
        const manualControls = document.getElementById("manualRequirementControls");

        if (!form || !tableBody || typeInputs.length === 0 || !replacementContainer) {
            return;
        }

        typeInputs.forEach((input) => input.addEventListener("change", updateRequestType));
        employeeCheckboxes.forEach((checkbox) => checkbox.addEventListener("change", function () {
            clearSelectionError();
            rebuildReplacementRequirements();
        }));
        searchInput?.addEventListener("input", filterEmployeeOptions);
        searchInput?.addEventListener("keydown", preventSearchSubmit);
        clearSearchButton?.addEventListener("click", clearEmployeeSearch);
        clearSelectionButton?.addEventListener("click", clearEmployeeSelection);
        dropdownMenu?.addEventListener("click", (event) => event.stopPropagation());
        dropdownButton?.addEventListener("shown.bs.dropdown", () => searchInput?.focus());
        form.addEventListener("submit", validateReplacementSelection);

        filterEmployeeOptions();
        updateRequestType();

        function isReplacementRequest() {
            return document.querySelector(".hiring-request-type:checked")?.value === "EMPLOYEE_REPLACEMENT";
        }

        function updateRequestType() {
            const replacement = isReplacementRequest();
            form.enctype = replacement
                    ? "application/x-www-form-urlencoded"
                    : "multipart/form-data";
            replacementContainer.classList.toggle("d-none", !replacement);
            manualControls?.classList.toggle("d-none", replacement);
            document.querySelectorAll(".requirementActionColumn").forEach((element) => {
                element.classList.toggle("d-none", replacement);
            });
            employeeCheckboxes.forEach((checkbox) => {
                checkbox.disabled = !replacement;
            });

            if (replacement) {
                rebuildReplacementRequirements();
            } else {
                clearSelectionError();
                updateDropdownLabel();
                setVacancyInputsReadonly(false);
            }
        }

        function rebuildReplacementRequirements() {
            const groupedRequirements = new Map();
            selectedEmployees().forEach((checkbox) => {
                const designationId = checkbox.dataset.designationId;
                const levelCode = checkbox.dataset.levelCode;
                if (!designationId || !levelCode) {
                    return;
                }

                const key = `${designationId}_${levelCode}`;
                const group = groupedRequirements.get(key) || {
                    key,
                    designationId,
                    designationName: checkbox.dataset.designationName || "Designation",
                    levelCode,
                    levelName: checkbox.dataset.levelName || levelCode,
                    vacancyCount: 0
                };
                group.vacancyCount += 1;
                groupedRequirements.set(key, group);
            });

            tableBody.replaceChildren();
            Array.from(groupedRequirements.values()).forEach(appendRequirementRow);
            resequenceRequirementRows();
            updateDropdownLabel();
            form.dispatchEvent(new CustomEvent("internal-vacancy-requirements-rebuilt"));
        }

        function appendRequirementRow(requirement) {
            const row = document.createElement("tr");
            row.dataset.rowKey = requirement.key;
            row.appendChild(textAndHiddenCell(
                    requirement.designationName,
                    "designationId",
                    requirement.designationId,
                    "designationName",
                    requirement.designationName));
            row.appendChild(textAndHiddenCell(
                    requirement.levelName,
                    "levelCode",
                    requirement.levelCode,
                    "levelName",
                    requirement.levelName));

            const vacancyCell = document.createElement("td");
            const vacancyInput = createInput("numberOfVacancy", requirement.vacancyCount);
            vacancyInput.type = "number";
            vacancyInput.min = "1";
            vacancyInput.readOnly = true;
            vacancyInput.className = "form-control numberOfVacancyInput";
            vacancyCell.appendChild(vacancyInput);
            row.appendChild(vacancyCell);

            if (document.querySelector("#internalRequirementTable thead th.requirementActionColumn")) {
                const actionCell = document.createElement("td");
                actionCell.className = "requirementActionColumn d-none";
                row.appendChild(actionCell);
            }
            tableBody.appendChild(row);
        }

        function textAndHiddenCell(text, firstField, firstValue, secondField, secondValue) {
            const cell = document.createElement("td");
            const label = document.createElement("span");
            label.textContent = text;
            cell.append(label, createInput(firstField, firstValue), createInput(secondField, secondValue));
            return cell;
        }

        function createInput(field, value) {
            const input = document.createElement("input");
            input.type = "hidden";
            input.dataset.field = field;
            input.value = value;
            return input;
        }

        function resequenceRequirementRows() {
            Array.from(tableBody.rows).forEach((row, index) => {
                row.querySelectorAll("[data-field]").forEach((input) => {
                    input.name = `requirements[${index}].${input.dataset.field}`;
                });
            });
        }

        function selectedEmployees() {
            return employeeCheckboxes.filter((checkbox) => checkbox.checked);
        }

        function updateDropdownLabel() {
            if (!dropdownButton) {
                return;
            }
            const count = selectedEmployees().length;
            const label = count === 0
                    ? "Select Employees"
                    : `${count} employee${count === 1 ? "" : "s"} selected`;
            if (dropdownLabel) {
                dropdownLabel.textContent = label;
            } else {
                dropdownButton.textContent = label;
            }
            if (countBadge) {
                countBadge.textContent = String(count);
                countBadge.classList.toggle("d-none", count === 0);
            }
            if (selectedSummary) {
                selectedSummary.textContent = `${count} selected`;
            }
            if (clearSelectionButton) {
                clearSelectionButton.disabled = count === 0;
            }
        }

        function filterEmployeeOptions() {
            if (!searchInput || employeeOptions.length === 0) {
                return;
            }
            const query = searchInput.value.trim().toLocaleLowerCase();
            let matches = 0;
            employeeOptions.forEach((option) => {
                const isMatch = !query || option.textContent.toLocaleLowerCase().includes(query);
                option.classList.toggle("d-none", !isMatch);
                if (isMatch) {
                    matches += 1;
                }
            });
            clearSearchButton?.classList.toggle("d-none", query.length === 0);
            noResults?.classList.toggle("d-none", matches > 0);
            if (visibleCount) {
                visibleCount.textContent = query
                        ? `${matches} of ${employeeOptions.length} employees`
                        : `${employeeOptions.length} employees available`;
            }
        }

        function clearEmployeeSearch() {
            if (!searchInput) {
                return;
            }
            searchInput.value = "";
            filterEmployeeOptions();
            searchInput.focus();
        }

        function preventSearchSubmit(event) {
            if (event.key === "Enter") {
                event.preventDefault();
            }
        }

        function clearEmployeeSelection() {
            employeeCheckboxes.forEach((checkbox) => {
                checkbox.checked = false;
            });
            clearSelectionError();
            rebuildReplacementRequirements();
        }

        function setVacancyInputsReadonly(readonly) {
            tableBody.querySelectorAll(".numberOfVacancyInput").forEach((input) => {
                input.readOnly = readonly;
            });
        }

        function validateReplacementSelection(event) {
            const action = event.submitter?.value;
            if (!isReplacementRequest() || action === "draft" || action === "reject") {
                return;
            }
            if (selectedEmployees().length > 0) {
                clearSelectionError();
                return;
            }

            event.preventDefault();
            clientError?.classList.remove("d-none");
            replacementContainer.scrollIntoView({ behavior: "smooth", block: "center" });
        }

        function clearSelectionError() {
            clientError?.classList.add("d-none");
        }
    });
})();
