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
    
    // Pagination & Search Elements
    const authoritySearchContainer = document.getElementById("interviewAuthoritySearchContainer");
    const authoritySearchInput = document.getElementById("authoritySearchInput");
    const authoritySearchBtn = document.getElementById("authoritySearchBtn");
    const authorityClearBtn = document.getElementById("authorityClearBtn");
    const authorityPagination = document.getElementById("interviewAuthorityPagination");
    const authorityPageInfo = document.getElementById("authorityPageInfo");
    const authorityPrevPage = document.getElementById("authorityPrevPage");
    const authorityNextPage = document.getElementById("authorityNextPage");
    const hiddenAuthoritiesContainer = document.getElementById("selectedAuthoritiesHiddenContainer");

    const requirementRowKeys = new Set();
    
    // Selection State
    let selectedUserIds = new Set();
    let selectedEmployeeIds = new Set();
    let currentAuthorityPage = 0;
    let authorityPageSize = 10;
    let currentSearchQuery = "";

    initializeExistingRows();
    initializeExistingSelections();

    designationSelectElement?.addEventListener("change", onDesignationChange);
    addRequirementButton?.addEventListener("click", onAddRequirementClick);
    requirementTableBody?.addEventListener("click", onRequirementTableClick);
    interviewAuthorityRoleContainerElement?.addEventListener("change", onInterviewAuthorityRolesChange);
    interviewAuthorityUserContainerElement?.addEventListener("change", onAuthorityCheckboxChange);
    
    authoritySearchBtn?.addEventListener("click", onAuthoritySearch);
    authoritySearchInput?.addEventListener("keypress", (e) => e.key === "Enter" && (e.preventDefault(), onAuthoritySearch()));
    authorityClearBtn?.addEventListener("click", onAuthorityClear);
    authorityPrevPage?.addEventListener("click", () => changeAuthorityPage(-1));
    authorityNextPage?.addEventListener("click", () => changeAuthorityPage(1));
    
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

    function initializeExistingSelections() {
        // Initial state from pre-rendered HTML
        const userCheckboxes = document.querySelectorAll('input[name="interviewAuthorityUserIds"]:checked');
        const employeeCheckboxes = document.querySelectorAll('input[name="interviewAuthorityEmployeeIds"]:checked');
        
        userCheckboxes.forEach(cb => selectedUserIds.add(String(cb.value)));
        employeeCheckboxes.forEach(cb => selectedEmployeeIds.add(String(cb.value)));
        
        const selectedRoles = getCheckedValues("interviewAuthorityRoleIds");
        if (selectedRoles.length > 0) {
            authoritySearchContainer?.classList.remove("d-none");
            loadInterviewAuthorities(); // Initial load for current page
        }
        
        updateHiddenAuthorityInputs();
    }

    function onAuthorityCheckboxChange(event) {
        const checkbox = event.target;
        if (!checkbox || checkbox.type !== "checkbox") return;
        
        const id = String(checkbox.value);
        if (checkbox.name === "interviewAuthorityUserIds") {
            checkbox.checked ? selectedUserIds.add(id) : selectedUserIds.delete(id);
        } else if (checkbox.name === "interviewAuthorityEmployeeIds") {
            checkbox.checked ? selectedEmployeeIds.add(id) : selectedEmployeeIds.delete(id);
        }
        
        updateHiddenAuthorityInputs();
        clearInterviewAuthorityValidationError();
    }

    function updateHiddenAuthorityInputs() {
        if (!hiddenAuthoritiesContainer) return;
        
        hiddenAuthoritiesContainer.innerHTML = "";
        
        // Find IDs that are currently visible as checked checkboxes
        const visibleUserIds = new Set(
            Array.from(interviewAuthorityUserContainerElement?.querySelectorAll('input[name="interviewAuthorityUserIds"]:checked') || [])
                 .map(cb => String(cb.value))
        );
        const visibleEmployeeIds = new Set(
            Array.from(interviewAuthorityUserContainerElement?.querySelectorAll('input[name="interviewAuthorityEmployeeIds"]:checked') || [])
                 .map(cb => String(cb.value))
        );
        
        // Add hidden inputs for selected IDs that are NOT visible
        selectedUserIds.forEach(id => {
            if (!visibleUserIds.has(id)) {
                const input = document.createElement("input");
                input.type = "hidden";
                input.name = "interviewAuthorityUserIds";
                input.value = id;
                hiddenAuthoritiesContainer.appendChild(input);
            }
        });
        
        selectedEmployeeIds.forEach(id => {
            if (!visibleEmployeeIds.has(id)) {
                const input = document.createElement("input");
                input.type = "hidden";
                input.name = "interviewAuthorityEmployeeIds";
                input.value = id;
                hiddenAuthoritiesContainer.appendChild(input);
            }
        });
    }

    function onAuthoritySearch() {
        currentSearchQuery = authoritySearchInput?.value?.trim() || "";
        currentAuthorityPage = 0;
        loadInterviewAuthorities();
    }

    function onAuthorityClear() {
        if (authoritySearchInput) authoritySearchInput.value = "";
        currentSearchQuery = "";
        currentAuthorityPage = 0;
        loadInterviewAuthorities();
    }

    function changeAuthorityPage(delta) {
        currentAuthorityPage += delta;
        loadInterviewAuthorities();
    }

    function onInterviewAuthorityRolesChange() {
        currentAuthorityPage = 0;
        currentSearchQuery = "";
        if (authoritySearchInput) authoritySearchInput.value = "";
        
        const selectedRoleIds = getCheckedValues("interviewAuthorityRoleIds");
        if (selectedRoleIds.length === 0) {
            authoritySearchContainer?.classList.add("d-none");
            authorityPagination?.classList.add("d-none");
            resetInterviewAuthorityUsers();
            return;
        }
        
        authoritySearchContainer?.classList.remove("d-none");
        loadInterviewAuthorities();
    }

    function loadInterviewAuthorities() {
        const selectedRoleIds = getCheckedValues("interviewAuthorityRoleIds");
        if (selectedRoleIds.length === 0) return;

        const params = new URLSearchParams();
        selectedRoleIds.forEach(id => params.append("roleIds", id));
        if (currentSearchQuery) params.append("search", currentSearchQuery);
        params.append("page", currentAuthorityPage);
        params.append("size", authorityPageSize);

        if (interviewAuthorityUserContainerElement) {
            interviewAuthorityUserContainerElement.innerHTML = `
                <div class="text-center p-3">
                    <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
                    <span class="ms-2 small text-muted">Loading...</span>
                </div>
            `;
        }

        fetch(`${contextPath}/hr/internal-vacancies/interview-authorities?${params.toString()}`)
            .then(res => {
                if (!res.ok) throw new Error("Fetch failed");
                return res.json();
            })
            .then(pageData => {
                renderAuthoritiesPage(pageData);
            })
            .catch(err => {
                console.error(err);
                if (interviewAuthorityUserContainerElement) {
                    interviewAuthorityUserContainerElement.innerHTML = `<div class="text-danger small p-2">Error loading authorities.</div>`;
                }
            });
    }

    function renderAuthoritiesPage(pageData) {
        if (!interviewAuthorityUserContainerElement) return;
        
        const authorities = pageData.content || [];
        interviewAuthorityUserContainerElement.innerHTML = "";
        
        if (authorities.length === 0) {
            interviewAuthorityUserContainerElement.innerHTML = `<div class="text-muted small p-2">No results found.</div>`;
            authorityPagination?.classList.add("d-none");
            return;
        }

        authorities.forEach(user => {
            const wrapper = document.createElement("div");
            wrapper.className = "form-check mb-2";
            
            const input = document.createElement("input");
            input.type = "checkbox";
            input.className = "form-check-input";
            input.name = user.type === "EMPLOYEE" ? "interviewAuthorityEmployeeIds" : "interviewAuthorityUserIds";
            input.value = user.userId;
            input.id = `auth_${user.type}_${user.userId}`;
            
            const isSelected = user.type === "EMPLOYEE" 
                ? selectedEmployeeIds.has(String(user.userId))
                : selectedUserIds.has(String(user.userId));
            
            if (isSelected) input.checked = true;
            
            const label = document.createElement("label");
            label.className = "form-check-label";
            label.htmlFor = input.id;
            label.textContent = user.displayLabel;
            
            wrapper.appendChild(input);
            wrapper.appendChild(label);
            interviewAuthorityUserContainerElement.appendChild(wrapper);
        });

        // Update Pagination Info
        const totalPages = pageData.totalPages || 0;
        const totalElements = pageData.totalElements || 0;
        
        if (totalPages > 1) {
            authorityPagination?.classList.remove("d-none");
            if (authorityPageInfo) authorityPageInfo.textContent = `Page ${pageData.number + 1} of ${totalPages} (${totalElements} total)`;
            
            authorityPrevPage?.classList.toggle("disabled", pageData.first);
            authorityNextPage?.classList.toggle("disabled", pageData.last);
        } else {
            authorityPagination?.classList.add("d-none");
        }
        
        // Sync hidden inputs after rendering new page
        updateHiddenAuthorityInputs();
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
            <div class="text-muted small" id="noAuthorityPlaceholder">
                Select roles first. Users or Employees with those roles will appear here.
            </div>
        `;
        clearInterviewAuthorityValidationError(true);
    }

    function getCheckedValues(fieldName) {
        return Array.from(document.querySelectorAll(`input[name="${fieldName}"]:checked`))
            .map((checkboxElement) => checkboxElement.value)
            .filter((value) => value);
    }

    function hasSelectedInterviewAuthority() {
        return selectedUserIds.size > 0 || selectedEmployeeIds.size > 0;
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
