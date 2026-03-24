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
    const interviewAuthorityRoleContainerElement = document.getElementById("interviewAuthorityRoleIds");
    const interviewAuthorityUserContainerElement = document.getElementById("interviewAuthorityUserIds");
    const interviewAuthorityUserClientErrorElement = document.getElementById("interviewAuthorityUserIdsClientError");

    const requirementRowKeys = new Set();

    initializeExistingRows();

    designationSelectElement?.addEventListener("change", onDesignationChange);
    addRequirementButton?.addEventListener("click", onAddRequirementClick);
    requirementTableBody?.addEventListener("click", onRequirementTableClick);
    interviewAuthorityRoleContainerElement?.addEventListener("change", onInterviewAuthorityRolesChange);
    interviewAuthorityUserContainerElement?.addEventListener("change", clearInterviewAuthorityValidationError);
    formElement.addEventListener("submit", onFormSubmit);

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

    function onInterviewAuthorityRolesChange() {
        const selectedRoleIds = getCheckedValues("interviewAuthorityRoleIds");
        const retainedAuthorityIds = new Set(getCheckedValues("interviewAuthorityUserIds"));
        clearInterviewAuthorityValidationError(true);

        if (selectedRoleIds.length === 0) {
            resetInterviewAuthorityUsers();
            return;
        }

        const queryString = selectedRoleIds
            .map((roleId) => `roleIds=${encodeURIComponent(roleId)}`)
            .join("&");

        fetch(`${contextPath}/hr/internal-vacancies/interview-authorities?${queryString}`)
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Interview authority lookup failed.");
                }
                return response.json();
            })
            .then((users) => {
                populateInterviewAuthorityUsers(users, retainedAuthorityIds);
            })
            .catch((error) => {
                console.error("Unable to load interview authorities.", error);
                alert("Unable to load interview authorities for the selected roles.");
            });
    }

    function onFormSubmit(event) {
        if (hasSelectedInterviewAuthority()) {
            clearInterviewAuthorityValidationError();
            return;
        }

        event.preventDefault();
        showInterviewAuthorityValidationError();
        interviewAuthorityUserContainerElement?.scrollIntoView({
            behavior: "smooth",
            block: "center"
        });
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

    function resetInterviewAuthorityUsers() {
        if (!interviewAuthorityUserContainerElement) {
            return;
        }
        interviewAuthorityUserContainerElement.innerHTML = `
            <div class="text-muted small">
                Select HOD, PM, or STM roles first. Users with those roles will appear here.
            </div>
        `;
        clearInterviewAuthorityValidationError(true);
    }

    function populateInterviewAuthorityUsers(users, retainedAuthorityIds) {
        if (!interviewAuthorityUserContainerElement) {
            return;
        }

        interviewAuthorityUserContainerElement.innerHTML = "";
        if (!Array.isArray(users) || users.length === 0) {
            interviewAuthorityUserContainerElement.innerHTML = `
                <div class="text-muted small">
                    No users are available for the selected HOD, PM, or STM roles.
                </div>
            `;
            clearInterviewAuthorityValidationError(true);
            return;
        }

        users.forEach((user) => {
            const wrapperElement = document.createElement("div");
            wrapperElement.className = "form-check mb-2";

            const inputElement = document.createElement("input");
            inputElement.type = "checkbox";
            inputElement.className = "form-check-input";
            inputElement.name = "interviewAuthorityUserIds";
            inputElement.id = `interviewAuthorityUserIds_${user.userId}`;
            inputElement.value = user.userId;
            if (retainedAuthorityIds.has(String(user.userId))) {
                inputElement.checked = true;
            }

            const labelElement = document.createElement("label");
            labelElement.className = "form-check-label";
            labelElement.htmlFor = inputElement.id;
            labelElement.textContent = user.displayLabel;

            wrapperElement.appendChild(inputElement);
            wrapperElement.appendChild(labelElement);
            interviewAuthorityUserContainerElement.appendChild(wrapperElement);
        });

        if (hasSelectedInterviewAuthority()) {
            clearInterviewAuthorityValidationError(true);
        }
    }

    function getCheckedValues(fieldName) {
        return Array.from(document.querySelectorAll(`input[name="${fieldName}"]:checked`))
            .map((checkboxElement) => checkboxElement.value)
            .filter((value) => value);
    }

    function hasSelectedInterviewAuthority() {
        return getCheckedValues("interviewAuthorityUserIds").length > 0;
    }

    function showInterviewAuthorityValidationError() {
        interviewAuthorityUserContainerElement?.classList.add("border-danger");
        interviewAuthorityUserClientErrorElement?.classList.remove("d-none");
    }

    function clearInterviewAuthorityValidationError(forceClear) {
        if (forceClear || hasSelectedInterviewAuthority()) {
            interviewAuthorityUserContainerElement?.classList.remove("border-danger");
            interviewAuthorityUserClientErrorElement?.classList.add("d-none");
        }
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
