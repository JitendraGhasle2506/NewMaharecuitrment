(function () {
    "use strict";

    const formElement = document.getElementById("internalVacancyOpeningForm");
    if (!formElement) {
        return;
    }

    const contextPath = document.body?.dataset?.contextPath || "";
    const designationSelectElement = document.getElementById("designationSelect");
    const levelSelectElement = document.getElementById("levelSelect");
    const requirementTableBody = document.querySelector("#internalRequirementTable tbody");
    const addRequirementButton = document.getElementById("addRequirementButton");

    const requirementRowKeys = new Set();

    initializeExistingRows();

    designationSelectElement?.addEventListener("change", onDesignationChange);
    addRequirementButton?.addEventListener("click", onAddRequirementClick);
    requirementTableBody?.addEventListener("click", onRequirementTableClick);

    function initializeExistingRows() {
        const rows = requirementTableBody?.querySelectorAll("tr") || [];
        rows.forEach((row) => {
            const rowKey = row.getAttribute("data-row-key");
            if (rowKey) {
                requirementRowKeys.add(rowKey);
            }
        });
        resequenceRequirementRows();
    }

    function onDesignationChange() {
        const designationId = designationSelectElement?.value;
        resetLevelSelect();
        if (!designationId) {
            return;
        }

        fetch(`${contextPath}/hr/internal-vacancies/by-designation/${designationId}`)
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Level lookup failed.");
                }
                return response.json();
            })
            .then((levels) => {
                levels.forEach((level) => {
                    const optionElement = document.createElement("option");
                    optionElement.value = level.levelCode;
                    optionElement.textContent = `${level.levelName} (${level.levelCode})`;
                    levelSelectElement.appendChild(optionElement);
                });
            })
            .catch((error) => {
                console.error("Unable to load levels by designation.", error);
                alert("Unable to load levels for the selected designation.");
            });
    }

    function onAddRequirementClick() {
        const designationId = designationSelectElement?.value;
        const designationName = designationSelectElement?.options[designationSelectElement.selectedIndex]?.text?.trim();
        const levelCode = levelSelectElement?.value;
        const levelName = levelSelectElement?.options[levelSelectElement.selectedIndex]?.text?.trim();

        if (!designationId || !designationName || !levelCode || !levelName) {
            alert("Please select both designation and level.");
            return;
        }

        const rowKey = `${designationId}_${levelCode}`;
        if (requirementRowKeys.has(rowKey)) {
            alert("This designation and level combination is already added.");
            return;
        }

        appendRequirementRow({
            rowKey,
            designationId,
            designationName,
            levelCode,
            levelName
        });

        requirementRowKeys.add(rowKey);
        resequenceRequirementRows();
        resetRequirementSelectors();
    }

    function appendRequirementRow(requirementData) {
        const rowElement = document.createElement("tr");
        rowElement.setAttribute("data-row-key", requirementData.rowKey);

        rowElement.innerHTML = `
            <td>
                <span>${escapeHtml(requirementData.designationName)}</span>
                <input type="hidden" data-field="designationId" value="${requirementData.designationId}">
                <input type="hidden" data-field="designationName" value="${escapeHtml(requirementData.designationName)}">
            </td>
            <td>
                <span>${escapeHtml(requirementData.levelName)}</span>
                <input type="hidden" data-field="levelCode" value="${escapeHtml(requirementData.levelCode)}">
                <input type="hidden" data-field="levelName" value="${escapeHtml(requirementData.levelName)}">
            </td>
            <td>
                <input type="number" min="1" class="form-control numberOfVacancyInput" data-field="numberOfVacancy" value="1">
            </td>
            <td>
                <button type="button" class="btn btn-outline-danger btn-sm removeRequirementButton">Remove</button>
            </td>
        `;

        requirementTableBody.appendChild(rowElement);
    }

    function onRequirementTableClick(event) {
        const removeButton = event.target.closest(".removeRequirementButton");
        if (!removeButton) {
            return;
        }

        const rowElement = removeButton.closest("tr");
        if (!rowElement) {
            return;
        }

        const rowKey = rowElement.getAttribute("data-row-key");
        if (rowKey) {
            requirementRowKeys.delete(rowKey);
        }

        rowElement.remove();
        resequenceRequirementRows();
    }

    function resequenceRequirementRows() {
        const rows = requirementTableBody?.querySelectorAll("tr") || [];
        rows.forEach((row, index) => {
            row.querySelectorAll("[data-field]").forEach((inputElement) => {
                const fieldName = inputElement.getAttribute("data-field");
                inputElement.name = `requirements[${index}].${fieldName}`;
            });
        });
    }

    function resetLevelSelect() {
        if (!levelSelectElement) {
            return;
        }
        levelSelectElement.innerHTML = '<option value="">Select Level</option>';
    }

    function resetRequirementSelectors() {
        if (designationSelectElement) {
            designationSelectElement.value = "";
        }
        resetLevelSelect();
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/\"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }
})();
