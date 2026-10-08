(() => {
    'use strict';

    const form = document.getElementById('employeeConversionForm');
    if (!form) return;

    const targetTypeInput = document.getElementById('targetType');
    const employeeIdInput = document.getElementById('employeeId');
    const employeeSelect = document.getElementById('sourceEmployee');
    const departmentSelect = document.getElementById('departmentId');
    const locationSelect = document.getElementById('locationId');
    const departmentMappingCard = document.getElementById('departmentMappingCard');
    const departmentSummaryRow = document.getElementById('summaryDepartmentRow');
    const internalAssignments = document.getElementById('internalAssignments');
    const cellSelect = document.getElementById('cellId');
    const hodSelect = document.getElementById('reportingHodUserId');
    const managerTypeSelect = document.getElementById('managerType');
    const managerSelect = document.getElementById('reportingManagerEmployeeId');
    const directionButtons = [...document.querySelectorAll('.direction-option')];
    const convertButton = document.getElementById('convertButton');
    const initialEmployeeId = employeeIdInput.value;

    const title = value => value.charAt(0) + value.slice(1).toLowerCase();
    const sourceTypeFor = targetType => targetType === 'INTERNAL' ? 'EXTERNAL' : 'INTERNAL';
    const text = (id, value) => { document.getElementById(id).textContent = value; };

    function refreshSearchableSelect(select) {
        if (window.jQuery?.fn?.select2 && window.jQuery(select).hasClass('select2-hidden-accessible')) {
            window.jQuery(select).trigger('change.select2');
        }
    }

    function initializeSearchableSelects() {
        if (!window.jQuery?.fn?.select2) return;

        window.jQuery('.searchable-conversion-select').each(function () {
            const select = window.jQuery(this);
            if (select.hasClass('select2-hidden-accessible')) return;

            select.select2({
                theme: 'bootstrap-5',
                width: '100%',
                placeholder: select.data('placeholder') || 'Search and select',
                allowClear: true,
                minimumResultsForSearch: 0
            });
        });
    }

    function bindSelectChange(select, handler) {
        if (window.jQuery) {
            window.jQuery(select).on('change.employeeTypeConversion', handler);
            return;
        }
        select.addEventListener('change', handler);
    }

    function renderEmployees(targetType, selectedId) {
        const sourceType = sourceTypeFor(targetType);
        const template = document.getElementById(`${sourceType.toLowerCase()}EmployeeOptions`);
        employeeSelect.replaceChildren(new Option(`Select ${title(sourceType)} employee`, ''));
        employeeSelect.append(template.content.cloneNode(true));
        employeeSelect.disabled = employeeSelect.options.length === 1;
        document.getElementById('noEmployeesMessage').hidden = !employeeSelect.disabled;
        text('employeeHelp', `Choose an active ${sourceType.toLowerCase()} employee to convert.`);

        if (selectedId && [...employeeSelect.options].some(option => option.value === selectedId)) {
            employeeSelect.value = selectedId;
        } else {
            employeeIdInput.value = '';
        }
        refreshSearchableSelect(employeeSelect);
        updateEmployeeDetails();
    }

    function selectDirection(targetType, preserveSelection = false) {
        targetTypeInput.value = targetType;
        directionButtons.forEach(button => {
            const active = button.dataset.targetType === targetType;
            button.classList.toggle('active', active);
            button.setAttribute('aria-checked', String(active));
        });
        text('summarySourceType', title(sourceTypeFor(targetType)));
        text('summaryTargetType', title(targetType));
        updateInternalRequirements(targetType === 'INTERNAL');
        renderEmployees(targetType, preserveSelection ? employeeIdInput.value : '');
        updateButton();
    }

    function updateInternalRequirements(isInternal) {
        departmentMappingCard.hidden = isInternal;
        departmentSummaryRow.hidden = isInternal;
        departmentSelect.required = !isInternal;
        departmentSelect.disabled = isInternal;
        if (isInternal) departmentSelect.value = '';
        internalAssignments.hidden = !isInternal;
        document.querySelectorAll('.internal-summary-row').forEach(row => { row.hidden = !isInternal; });
        internalAssignments.querySelectorAll('[data-internal-required]').forEach(field => {
            field.required = isInternal;
            field.disabled = !isInternal || (field === managerSelect && !managerTypeSelect.value);
            refreshSearchableSelect(field);
        });
        refreshSearchableSelect(departmentSelect);
        text('summaryNoticeText', isInternal
            ? 'This clears department mappings and updates employee type, cell and reporting hierarchy in one audited transaction.'
            : 'This updates employee type and department and clears the internal reporting hierarchy.');
        updateInternalSummary();
    }

    function updateEmployeeDetails() {
        const option = employeeSelect.selectedOptions[0];
        const hasEmployee = Boolean(option?.value);
        const profile = document.getElementById('employeeProfile');
        profile.hidden = !hasEmployee;
        employeeIdInput.value = hasEmployee ? option.value : '';

        if (!hasEmployee) {
            text('summaryName', 'No employee selected');
            text('summaryCode', 'Select an employee to continue');
            text('summaryAvatar', '?');
            text('summaryCurrentDepartment', '—');
            updateButton();
            return;
        }

        const data = option.dataset;
        const initial = (data.name || 'E').trim().charAt(0).toUpperCase();
        text('employeeAvatar', initial);
        text('employeeName', data.name || '—');
        text('employeeEmail', data.email || '—');
        text('employeeCode', data.code || '—');
        text('employeeDesignation', data.designation || 'Not assigned');
        text('employeeDepartment', data.department || 'Not mapped');
        text('summaryAvatar', initial);
        text('summaryName', data.name || '—');
        text('summaryCode', data.code || 'Employee code not assigned');
        text('summaryCurrentDepartment', data.department || 'Not mapped');

        if (!departmentSelect.value && data.departmentId) {
            departmentSelect.value = data.departmentId;
            refreshSearchableSelect(departmentSelect);
        }
        updateDepartmentSummary();
        updateButton();
    }

    function updateDepartmentSummary() {
        const option = departmentSelect.selectedOptions[0];
        text('summaryNewDepartment', targetTypeInput.value === 'INTERNAL'
            ? 'Not mapped'
            : (option?.value ? option.textContent.trim() : 'Not selected'));
        updateButton();
    }

    function selectedLabel(select, fallback = 'Not selected') {
        const option = select.selectedOptions[0];
        return option?.value ? option.textContent.trim() : fallback;
    }

    function updateInternalSummary() {
        text('summaryCell', selectedLabel(cellSelect));
        text('summaryHod', selectedLabel(hodSelect));
        text('summaryManager', selectedLabel(managerSelect));
        updateButton();
    }

    function updateLocationSummary() {
        text('summaryLocation', selectedLabel(locationSelect, 'Keep current mapping'));
    }

    function updateButton() {
        const departmentComplete = targetTypeInput.value === 'INTERNAL' || departmentSelect.value;
        const baseComplete = employeeIdInput.value && departmentComplete && targetTypeInput.value;
        const internalComplete = targetTypeInput.value !== 'INTERNAL'
            || (cellSelect.value && hodSelect.value && managerTypeSelect.value && managerSelect.value);
        convertButton.disabled = !(baseComplete && internalComplete);
    }

    directionButtons.forEach(button => button.addEventListener('click', () => {
        selectDirection(button.dataset.targetType);
    }));
    bindSelectChange(employeeSelect, updateEmployeeDetails);
    bindSelectChange(departmentSelect, updateDepartmentSummary);
    bindSelectChange(locationSelect, updateLocationSummary);
    bindSelectChange(cellSelect, updateInternalSummary);
    bindSelectChange(hodSelect, updateInternalSummary);
    bindSelectChange(managerSelect, updateInternalSummary);
    form.addEventListener('submit', event => {
        const internalIncomplete = targetTypeInput.value === 'INTERNAL'
            && !(cellSelect.value && hodSelect.value && managerTypeSelect.value && managerSelect.value);
        const departmentIncomplete = targetTypeInput.value === 'EXTERNAL' && !departmentSelect.value;
        if (!employeeIdInput.value || departmentIncomplete || !targetTypeInput.value || internalIncomplete) {
            event.preventDefault();
            form.classList.add('was-validated');
        }
    });

    const directionFromUrl = new URLSearchParams(window.location.search).get('direction');
    const initialTarget = directionFromUrl === 'external-to-internal'
        ? 'INTERNAL'
        : (directionFromUrl === 'internal-to-external' ? 'EXTERNAL' : targetTypeInput.value || 'EXTERNAL');
    employeeIdInput.value = initialEmployeeId;
    selectDirection(initialTarget, true);
    updateDepartmentSummary();
    updateInternalSummary();
    updateLocationSummary();
    initializeSearchableSelects();
})();
