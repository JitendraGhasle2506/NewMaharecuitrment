(function () {
    "use strict";

    const MINIMUM_DURATION_IN_MONTHS = 1;

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
    const workOrderNumberInput = formElement.querySelector('[name="workOrderNumber"]');
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
    let commissionRates = { AGENCY: 10, MAHAIT: 10 };
    let selectedWorkOrderObjectUrl = null;

    initializeExistingRows();
    loadApplicableTaxRates();
    loadCommissionRates();
    recalculateGrandTotalCost();

    designationSelectElement?.addEventListener("change", onDesignationChange);
    addRequirementButton?.addEventListener("click", onAddRequirementClick);
    resourceRequirementTableBody?.addEventListener("input", onResourceTableInput);
    resourceRequirementTableBody?.addEventListener("change", onResourceTableChange);
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

    function loadCommissionRates() {
        const endpoint = `${contextPath}/department/manpower/commission-rates`;
        fetch(endpoint)
            .then((response) => response.json())
            .then((rates) => {
                commissionRates = {
                    AGENCY: Number(rates.AGENCY || 10),
                    MAHAIT: Number(rates.MAHAIT || 10)
                };
                recalculateGrandTotalCost();
            })
            .catch((error) => {
                console.error("Unable to load commission rates.", error);
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
                <input type="number" min="${MINIMUM_DURATION_IN_MONTHS}" class="form-control durationInMonthsInput" data-field="durationInMonths" value="12">
            </td>
            <td>
                <span class="rowTotalCostText">0.00</span>
                <input type="hidden" class="rowTotalCostValue" data-field="totalCost" value="0">
                <input type="hidden" data-field="agencyCommissionAmount" value="0">
                <input type="hidden" data-field="mahaItCommissionAmount" value="0">
                <input type="hidden" data-field="taxableAmount" value="0">
                <input type="hidden" data-field="gstAmount" value="0">
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

        recalculateRowTotalCost(rowElement, false);
        recalculateGrandTotalCost();
    }

    function onResourceTableChange(event) {
        const changedElement = event.target;
        if (!changedElement.classList.contains("requiredQuantityInput")
            && !changedElement.classList.contains("durationInMonthsInput")) {
            return;
        }

        const rowElement = changedElement.closest("tr");
        if (!rowElement) {
            return;
        }

        recalculateRowTotalCost(rowElement, true);
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

    function recalculateRowTotalCost(rowElement, normalizeInputs = true) {
        const quantityInput = rowElement.querySelector(".requiredQuantityInput");
        const durationInput = rowElement.querySelector(".durationInMonthsInput");
        const monthlyRateInput = rowElement.querySelector(".monthlyRateValue");
        const rowTotalCostText = rowElement.querySelector(".rowTotalCostText");
        const rowTotalCostValue = rowElement.querySelector(".rowTotalCostValue");

        // New hidden fields
        const agencyCommInput = rowElement.querySelector('[data-field="agencyCommissionAmount"]');
        const mahaItCommInput = rowElement.querySelector('[data-field="mahaItCommissionAmount"]');
        const taxableAmountInput = rowElement.querySelector('[data-field="taxableAmount"]');
        const gstAmountInput = rowElement.querySelector('[data-field="gstAmount"]');

        const rawQuantity = Number(quantityInput?.value);
        const rawDurationInMonths = Number(durationInput?.value);
        const quantity = Number.isFinite(rawQuantity) && rawQuantity > 0 ? Math.max(1, rawQuantity) : 0;
        const durationInMonths = Number.isFinite(rawDurationInMonths) && rawDurationInMonths > 0
            ? Math.max(MINIMUM_DURATION_IN_MONTHS, rawDurationInMonths)
            : 0;
        const monthlyRate = Number(monthlyRateInput?.value || 0);

        // Calculation Logic
        const manpowerValue = roundToTwo(quantity * durationInMonths * monthlyRate);
        const agencyCommission = roundToTwo(manpowerValue * (commissionRates.AGENCY / 100));
        const subTotal1 = manpowerValue + agencyCommission;
        const mahaItCommission = roundToTwo(subTotal1 * (commissionRates.MAHAIT / 100));
        const taxableAmount = subTotal1 + mahaItCommission;
        const gstAmount = roundToTwo(taxableAmount * 0.18);

        if (normalizeInputs && quantityInput) {
            quantityInput.value = quantity;
        }
        if (normalizeInputs && durationInput) {
            durationInput.value = durationInMonths;
        }
        if (rowTotalCostText) {
            rowTotalCostText.textContent = formatCurrency(manpowerValue);
        }
        if (rowTotalCostValue) {
            rowTotalCostValue.value = manpowerValue.toFixed(2);
        }

        // Update hidden fields
        if (agencyCommInput) agencyCommInput.value = agencyCommission.toFixed(2);
        if (mahaItCommInput) mahaItCommInput.value = mahaItCommission.toFixed(2);
        if (taxableAmountInput) taxableAmountInput.value = taxableAmount.toFixed(2);
        if (gstAmountInput) gstAmountInput.value = gstAmount.toFixed(2);
    }

    function recalculateGrandTotalCost() {
        const rows = resourceRequirementTableBody?.querySelectorAll("tr") || [];
        let totalManpowerValue = 0;
        let totalAgencyComm = 0;
        let totalMahaItComm = 0;
        let totalTaxableAmount = 0;

        rows.forEach((row) => {
            totalManpowerValue += Number(row.querySelector(".rowTotalCostValue")?.value || 0);
            totalAgencyComm += Number(row.querySelector('[data-field="agencyCommissionAmount"]')?.value || 0);
            totalMahaItComm += Number(row.querySelector('[data-field="mahaItCommissionAmount"]')?.value || 0);
            totalTaxableAmount += Number(row.querySelector('[data-field="taxableAmount"]')?.value || 0);
        });

        if (grandTotalCostText) {
            grandTotalCostText.textContent = formatCurrency(totalManpowerValue);
        }

        // Update summary displayed values (Total including commissions but excluding GST)
        updateTaxSummary(totalManpowerValue, totalAgencyComm, totalMahaItComm, totalTaxableAmount);
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
            setWorkOrderValidationMessage("Work Order Document / Demand / Requisition Letter is mandatory.");
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
        if (!validateWorkOrderNumber()) {
            return;
        }

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
        const previewWorkOrderNumber = document.getElementById("previewWorkOrderNumber");
        const previewTableBody = document.getElementById("previewResourceRequirementBody");

        if (!previewTableBody) {
            return true;
        }

        previewProjectName.textContent = formElement.querySelector('[name="projectName"]')?.value || "";
        previewApplicationType.textContent = formElement.querySelector('[name="applicationType"] option:checked')?.textContent || "";
        if (previewWorkOrderNumber) {
            previewWorkOrderNumber.textContent = workOrderNumberInput?.value?.trim() || "";
        }

        previewTableBody.innerHTML = "";
        const rows = resourceRequirementTableBody?.querySelectorAll("tr") || [];
        if (!rows.length) {
            return false;
        }

        let totalManpower = 0;
        let totalAgencyComm = 0;
        let totalMahaItComm = 0;
        let totalTaxable = 0;

        rows.forEach((row) => {
            const designationName = row.querySelector('[data-field="designationName"]')?.value || "";
            const levelName = row.querySelector('[data-field="levelName"]')?.value || "";
            const monthlyRate = Number(row.querySelector(".monthlyRateValue")?.value || 0);
            const requiredQuantity = Number(row.querySelector(".requiredQuantityInput")?.value || 0);
            const durationInMonths = Number(row.querySelector(".durationInMonthsInput")?.value || 0);
            const manpowerValue = Number(row.querySelector(".rowTotalCostValue")?.value || 0);

            totalManpower += manpowerValue;
            totalAgencyComm += Number(row.querySelector('[data-field="agencyCommissionAmount"]')?.value || 0);
            totalMahaItComm += Number(row.querySelector('[data-field="mahaItCommissionAmount"]')?.value || 0);
            totalTaxable += Number(row.querySelector('[data-field="taxableAmount"]')?.value || 0);

            const previewRow = document.createElement("tr");
            previewRow.innerHTML = `
                <td>${escapeHtml(designationName)}</td>
                <td>${escapeHtml(levelName)}</td>
                <td>${formatCurrency(monthlyRate)}</td>
                <td>${requiredQuantity}</td>
                <td>${durationInMonths}</td>
                <td>${formatCurrency(manpowerValue)}</td>
            `;
            previewTableBody.appendChild(previewRow);
        });

        const taxComponents = buildTaxComponents(totalTaxable);
        updatePreviewTaxSummary(totalManpower, totalAgencyComm, totalMahaItComm, totalTaxable, taxComponents);
        return true;
    }

    function validateWorkOrderNumber() {
        const workOrderNumber = workOrderNumberInput?.value?.trim() || "";
        if (workOrderNumber.length > 0) {
            return true;
        }

        alert("Work Order Number is mandatory.");
        workOrderNumberInput?.focus();
        return false;
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

    function updateTaxSummary(totalManpower, totalAgencyComm, totalMahaItComm, totalTaxable) {
        // Build commissions for the display
        const commissions = [
            { label: `Agency Commission (${formatPercentage(commissionRates.AGENCY)}%)`, amount: totalAgencyComm },
            { label: `MahaIT Commission (${formatPercentage(commissionRates.MAHAIT)}%)`, amount: totalMahaItComm }
        ];

        const taxComponents = buildTaxComponents(totalTaxable);
        const totalTaxAmount = taxComponents.reduce((sum, tx) => sum + tx.taxAmount, 0);
        const grandTotalIncludingTax = totalTaxable + totalTaxAmount;

        renderFinancialBreakdownRows(taxBreakupBody, commissions, taxComponents);

        if (totalTaxAmountText) {
            totalTaxAmountText.textContent = formatCurrency(totalTaxAmount);
        }
        if (grandTotalIncludingTaxText) {
            grandTotalIncludingTaxText.textContent = formatCurrency(grandTotalIncludingTax);
        }
        if (grandTotalCostValue) {
            grandTotalCostValue.value = grandTotalIncludingTax.toFixed(2);
        }

        updatePreviewTaxSummary(totalManpower, totalAgencyComm, totalMahaItComm, totalTaxable, taxComponents);
    }

    function updatePreviewTaxSummary(totalManpower, totalAgencyComm, totalMahaItComm, totalTaxable, taxComponents) {
        const totalTaxAmount = taxComponents.reduce((sum, tx) => sum + tx.taxAmount, 0);
        const grandTotalIncludingTax = totalTaxable + totalTaxAmount;

        if (previewGrandTotalCost) {
            previewGrandTotalCost.textContent = formatCurrency(totalManpower);
        }

        const commissions = [
            { label: `Agency Commission (${formatPercentage(commissionRates.AGENCY)}%)`, amount: totalAgencyComm },
            { label: `MahaIT Commission (${formatPercentage(commissionRates.MAHAIT)}%)`, amount: totalMahaItComm }
        ];

        renderFinancialBreakdownRows(previewTaxBreakupBody, commissions, taxComponents);

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

    function renderFinancialBreakdownRows(container, commissions, taxComponents) {
        if (!container) return;
        container.innerHTML = "";

        commissions.forEach(comm => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <th class="text-end text-muted small">${escapeHtml(comm.label)}</th>
                <td class="text-end small">${formatCurrency(comm.amount)}</td>
            `;
            container.appendChild(row);
        });

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
