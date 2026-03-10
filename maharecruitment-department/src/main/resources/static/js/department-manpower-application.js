(function () {
    "use strict";

    const MINIMUM_DURATION_IN_MONTHS = 3;

    const formElement = document.getElementById("departmentManpowerApplicationForm");
    if (!formElement) {
        return;
    }

    const contextPath = document.body?.dataset?.contextPath || "";
    const designationSelectElement = document.getElementById("designationSelect");
    const levelSelectElement = document.getElementById("levelSelect");
    const resourceRequirementTableBody = document.querySelector("#resourceRequirementTable tbody");
    const addRequirementButton = document.getElementById("addRequirementButton");
    const saveDraftButton = document.getElementById("saveDraftButton");
    const previewSubmitButton = document.getElementById("previewSubmitButton");
    const confirmSubmitButton = document.getElementById("confirmSubmitButton");
    const hiddenSubmitButton = document.getElementById("hiddenFormSubmitButton");
    const actionStatusInput = document.getElementById("applicationActionStatus");
    const grandTotalCostText = document.getElementById("grandTotalCostText");
    const grandTotalCostValue = document.getElementById("grandTotalCostValue");
    const taxBreakupBody = document.getElementById("taxBreakupBody");
    const totalTaxAmountText = document.getElementById("totalTaxAmountText");
    const grandTotalIncludingTaxText = document.getElementById("grandTotalIncludingTaxText");
    const workOrderFileInput = document.getElementById("workOrderFileInput");
    const workOrderValidationMessage = document.getElementById("workOrderValidationMessage");
    const existingWorkOrderFilePathInput = formElement.querySelector('[name="existingWorkOrderFilePath"]');
    const selectedWorkOrderPreview = document.getElementById("selectedWorkOrderPreview");
    const selectedWorkOrderLink = document.getElementById("selectedWorkOrderLink");
    const selectedWorkOrderName = document.getElementById("selectedWorkOrderName");
    const previewGrandTotalCost = document.getElementById("previewGrandTotalCost");
    const previewTaxBreakupBody = document.getElementById("previewTaxBreakupBody");
    const previewTotalTaxAmount = document.getElementById("previewTotalTaxAmount");
    const previewGrandTotalIncludingTax = document.getElementById("previewGrandTotalIncludingTax");

    const requirementRowKeys = new Set();
    let applicableTaxRates = [];
    let selectedWorkOrderObjectUrl = null;

    initializeExistingRows();
    loadApplicableTaxRates();
    recalculateGrandTotalCost();

    designationSelectElement?.addEventListener("change", onDesignationChange);
    addRequirementButton?.addEventListener("click", onAddRequirementClick);
    resourceRequirementTableBody?.addEventListener("input", onResourceTableInput);
    resourceRequirementTableBody?.addEventListener("click", onResourceTableClick);
    workOrderFileInput?.addEventListener("change", onWorkOrderFileChange);
    saveDraftButton?.addEventListener("click", onSaveDraftClick);
    previewSubmitButton?.addEventListener("click", onPreviewSubmitClick);
    confirmSubmitButton?.addEventListener("click", onConfirmSubmitClick);
    window.addEventListener("beforeunload", clearSelectedWorkOrderPreview);

    function initializeExistingRows() {
        const rows = resourceRequirementTableBody?.querySelectorAll("tr") || [];
        rows.forEach((row) => {
            const rowKey = row.getAttribute("data-row-key");
            if (rowKey) {
                requirementRowKeys.add(rowKey);
            }
            recalculateRowTotalCost(row);
        });
        resequenceRequirementRows();
    }

    function onDesignationChange() {
        const designationId = designationSelectElement.value;
        resetLevelSelect();
        if (!designationId) {
            return;
        }

        const levelEndpoint = `${contextPath}/department/manpower/by-designation/${designationId}`;
        fetch(levelEndpoint)
            .then((response) => response.json())
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
            });
    }

    function loadApplicableTaxRates() {
        const taxRateEndpoint = `${contextPath}/department/manpower/tax-rates`;
        fetch(taxRateEndpoint)
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Tax-rate lookup failed.");
                }
                return response.json();
            })
            .then((taxRates) => {
                applicableTaxRates = normalizeTaxRates(taxRates);
                recalculateGrandTotalCost();
            })
            .catch((error) => {
                console.error("Unable to load applicable tax rates.", error);
                applicableTaxRates = [];
                recalculateGrandTotalCost();
            });
    }

    function onAddRequirementClick() {
        const designationId = designationSelectElement.value;
        const designationName = designationSelectElement.options[designationSelectElement.selectedIndex]?.text?.trim();
        const levelCode = levelSelectElement.value;
        const levelName = levelSelectElement.options[levelSelectElement.selectedIndex]?.text?.trim();

        if (!designationId || !designationName || !levelCode || !levelName) {
            alert("Please select both designation and level.");
            return;
        }

        const rowKey = `${designationId}_${levelCode}`;
        if (requirementRowKeys.has(rowKey)) {
            alert("This designation and level combination is already added.");
            return;
        }

        const rateEndpoint = `${contextPath}/department/manpower/rate?designationId=${encodeURIComponent(designationId)}&levelCode=${encodeURIComponent(levelCode)}`;
        fetch(rateEndpoint)
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Rate lookup failed.");
                }
                return response.text();
            })
            .then((rateText) => {
                const monthlyRate = Number(rateText);
                if (Number.isNaN(monthlyRate) || monthlyRate <= 0) {
                    throw new Error("Invalid monthly rate received.");
                }

                appendRequirementRow({
                    rowKey,
                    designationId,
                    designationName,
                    levelCode,
                    levelName,
                    monthlyRate
                });

                requirementRowKeys.add(rowKey);
                resequenceRequirementRows();
                recalculateGrandTotalCost();
                resetRequirementSelectors();
            })
            .catch((error) => {
                console.error("Unable to fetch monthly rate.", error);
                alert("Unable to fetch rate for selected designation and level.");
            });
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
                <span class="monthlyRateText">${formatCurrency(requirementData.monthlyRate)}</span>
                <input type="hidden" class="monthlyRateValue" data-field="monthlyRate" value="${requirementData.monthlyRate}">
            </td>
            <td>
                <input type="number" min="1" class="form-control requiredQuantityInput" data-field="requiredQuantity" value="1">
            </td>
            <td>
                <input type="number" min="${MINIMUM_DURATION_IN_MONTHS}" class="form-control durationInMonthsInput" data-field="durationInMonths" value="${MINIMUM_DURATION_IN_MONTHS}">
            </td>
            <td>
                <span class="rowTotalCostText">0.00</span>
                <input type="hidden" class="rowTotalCostValue" data-field="totalCost" value="0">
            </td>
            <td>
                <button type="button" class="btn btn-outline-danger btn-sm removeRequirementButton">Remove</button>
            </td>
        `;

        resourceRequirementTableBody.appendChild(rowElement);
        recalculateRowTotalCost(rowElement);
    }

    function onResourceTableInput(event) {
        const changedElement = event.target;
        if (!changedElement.classList.contains("requiredQuantityInput")
            && !changedElement.classList.contains("durationInMonthsInput")) {
            return;
        }

        const rowElement = changedElement.closest("tr");
        if (!rowElement) {
            return;
        }

        recalculateRowTotalCost(rowElement);
        recalculateGrandTotalCost();
    }

    function onResourceTableClick(event) {
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
        recalculateGrandTotalCost();
    }

    function recalculateRowTotalCost(rowElement) {
        const quantityInput = rowElement.querySelector(".requiredQuantityInput");
        const durationInput = rowElement.querySelector(".durationInMonthsInput");
        const monthlyRateInput = rowElement.querySelector(".monthlyRateValue");
        const rowTotalCostText = rowElement.querySelector(".rowTotalCostText");
        const rowTotalCostValue = rowElement.querySelector(".rowTotalCostValue");

        const quantity = Math.max(1, Number(quantityInput?.value || 1));
        const durationInMonths = Math.max(MINIMUM_DURATION_IN_MONTHS, Number(durationInput?.value || MINIMUM_DURATION_IN_MONTHS));
        const monthlyRate = Number(monthlyRateInput?.value || 0);
        const totalCost = quantity * durationInMonths * monthlyRate;

        if (quantityInput) {
            quantityInput.value = quantity;
        }
        if (durationInput) {
            durationInput.value = durationInMonths;
        }
        if (rowTotalCostText) {
            rowTotalCostText.textContent = formatCurrency(totalCost);
        }
        if (rowTotalCostValue) {
            rowTotalCostValue.value = totalCost.toFixed(2);
        }
    }

    function recalculateGrandTotalCost() {
        const rows = resourceRequirementTableBody?.querySelectorAll("tr") || [];
        let total = 0;

        rows.forEach((row) => {
            const rowCostValue = Number(row.querySelector(".rowTotalCostValue")?.value || 0);
            total += rowCostValue;
        });

        if (grandTotalCostText) {
            grandTotalCostText.textContent = formatCurrency(total);
        }
        if (grandTotalCostValue) {
            grandTotalCostValue.value = total.toFixed(2);
        }

        updateTaxSummary(total);
    }

    function resequenceRequirementRows() {
        const rows = resourceRequirementTableBody?.querySelectorAll("tr") || [];
        rows.forEach((row, rowIndex) => {
            const fields = row.querySelectorAll("[data-field]");
            fields.forEach((fieldElement) => {
                const fieldName = fieldElement.getAttribute("data-field");
                if (!fieldName) {
                    return;
                }
                fieldElement.setAttribute("name", `resourceRequirements[${rowIndex}].${fieldName}`);
            });
        });
    }

    function onWorkOrderFileChange() {
        if (!workOrderFileInput) {
            return;
        }
        validateWorkOrderFile(workOrderFileInput);
    }

    function validateWorkOrderFile(fileInputElement) {
        const selectedFile = fileInputElement.files && fileInputElement.files[0];
        if (!selectedFile) {
            clearSelectedWorkOrderPreview();
            if (hasExistingWorkOrderDocument()) {
                setWorkOrderValidationMessage("");
                return true;
            }
            setWorkOrderValidationMessage("Work-order document is mandatory.");
            return false;
        }

        const allowedTypes = new Set([
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        ]);
        const maxFileSizeInBytes = 5 * 1024 * 1024;

        if (!allowedTypes.has(selectedFile.type)) {
            setWorkOrderValidationMessage("Invalid file type. Only PDF, DOC, DOCX are allowed.");
            fileInputElement.value = "";
            clearSelectedWorkOrderPreview();
            return false;
        }

        if (selectedFile.size > maxFileSizeInBytes) {
            setWorkOrderValidationMessage("File size must be less than or equal to 5 MB.");
            fileInputElement.value = "";
            clearSelectedWorkOrderPreview();
            return false;
        }

        setWorkOrderValidationMessage("");
        updateSelectedWorkOrderPreview(selectedFile);
        return true;
    }

    function setWorkOrderValidationMessage(message) {
        if (workOrderValidationMessage) {
            workOrderValidationMessage.textContent = message || "";
        }
    }

    function onSaveDraftClick() {
        if (!validateWorkOrderFile(workOrderFileInput || { files: [] })) {
            return;
        }
        actionStatusInput.value = "draft";
        hiddenSubmitButton.click();
    }

    function onPreviewSubmitClick() {
        if (!validateWorkOrderFile(workOrderFileInput || { files: [] })) {
            return;
        }

        if (!buildPreviewContent()) {
            alert("Please add at least one resource requirement before submit.");
            return;
        }

        actionStatusInput.value = "submit";
        const previewModalElement = document.getElementById("applicationPreviewModal");
        if (!previewModalElement || !window.bootstrap) {
            hiddenSubmitButton.click();
            return;
        }

        const modalInstance = window.bootstrap.Modal.getOrCreateInstance(previewModalElement);
        modalInstance.show();
    }

    function onConfirmSubmitClick() {
        actionStatusInput.value = "submit";
        hiddenSubmitButton.click();
    }

    function buildPreviewContent() {
        const previewProjectName = document.getElementById("previewProjectName");
        const previewApplicationType = document.getElementById("previewApplicationType");
        const previewTableBody = document.getElementById("previewResourceRequirementBody");

        if (!previewTableBody) {
            return true;
        }

        previewProjectName.textContent = formElement.querySelector('[name="projectName"]')?.value || "";
        previewApplicationType.textContent = formElement.querySelector('[name="applicationType"] option:checked')?.textContent || "";

        previewTableBody.innerHTML = "";
        const rows = resourceRequirementTableBody?.querySelectorAll("tr") || [];
        if (!rows.length) {
            return false;
        }

        rows.forEach((row) => {
            const designationName = row.querySelector('[data-field="designationName"]')?.value || "";
            const levelName = row.querySelector('[data-field="levelName"]')?.value || "";
            const monthlyRate = Number(row.querySelector(".monthlyRateValue")?.value || 0);
            const requiredQuantity = Number(row.querySelector(".requiredQuantityInput")?.value || 0);
            const durationInMonths = Number(row.querySelector(".durationInMonthsInput")?.value || 0);
            const rowTotalCost = Number(row.querySelector(".rowTotalCostValue")?.value || 0);

            const previewRow = document.createElement("tr");
            previewRow.innerHTML = `
                <td>${escapeHtml(designationName)}</td>
                <td>${escapeHtml(levelName)}</td>
                <td>${formatCurrency(monthlyRate)}</td>
                <td>${requiredQuantity}</td>
                <td>${durationInMonths}</td>
                <td>${formatCurrency(rowTotalCost)}</td>
            `;
            previewTableBody.appendChild(previewRow);
        });

        const baseCost = Number(grandTotalCostValue?.value || 0);
        const taxComponents = buildTaxComponents(baseCost);
        updatePreviewTaxSummary(baseCost, taxComponents);
        return true;
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

    function normalizeTaxRates(taxRates) {
        if (!Array.isArray(taxRates)) {
            return [];
        }

        return taxRates
            .map((taxRate) => {
                const taxCode = String(taxRate?.taxCode || "").trim();
                const taxName = String(taxRate?.taxName || "").trim();
                const ratePercentage = Number(taxRate?.ratePercentage || 0);

                return {
                    taxCode,
                    taxName,
                    ratePercentage
                };
            })
            .filter((taxRate) => taxRate.taxCode && Number.isFinite(taxRate.ratePercentage) && taxRate.ratePercentage > 0);
    }

    function updateTaxSummary(baseCost) {
        const taxComponents = buildTaxComponents(baseCost);
        const totalTaxAmount = taxComponents.reduce((sum, taxComponent) => sum + taxComponent.taxAmount, 0);
        const grandTotalIncludingTax = baseCost + totalTaxAmount;

        renderTaxBreakupRows(taxBreakupBody, taxComponents);

        if (totalTaxAmountText) {
            totalTaxAmountText.textContent = formatCurrency(totalTaxAmount);
        }
        if (grandTotalIncludingTaxText) {
            grandTotalIncludingTaxText.textContent = formatCurrency(grandTotalIncludingTax);
        }

        updatePreviewTaxSummary(baseCost, taxComponents);
    }

    function updatePreviewTaxSummary(baseCost, taxComponents) {
        const totalTaxAmount = taxComponents.reduce((sum, taxComponent) => sum + taxComponent.taxAmount, 0);
        const grandTotalIncludingTax = baseCost + totalTaxAmount;

        if (previewGrandTotalCost) {
            previewGrandTotalCost.textContent = formatCurrency(baseCost);
        }
        renderTaxBreakupRows(previewTaxBreakupBody, taxComponents);
        if (previewTotalTaxAmount) {
            previewTotalTaxAmount.textContent = formatCurrency(totalTaxAmount);
        }
        if (previewGrandTotalIncludingTax) {
            previewGrandTotalIncludingTax.textContent = formatCurrency(grandTotalIncludingTax);
        }
    }

    function buildTaxComponents(baseCost) {
        return applicableTaxRates.map((taxRate) => {
            const taxAmount = roundToTwo((baseCost * taxRate.ratePercentage) / 100);
            return {
                label: taxRate.taxName || taxRate.taxCode,
                ratePercentage: taxRate.ratePercentage,
                taxAmount
            };
        });
    }

    function renderTaxBreakupRows(container, taxComponents) {
        if (!container) {
            return;
        }

        container.innerHTML = "";

        taxComponents.forEach((taxComponent) => {
            const taxRow = document.createElement("tr");
            taxRow.innerHTML = `
                <th class="text-end">${escapeHtml(taxComponent.label)} (${formatPercentage(taxComponent.ratePercentage)}%)</th>
                <td class="text-end">${formatCurrency(taxComponent.taxAmount)}</td>
            `;
            container.appendChild(taxRow);
        });
    }

    function hasExistingWorkOrderDocument() {
        if (!existingWorkOrderFilePathInput || typeof existingWorkOrderFilePathInput.value !== "string") {
            return false;
        }
        return existingWorkOrderFilePathInput.value.trim().length > 0;
    }

    function updateSelectedWorkOrderPreview(selectedFile) {
        if (!selectedWorkOrderPreview || !selectedWorkOrderLink || !selectedWorkOrderName || !selectedFile) {
            return;
        }

        clearSelectedWorkOrderPreview();

        selectedWorkOrderObjectUrl = URL.createObjectURL(selectedFile);
        selectedWorkOrderLink.href = selectedWorkOrderObjectUrl;
        selectedWorkOrderName.textContent = selectedFile.name || "";
        selectedWorkOrderPreview.classList.remove("d-none");
    }

    function clearSelectedWorkOrderPreview() {
        if (selectedWorkOrderObjectUrl) {
            URL.revokeObjectURL(selectedWorkOrderObjectUrl);
            selectedWorkOrderObjectUrl = null;
        }

        if (!selectedWorkOrderPreview || !selectedWorkOrderLink || !selectedWorkOrderName) {
            return;
        }

        selectedWorkOrderLink.href = "#";
        selectedWorkOrderName.textContent = "";
        selectedWorkOrderPreview.classList.add("d-none");
    }

    function formatCurrency(value) {
        return Number(value || 0).toFixed(2);
    }

    function formatPercentage(value) {
        return Number(value || 0).toFixed(2);
    }

    function roundToTwo(value) {
        return Math.round((Number(value || 0) + Number.EPSILON) * 100) / 100;
    }

    function escapeHtml(value) {
        return String(value || "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }
})();
