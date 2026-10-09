(function () {
    "use strict";

    const formElement = document.getElementById("internalVacancyOpeningForm");
    if (!formElement) {
        return;
    }

    const contextPath = document.body?.dataset?.contextPath || "";
    const designationSelectElement = document.getElementById("designationSelect");
    const levelSelectElement = document.getElementById("levelSelect");
    const projectSelectElement = document.getElementById("projectId");
    const approvalDocumentElement = document.getElementById("eOfficeApprovalDocument");
    const approvalDocumentClientError = document.getElementById("eOfficeApprovalClientError");
    const remarksElement = document.getElementById("remarks");
    const remarksCharacterCount = document.getElementById("remarksCharacterCount");
    const validationSummary = document.getElementById("formValidationSummary");
    const validationSummaryList = document.getElementById("formValidationSummaryList");
    const requirementTableBody = document.querySelector("#internalRequirementTable tbody");
    const addRequirementButton = document.getElementById("addRequirementButton");
    const requirementsClientError = document.getElementById("requirementsClientError");
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
    const levelOneAuthoritySelectionCount = document.getElementById("levelOneAuthoritySelectionCount");
    const levelOneAuthoritySection = document.getElementById("levelOneAuthoritySection");
    const levelOneAuthorityLocked = levelOneAuthoritySection?.dataset?.locked === "true";

    const levelTwoAuthoritySection = document.getElementById("levelTwoAuthoritySection");
    const levelTwoAuthorityLocked = levelTwoAuthoritySection?.dataset?.locked === "true";
    const levelTwoAuthorityContainer = document.getElementById("levelTwoInterviewAuthorityUserIds");
    const levelTwoAuthoritySearchInput = document.getElementById("levelTwoAuthoritySearchInput");
    const levelTwoAuthoritySearchBtn = document.getElementById("levelTwoAuthoritySearchBtn");
    const levelTwoAuthorityClearBtn = document.getElementById("levelTwoAuthorityClearBtn");
    const levelTwoAuthorityPagination = document.getElementById("levelTwoAuthorityPagination");
    const levelTwoAuthorityPageInfo = document.getElementById("levelTwoAuthorityPageInfo");
    const levelTwoAuthorityPrevPage = document.getElementById("levelTwoAuthorityPrevPage");
    const levelTwoAuthorityNextPage = document.getElementById("levelTwoAuthorityNextPage");
    const levelTwoAuthorityClientError = document.getElementById("levelTwoInterviewAuthorityUserIdsClientError");
    const hiddenLevelTwoAuthoritiesContainer = document.getElementById("selectedLevelTwoAuthoritiesHiddenContainer");
    const levelTwoAuthoritySelectionCount = document.getElementById("levelTwoAuthoritySelectionCount");

    const requirementRowKeys = new Set();
    
    // Selection State
    let selectedUserIds = new Set();
    let selectedEmployeeIds = new Set();
    let currentAuthorityPage = 0;
    let authorityPageSize = 10;
    let currentSearchQuery = "";
    let selectedLevelTwoUserIds = new Set();
    let currentLevelTwoAuthorityPage = 0;
    let currentLevelTwoSearchQuery = "";

    initializeExistingRows();
    initializeExistingSelections();

    designationSelectElement?.addEventListener("change", onDesignationChange);
    projectSelectElement?.addEventListener("change", () => clearControlValidation(projectSelectElement));
    approvalDocumentElement?.addEventListener("change", clearApprovalDocumentValidation);
    remarksElement?.addEventListener("input", updateRemarksCharacterCount);
    document.querySelectorAll(".hiring-request-type").forEach(input => input.addEventListener("change", () => {
        document.querySelectorAll(".hiring-request-type").forEach(option => option.classList.remove("is-invalid"));
        clearApprovalDocumentValidation();
    }));
    addRequirementButton?.addEventListener("click", onAddRequirementClick);
    requirementTableBody?.addEventListener("click", onRequirementTableClick);
    requirementTableBody?.addEventListener("input", onRequirementInput);
    interviewAuthorityRoleContainerElement?.addEventListener("change", onInterviewAuthorityRolesChange);
    interviewAuthorityUserContainerElement?.addEventListener("change", onAuthorityCheckboxChange);
    
    authoritySearchBtn?.addEventListener("click", onAuthoritySearch);
    authoritySearchInput?.addEventListener("keypress", (e) => e.key === "Enter" && (e.preventDefault(), onAuthoritySearch()));
    authorityClearBtn?.addEventListener("click", onAuthorityClear);
    authorityPrevPage?.addEventListener("click", () => changeAuthorityPage(-1));
    authorityNextPage?.addEventListener("click", () => changeAuthorityPage(1));
    levelTwoAuthorityContainer?.addEventListener("change", onLevelTwoAuthorityCheckboxChange);
    levelTwoAuthoritySearchBtn?.addEventListener("click", onLevelTwoAuthoritySearch);
    levelTwoAuthoritySearchInput?.addEventListener("keypress", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            onLevelTwoAuthoritySearch();
        }
    });
    levelTwoAuthorityClearBtn?.addEventListener("click", onLevelTwoAuthorityClear);
    levelTwoAuthorityPrevPage?.addEventListener("click", () => changeLevelTwoAuthorityPage(-1));
    levelTwoAuthorityNextPage?.addEventListener("click", () => changeLevelTwoAuthorityPage(1));
    
    formElement.addEventListener("submit", onFormSubmit);
    formElement.addEventListener("internal-vacancy-requirements-rebuilt", function () {
        requirementRowKeys.clear();
        initializeExistingRows();
        clearRequirementsValidation();
    });
    updateRemarksCharacterCount();

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
        document.querySelectorAll('input[name="levelTwoInterviewAuthorityUserIds"]:checked')
            .forEach(checkbox => selectedLevelTwoUserIds.add(String(checkbox.value)));
        
        const selectedRoles = getCheckedValues("interviewAuthorityRoleIds");
        if (selectedRoles.length > 0 && !levelOneAuthorityLocked) {
            authoritySearchContainer?.classList.remove("d-none");
            loadInterviewAuthorities(); // Initial load for current page
        }
        
        updateHiddenAuthorityInputs();
        updateHiddenLevelTwoAuthorityInputs();
        updateAuthoritySelectionCounts();
        if (levelTwoAuthorityContainer && !levelTwoAuthorityLocked) {
            loadLevelTwoInterviewAuthorities();
        }
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
        updateAuthoritySelectionCounts();
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

    function onLevelTwoAuthorityCheckboxChange(event) {
        const checkbox = event.target;
        if (!checkbox || checkbox.type !== "checkbox") return;

        const id = String(checkbox.value);
        if (checkbox.checked && !selectedLevelTwoUserIds.has(id) && selectedLevelTwoUserIds.size >= 5) {
            checkbox.checked = false;
            showLevelTwoAuthorityValidationError("A maximum of five Round L2 interview authorities can be selected.");
            return;
        }
        checkbox.checked ? selectedLevelTwoUserIds.add(id) : selectedLevelTwoUserIds.delete(id);
        updateHiddenLevelTwoAuthorityInputs();
        updateAuthoritySelectionCounts();
        clearLevelTwoAuthorityValidationError();
    }

    function updateHiddenLevelTwoAuthorityInputs() {
        if (!hiddenLevelTwoAuthoritiesContainer || levelTwoAuthorityLocked) return;

        hiddenLevelTwoAuthoritiesContainer.innerHTML = "";
        const visibleIds = new Set(
            Array.from(levelTwoAuthorityContainer?.querySelectorAll(
                'input[name="levelTwoInterviewAuthorityUserIds"]:checked') || [])
                .map(checkbox => String(checkbox.value))
        );
        selectedLevelTwoUserIds.forEach(id => {
            if (visibleIds.has(id)) return;
            const input = document.createElement("input");
            input.type = "hidden";
            input.name = "levelTwoInterviewAuthorityUserIds";
            input.value = id;
            hiddenLevelTwoAuthoritiesContainer.appendChild(input);
        });
    }

    function updateAuthoritySelectionCounts() {
        const levelOneCount = selectedUserIds.size + selectedEmployeeIds.size;
        if (levelOneAuthoritySelectionCount) {
            levelOneAuthoritySelectionCount.textContent = `${levelOneCount} selected`;
            levelOneAuthoritySelectionCount.classList.toggle("is-complete", levelOneCount > 0);
        }
        if (levelTwoAuthoritySelectionCount) {
            levelTwoAuthoritySelectionCount.textContent = `${selectedLevelTwoUserIds.size} / 5 selected`;
            levelTwoAuthoritySelectionCount.classList.toggle(
                "is-complete",
                selectedLevelTwoUserIds.size >= 2 && selectedLevelTwoUserIds.size <= 5
            );
            levelTwoAuthoritySelectionCount.classList.toggle(
                "is-over-limit",
                selectedLevelTwoUserIds.size > 5
            );
        }
    }

    function onLevelTwoAuthoritySearch() {
        currentLevelTwoSearchQuery = levelTwoAuthoritySearchInput?.value?.trim() || "";
        currentLevelTwoAuthorityPage = 0;
        loadLevelTwoInterviewAuthorities();
    }

    function onLevelTwoAuthorityClear() {
        if (levelTwoAuthoritySearchInput) levelTwoAuthoritySearchInput.value = "";
        currentLevelTwoSearchQuery = "";
        currentLevelTwoAuthorityPage = 0;
        loadLevelTwoInterviewAuthorities();
    }

    function changeLevelTwoAuthorityPage(delta) {
        currentLevelTwoAuthorityPage = Math.max(0, currentLevelTwoAuthorityPage + delta);
        loadLevelTwoInterviewAuthorities();
    }

    function loadLevelTwoInterviewAuthorities() {
        if (!levelTwoAuthorityContainer || levelTwoAuthorityLocked) return;

        const params = new URLSearchParams({
            page: String(currentLevelTwoAuthorityPage),
            size: String(authorityPageSize)
        });
        if (currentLevelTwoSearchQuery) params.append("search", currentLevelTwoSearchQuery);

        levelTwoAuthorityContainer.innerHTML = `
            <div class="text-center p-3">
                <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
                <span class="ms-2 small text-muted">Loading...</span>
            </div>
        `;

        fetch(`${contextPath}/hr/internal-vacancies/level-two-interview-authorities?${params.toString()}`)
            .then(response => {
                if (!response.ok) throw new Error("Fetch failed");
                return response.json();
            })
            .then(renderLevelTwoAuthoritiesPage)
            .catch(error => {
                console.error(error);
                levelTwoAuthorityContainer.innerHTML =
                    '<div class="text-danger small p-2">Error loading Round L2 authorities.</div>';
            });
    }

    function renderLevelTwoAuthoritiesPage(pageData) {
        const authorities = pageData.content || [];
        levelTwoAuthorityContainer.innerHTML = "";

        if (authorities.length === 0) {
            levelTwoAuthorityContainer.innerHTML =
                '<div class="text-muted small p-2">No eligible Round L2 authorities found.</div>';
            levelTwoAuthorityPagination?.classList.add("d-none");
            updateHiddenLevelTwoAuthorityInputs();
            return;
        }

        authorities.forEach(authority => {
            const wrapper = document.createElement("div");
            wrapper.className = "form-check mb-2";

            const input = document.createElement("input");
            input.type = "checkbox";
            input.className = "form-check-input";
            input.name = "levelTwoInterviewAuthorityUserIds";
            input.value = authority.userId;
            input.id = `levelTwoAuthority_${authority.userId}`;
            input.checked = selectedLevelTwoUserIds.has(String(authority.userId));

            const label = document.createElement("label");
            label.className = "form-check-label";
            label.htmlFor = input.id;
            label.textContent = authority.displayLabel;

            wrapper.appendChild(input);
            wrapper.appendChild(label);
            levelTwoAuthorityContainer.appendChild(wrapper);
        });

        const totalPages = pageData.totalPages || 0;
        if (totalPages > 1) {
            levelTwoAuthorityPagination?.classList.remove("d-none");
            if (levelTwoAuthorityPageInfo) {
                levelTwoAuthorityPageInfo.textContent =
                    `Page ${pageData.number + 1} of ${totalPages} (${pageData.totalElements || 0} total)`;
            }
            levelTwoAuthorityPrevPage?.classList.toggle("disabled", pageData.first);
            levelTwoAuthorityNextPage?.classList.toggle("disabled", pageData.last);
        } else {
            levelTwoAuthorityPagination?.classList.add("d-none");
        }
        updateHiddenLevelTwoAuthorityInputs();
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
        if (levelOneAuthorityLocked) return;
        currentAuthorityPage = 0;
        currentSearchQuery = "";
        if (authoritySearchInput) authoritySearchInput.value = "";
        
        const selectedRoleIds = getCheckedValues("interviewAuthorityRoleIds");
        if (selectedRoleIds.length === 0) {
            selectedUserIds.clear();
            selectedEmployeeIds.clear();
            updateHiddenAuthorityInputs();
            updateAuthoritySelectionCounts();
            authoritySearchContainer?.classList.add("d-none");
            authorityPagination?.classList.add("d-none");
            resetInterviewAuthorityUsers();
            return;
        }
        
        authoritySearchContainer?.classList.remove("d-none");
        loadInterviewAuthorities();
    }

    function loadInterviewAuthorities() {
        if (levelOneAuthorityLocked) return;
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
            designationSelectElement?.classList.toggle("is-invalid", !designationId);
            levelSelectElement?.classList.toggle("is-invalid", !levelCode);
            showRequirementsValidation("Select both designation and level before adding a requirement.");
            return;
        }

        const rowKey = `${designationId}_${levelCode}`;
        if (requirementRowKeys.has(rowKey)) {
            showRequirementsValidation("This designation and level combination is already added.");
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
        clearRequirementsValidation();
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
                <input type="number" min="1" step="1" required inputmode="numeric" aria-label="Number of vacancies"
                    class="form-control numberOfVacancyInput" data-field="numberOfVacancy" value="1">
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
        if (requirementTableBody?.rows.length) clearRequirementsValidation();
    }

    function onFormSubmit(event) {
        const action = event.submitter?.value;
        if (["draft", "reject"].includes(action)) {
            return;
        }

        const errors = [];
        let firstInvalidElement = null;
        const addError = (message, element) => {
            errors.push(message);
            firstInvalidElement ||= element;
        };

        clearClientValidation();

        if (!projectSelectElement?.value) {
            projectSelectElement?.classList.add("is-invalid");
            addError("Select an internal project.", projectSelectElement);
        }

        const requestType = document.querySelector('.hiring-request-type:checked')?.value;
        if (!requestType) {
            document.querySelectorAll(".hiring-request-type").forEach(input => input.classList.add("is-invalid"));
            addError("Select a hiring request type.", document.querySelector(".hiring-request-type"));
        }

        const approvalFileError = getApprovalDocumentError(requestType);
        if (approvalFileError) {
            showApprovalDocumentValidation(approvalFileError);
            addError(approvalFileError, approvalDocumentElement);
        }

        if (requestType === "EMPLOYEE_REPLACEMENT"
                && !document.querySelector(".replacement-employee-checkbox:checked")) {
            showElement(document.getElementById("replacementEmployeeClientError"));
            document.getElementById("replacementEmployeeContainer")?.classList.add("validation-group-invalid");
            addError("Select at least one employee to be replaced.", document.getElementById("replacementEmployeeDropdownButton"));
        }

        const rows = Array.from(requirementTableBody?.rows || []);
        if (rows.length === 0) {
            showRequirementsValidation("Add at least one designation and level requirement.");
            addError("Add at least one designation and level requirement.", designationSelectElement || requirementTableBody);
        } else {
            rows.forEach((row, index) => {
                const vacancyInput = row.querySelector(".numberOfVacancyInput");
                const vacancyCount = Number(vacancyInput?.value);
                if (!Number.isSafeInteger(vacancyCount) || vacancyCount < 1) {
                    vacancyInput?.classList.add("is-invalid");
                    addError(`Enter a whole vacancy count of at least 1 in requirement ${index + 1}.`, vacancyInput);
                }
            });
        }

        if ((remarksElement?.value?.length || 0) > 1000) {
            remarksElement.classList.add("is-invalid");
            addError("Remarks must not exceed 1000 characters.", remarksElement);
        }

        const requiresAuthorities = ["submit", "approve", "activate"].includes(action);
        if (requiresAuthorities && !hasSelectedInterviewAuthority()) {
            showInterviewAuthorityValidationError();
            addError("Select at least one Round L1 interview authority.", interviewAuthorityUserContainerElement);
        }
        if (requiresAuthorities
                && (selectedLevelTwoUserIds.size < 2 || selectedLevelTwoUserIds.size > 5)) {
            const message = selectedLevelTwoUserIds.size > 5
                ? "A maximum of five Round L2 interview authorities can be selected."
                : "Select at least two Round L2 interview authorities.";
            showLevelTwoAuthorityValidationError(message);
            addError(message, levelTwoAuthorityContainer);
        }

        if (errors.length === 0) return;

        event.preventDefault();
        renderValidationSummary(errors);
        firstInvalidElement?.scrollIntoView({ behavior: "smooth", block: "center" });
        firstInvalidElement?.focus?.({ preventScroll: true });
    }

    function getApprovalDocumentError(requestType) {
        const file = approvalDocumentElement?.files?.[0];
        const hasExistingApproval = Boolean(
            formElement.querySelector('[name="existingEOfficeApprovalFileName"]')?.value?.trim()
        );
        if (requestType === "NEW_CANDIDATE" && !file && !hasExistingApproval) {
            return "Upload the e-office approval document in PDF format.";
        }
        if (!file) return "";
        if (!file.name.toLocaleLowerCase().endsWith(".pdf")) {
            return "Upload the e-office approval document in PDF format only.";
        }
        if (file.size > 2 * 1024 * 1024) {
            return "E-office approval document must not exceed 2 MB.";
        }
        return "";
    }

    function renderValidationSummary(errors) {
        if (!validationSummary || !validationSummaryList) return;
        validationSummaryList.replaceChildren(...errors.map(message => {
            const item = document.createElement("li");
            item.textContent = message;
            return item;
        }));
        validationSummary.classList.remove("d-none");
        validationSummary.focus({ preventScroll: true });
    }

    function clearClientValidation() {
        validationSummary?.classList.add("d-none");
        validationSummaryList?.replaceChildren();
        formElement.querySelectorAll(".is-invalid").forEach(element => element.classList.remove("is-invalid"));
        document.getElementById("replacementEmployeeContainer")?.classList.remove("validation-group-invalid");
        hideElement(document.getElementById("replacementEmployeeClientError"));
        clearApprovalDocumentValidation();
        clearRequirementsValidation();
        clearInterviewAuthorityValidationError(true);
        clearLevelTwoAuthorityValidationError(true);
    }

    function updateRemarksCharacterCount() {
        if (remarksCharacterCount) remarksCharacterCount.textContent = String(remarksElement?.value?.length || 0);
        if ((remarksElement?.value?.length || 0) <= 1000) clearControlValidation(remarksElement);
    }

    function onRequirementInput(event) {
        if (!event.target.classList.contains("numberOfVacancyInput")) return;
        const value = Number(event.target.value);
        if (Number.isSafeInteger(value) && value >= 1) event.target.classList.remove("is-invalid");
    }

    function showApprovalDocumentValidation(message) {
        approvalDocumentElement?.classList.add("is-invalid");
        if (approvalDocumentClientError) approvalDocumentClientError.textContent = message;
        showElement(approvalDocumentClientError);
    }

    function clearApprovalDocumentValidation() {
        clearControlValidation(approvalDocumentElement);
        hideElement(approvalDocumentClientError);
    }

    function showRequirementsValidation(message) {
        if (requirementsClientError) requirementsClientError.textContent = message;
        showElement(requirementsClientError);
    }

    function clearRequirementsValidation() {
        designationSelectElement?.classList.remove("is-invalid");
        levelSelectElement?.classList.remove("is-invalid");
        hideElement(requirementsClientError);
    }

    function clearControlValidation(element) {
        element?.classList.remove("is-invalid");
    }

    function showElement(element) {
        element?.classList.remove("d-none");
        element?.classList.add("d-block");
    }

    function hideElement(element) {
        element?.classList.add("d-none");
        element?.classList.remove("d-block");
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
        showElement(interviewAuthorityUserClientErrorElement);
    }

    function clearInterviewAuthorityValidationError(forceClear) {
        if (forceClear || hasSelectedInterviewAuthority()) {
            interviewAuthorityUserContainerElement?.classList.remove("border-danger");
            hideElement(interviewAuthorityUserClientErrorElement);
        }
    }

    function showLevelTwoAuthorityValidationError(message) {
        levelTwoAuthorityContainer?.classList.add("border-danger");
        if (message && levelTwoAuthorityClientError) levelTwoAuthorityClientError.textContent = message;
        showElement(levelTwoAuthorityClientError);
    }

    function clearLevelTwoAuthorityValidationError(forceClear) {
        if (forceClear || (selectedLevelTwoUserIds.size >= 2 && selectedLevelTwoUserIds.size <= 5)) {
            levelTwoAuthorityContainer?.classList.remove("border-danger");
            hideElement(levelTwoAuthorityClientError);
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
