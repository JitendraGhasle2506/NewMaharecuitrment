(() => {
    const contextPath = document.querySelector('meta[name="app-context-path"]')?.content || '';
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    const state = {
        cells: [],
        selectedCellId: null,
        positions: [],
        designations: [],
        levels: [],
        employeeRequestId: 0,
        cellRequestId: 0
    };

    const $ = (id) => document.getElementById(id);
    const apiUrl = (path) => `${contextPath}${path}`;

    document.addEventListener('DOMContentLoaded', async () => {
        if (!$('tm2CellSource')) {
            return;
        }

        readCells();
        bindNewScreenEvents();
        renderWingTree();
        renderEmptyState();
        resetPositionForm();

        try {
            await loadDesignations();
            if (state.cells.length) {
                await selectCell(state.cells[0].id);
            }
        } catch (error) {
            showAlert(error.message, 'danger');
        }
    });

    function bindNewScreenEvents() {
        $('tm2WingTree').addEventListener('click', async (event) => {
            const button = event.target.closest('button[data-cell-id]');
            if (!button) {
                return;
            }
            await selectCell(Number(button.dataset.cellId));
        });

        $('tm2SearchInput').addEventListener('input', () => {
            renderWingTree();
            renderPositionsTable();
        });

        $('tm2PositionForm').addEventListener('submit', savePosition);
        $('tm2PositionResetButton').addEventListener('click', resetPositionForm);
        $('tm2DesignationId').addEventListener('change', async () => {
            try {
                await loadLevels();
                await loadEmployees();
                updatePositionName();
            } catch (error) {
                showAlert(error.message, 'danger');
            }
        });
        $('tm2PositionLevelCode').addEventListener('change', async () => {
            updatePositionName();
            try {
                await loadEmployees();
            } catch (error) {
                showAlert(error.message, 'danger');
            }
        });
        $('tm2PositionsBody').addEventListener('click', handlePositionAction);
    }

    function readCells() {
        state.cells = Array.from($('tm2CellSource').options)
            .filter((option) => option.value)
            .map((option) => ({
                id: Number(option.value),
                cellName: option.dataset.cell || option.textContent.trim(),
                wingName: option.dataset.wing || 'Unassigned Wing'
            }))
            .sort((left, right) => left.wingName.localeCompare(right.wingName, undefined, { sensitivity: 'base' })
                || left.cellName.localeCompare(right.cellName, undefined, { sensitivity: 'base' }));
    }

    async function selectCell(cellId) {
        const cell = state.cells.find((item) => item.id === Number(cellId));
        if (!cell) {
            return;
        }

        state.selectedCellId = cell.id;
        state.positions = [];
        const requestId = ++state.cellRequestId;

        $('tm2EmptyState').classList.add('d-none');
        $('tm2CellWorkspace').classList.remove('d-none');
        $('tm2SelectedWing').textContent = cell.wingName;
        $('tm2SelectedCell').textContent = cell.cellName;
        $('tm2PositionCellId').value = cell.id;
        $('tm2PositionsBody').innerHTML = loadingRow(6, 'Loading positions...');
        renderStats();
        renderWingTree();
        resetPositionForm();

        try {
            const positionsPage = await request(`/api/master/positions${query({ cellId: cell.id, size: 500 })}`);
            if (requestId !== state.cellRequestId) {
                return;
            }
            state.positions = pageItems(positionsPage);
            renderPositionsTable();
            renderStats();
            resetPositionForm();
            renderWingTree();
        } catch (error) {
            if (requestId === state.cellRequestId) {
                renderTableError($('tm2PositionsBody'), 6, 'Unable to load positions.');
            }
            showAlert(error.message, 'danger');
        }
    }

    async function loadDesignations() {
        state.designations = await request('/api/master/organization-hierarchy/options/designations') || [];
        fillSelect($('tm2DesignationId'), state.designations.map((designation) => ({
            value: designation.id,
            label: designation.label
        })), 'Select Designation');
    }

    async function loadLevels(selectedLevelCode) {
        const designationId = valueOrNull($('tm2DesignationId').value);
        const levelSelect = $('tm2PositionLevelCode');
        if (!designationId) {
            state.levels = [];
            fillSelect(levelSelect, [], 'Select Designation First');
            levelSelect.disabled = true;
            resetEmployeeSelect('Select Designation First');
            return;
        }

        state.levels = await request(`/api/master/organization-hierarchy/options/levels${query({ designationId })}`) || [];
        const levelOptions = state.levels.map((level) => ({
            value: level.code,
            label: level.code ? `${level.code} - ${level.label}` : level.label
        }));
        fillSelect(levelSelect, levelOptions, levelOptions.length ? 'Any Level' : 'No Level Required');
        levelSelect.disabled = levelOptions.length === 0;
        if (selectedLevelCode && levelOptions.some((option) => String(option.value) === String(selectedLevelCode))) {
            levelSelect.value = selectedLevelCode;
        }
    }

    async function loadEmployees(selectedEmployeeId) {
        const designationId = valueOrNull($('tm2DesignationId').value);
        if (!designationId) {
            resetEmployeeSelect('Select Designation First');
            return;
        }

        const requestId = ++state.employeeRequestId;
        const employeeSelect = $('tm2EmployeeId');
        employeeSelect.disabled = true;
        fillSelect(employeeSelect, [], 'Loading matching employees...');

        const levelCode = $('tm2PositionLevelCode').disabled ? null : $('tm2PositionLevelCode').value || null;
        const page = await request(`/api/master/organization-hierarchy/options/employees${query({
            designationId,
            levelCode,
            size: 500
        })}`);
        if (requestId !== state.employeeRequestId) {
            return;
        }

        const employees = pageItems(page).map((employee) => ({
            value: employee.id,
            label: employee.code ? `${employee.label} (${employee.code})` : employee.label
        }));
        fillSelect(employeeSelect, employees, employees.length ? 'Vacant' : 'Vacant (No matching employees)');
        employeeSelect.disabled = false;
        if (selectedEmployeeId && employees.some((employee) => String(employee.value) === String(selectedEmployeeId))) {
            employeeSelect.value = selectedEmployeeId;
        }
    }

    async function savePosition(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const cell = selectedCell();
        updatePositionName();
        form.classList.add('was-validated');

        const designationId = valueOrNull($('tm2DesignationId').value);
        if (!cell) {
            showAlert('Select a cell before saving the position.', 'warning');
            return;
        }
        if (!designationId || !form.checkValidity()) {
            showAlert('Select a designation before saving the position.', 'warning');
            return;
        }

        const positionId = valueOrNull($('tm2PositionId').value);
        const payload = {
            positionName: $('tm2PositionName').value.trim(),
            cellId: cell.id,
            teamId: null,
            designationId,
            levelCode: $('tm2PositionLevelCode').disabled ? null : $('tm2PositionLevelCode').value || null,
            reportingPositionId: valueOrNull($('tm2ReportingPositionId').value),
            employeeId: valueOrNull($('tm2EmployeeId').value),
            displayOrder: Number($('tm2PositionDisplayOrder').value || 0),
            status: 'ACTIVE'
        };

        try {
            await request(
                positionId ? `/api/master/positions/${positionId}` : '/api/master/positions',
                { method: positionId ? 'PUT' : 'POST', body: JSON.stringify(payload) }
            );
            showAlert(positionId ? 'Position updated successfully.' : 'Position created successfully.', 'success');
            await selectCell(cell.id);
        } catch (error) {
            showAlert(error.message, 'danger');
        }
    }

    async function handlePositionAction(event) {
        const button = event.target.closest('button[data-action]');
        if (!button) {
            return;
        }
        const positionId = Number(button.dataset.id);
        if (button.dataset.action === 'edit-position') {
            await editPosition(positionId);
        }
        if (button.dataset.action === 'deactivate-position') {
            if (!window.confirm('Deactivate this position?')) {
                return;
            }
            try {
                await request(`/api/master/positions/${positionId}`, { method: 'DELETE' });
                showAlert('Position deactivated successfully.', 'success');
                if (state.selectedCellId) {
                    await selectCell(state.selectedCellId);
                }
            } catch (error) {
                showAlert(error.message, 'danger');
            }
        }
    }

    async function editPosition(positionId) {
        try {
            const position = await request(`/api/master/positions/${positionId}`);
            $('tm2PositionFormTitle').textContent = 'Edit Position';
            $('tm2PositionId').value = position.positionId;
            $('tm2PositionDisplayOrder').value = position.displayOrder || 0;
            $('tm2PositionName').value = position.positionName || '';
            $('tm2DesignationId').value = position.designationId || '';
            await loadLevels(position.levelCode);
            populateReportingPositionSelect(position.reportingPositionId);
            await loadEmployees(position.employeeId);
            updatePositionName();
            $('tm2PositionForm').classList.remove('was-validated');
            $('tm2PositionForm').scrollIntoView({ behavior: 'smooth', block: 'start' });
        } catch (error) {
            showAlert(error.message, 'danger');
        }
    }

    function resetPositionForm() {
        const form = $('tm2PositionForm');
        if (!form) {
            return;
        }
        form.reset();
        form.classList.remove('was-validated');
        $('tm2PositionFormTitle').textContent = 'Create Position';
        $('tm2PositionId').value = '';
        $('tm2PositionCellId').value = state.selectedCellId || '';
        $('tm2PositionDisplayOrder').value = nextPositionOrder();
        populateReportingPositionSelect();
        loadLevels().catch((error) => showAlert(error.message, 'danger'));
        resetEmployeeSelect('Select Designation First');
        updatePositionName();
    }

    function renderWingTree() {
        const tree = $('tm2WingTree');
        const search = normalizedSearch();
        const groups = new Map();
        state.cells.forEach((cell) => {
            if (!matchesCellSearch(cell, search)) {
                return;
            }
            if (!groups.has(cell.wingName)) {
                groups.set(cell.wingName, []);
            }
            groups.get(cell.wingName).push(cell);
        });

        $('tm2TreeSummary').textContent = `${groups.size} ${groups.size === 1 ? 'Wing' : 'Wings'}`;
        if (!groups.size) {
            tree.innerHTML = stateBlock('No matching wing or cell found.', false);
            return;
        }

        tree.innerHTML = Array.from(groups.entries()).map(([wingName, cells]) => `
            <details class="tm-wing-node" open>
                <summary>
                    <span><i class="fa-solid fa-sitemap" aria-hidden="true"></i>${escapeHtml(wingName)}</span>
                    <small>${cells.length} ${cells.length === 1 ? 'cell' : 'cells'}</small>
                </summary>
                <div class="tm-cell-node-list">
                    ${cells.map(renderCellNode).join('')}
                </div>
            </details>
        `).join('');
    }

    function renderCellNode(cell) {
        const isActive = String(cell.id) === String(state.selectedCellId);
        const isLoadedCell = isActive;
        const filled = isLoadedCell ? state.positions.filter((position) => position.positionStatus === 'FILLED').length : 0;
        const total = isLoadedCell ? state.positions.length : null;
        const countLabel = total === null ? '' : `<small>${filled}/${total} filled</small>`;
        return `
            <button class="tm-cell-node ${isActive ? 'is-active' : ''}" type="button" data-cell-id="${cell.id}">
                <span>
                    <i class="fa-solid fa-table-cells-large" aria-hidden="true"></i>
                    ${escapeHtml(cell.cellName)}
                </span>
                ${countLabel}
            </button>
        `;
    }

    function renderPositionsTable() {
        const body = $('tm2PositionsBody');
        const rows = filteredPositions();
        $('tm2PositionsSummary').textContent = `${rows.length} ${rows.length === 1 ? 'position' : 'positions'}`;
        if (!rows.length) {
            body.innerHTML = emptyRow(6);
            return;
        }
        body.innerHTML = rows.map((position, index) => `
            <tr>
                <td class="tm-cell-serial">${index + 1}</td>
                <td>
                    <span class="tm-cell-primary">${escapeHtml(position.positionName || '-')}</span>
                    <span class="tm-cell-secondary">${escapeHtml(position.reportingPositionName ? `Reports to ${position.reportingPositionName}` : 'No reporting position')}</span>
                </td>
                <td>
                    <span class="tm-cell-primary">${escapeHtml(position.designationName || '-')}</span>
                    <span class="tm-cell-secondary">${escapeHtml(position.levelCode || 'Any Level')}</span>
                </td>
                <td>
                    <span class="tm-cell-primary">${escapeHtml(position.employeeName || 'Vacant')}</span>
                    <span class="tm-cell-secondary">${escapeHtml(position.employeeCode || '')}</span>
                </td>
                <td class="text-center">${statusBadge(position.positionStatus)}</td>
                <td class="tm-cell-actions">
                    <button type="button" class="btn btn-outline-primary tm-action-btn" data-action="edit-position" data-id="${position.positionId}" title="Edit position">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button type="button" class="btn btn-outline-danger tm-action-btn" data-action="deactivate-position" data-id="${position.positionId}" title="Deactivate position">
                        <i class="fa-solid fa-ban"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    }

    function filteredPositions() {
        const search = normalizedSearch();
        if (!search) {
            return state.positions;
        }
        return state.positions.filter((position) => [
            position.positionName,
            position.designationName,
            position.levelCode,
            position.employeeName,
            position.employeeCode
        ].some((value) => String(value || '').toLowerCase().includes(search)));
    }

    function renderStats() {
        const total = state.positions.length;
        const filled = state.positions.filter((position) => position.positionStatus === 'FILLED').length;
        $('tm2TotalPositions').textContent = total;
        $('tm2FilledPositions').textContent = filled;
        $('tm2VacantPositions').textContent = Math.max(total - filled, 0);
    }

    function renderEmptyState() {
        $('tm2CellWorkspace').classList.add('d-none');
        $('tm2EmptyState').classList.remove('d-none');
        if (!state.cells.length) {
            $('tm2EmptyState').querySelector('strong').textContent = 'No active cells found';
            $('tm2EmptyState').querySelector('span').textContent = 'Create active wings and cells in master before using cell position management.';
        }
    }

    function populateReportingPositionSelect(selectedReportingId) {
        const currentPositionId = valueOrNull($('tm2PositionId').value);
        const options = state.positions
            .filter((position) => !currentPositionId || String(position.positionId) !== String(currentPositionId))
            .map((position) => ({
                value: position.positionId,
                label: `${position.positionName} - ${position.employeeName || position.designationName || 'Vacant'}`
            }));
        fillSelect($('tm2ReportingPositionId'), options, 'No Reporting Position');
        if (selectedReportingId && options.some((option) => String(option.value) === String(selectedReportingId))) {
            $('tm2ReportingPositionId').value = selectedReportingId;
        }
    }

    function updatePositionName() {
        const designationLabel = selectedOptionLabel($('tm2DesignationId'));
        if (!designationLabel) {
            $('tm2PositionName').value = '';
            return;
        }

        const cell = selectedCell();
        const levelCode = $('tm2PositionLevelCode').disabled ? '' : $('tm2PositionLevelCode').value;
        const sequence = String(nextPositionSequence()).padStart(2, '0');
        const suffix = ` - ${sequence}`;
        const base = [cell?.cellName, designationLabel, levelCode].filter(Boolean).join(' - ');
        $('tm2PositionName').value = `${base.slice(0, 150 - suffix.length).trim()}${suffix}`;
    }

    function nextPositionOrder() {
        return state.positions.reduce((max, position) => Math.max(max, Number(position.displayOrder || 0)), 0) + 10;
    }

    function nextPositionSequence() {
        const currentPositionId = valueOrNull($('tm2PositionId').value);
        const designationId = valueOrNull($('tm2DesignationId').value);
        const levelCode = $('tm2PositionLevelCode').disabled ? null : $('tm2PositionLevelCode').value || null;
        const current = currentPositionId
            ? state.positions.find((position) => String(position.positionId) === String(currentPositionId))
            : null;
        if (current && positionMatchesContext(current, designationId, levelCode)) {
            return trailingSequence(current.positionName) || 1;
        }
        const matches = state.positions.filter((position) => {
            if (currentPositionId && String(position.positionId) === String(currentPositionId)) {
                return false;
            }
            return positionMatchesContext(position, designationId, levelCode);
        });
        const sequences = matches
            .map((position) => trailingSequence(position.positionName))
            .filter((sequence) => sequence !== null);
        return sequences.length ? Math.max(...sequences) + 1 : matches.length + 1;
    }

    function positionMatchesContext(position, designationId, levelCode) {
        return sameOptionalId(position.designationId, designationId)
            && String(position.levelCode || '') === String(levelCode || '');
    }

    async function request(path, options = {}) {
        const headers = {
            Accept: 'application/json',
            ...(options.body ? { 'Content-Type': 'application/json' } : {})
        };
        if (csrfToken && csrfHeader && options.method && options.method !== 'GET') {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch(apiUrl(path), {
            cache: options.method && options.method !== 'GET' ? 'default' : 'no-store',
            ...options,
            headers: { ...headers, ...(options.headers || {}) }
        });
        const payload = await response.json().catch(() => ({ message: response.statusText }));
        if (!response.ok) {
            throw new Error(payload.message || 'Request failed');
        }
        return Object.prototype.hasOwnProperty.call(payload, 'data') ? payload.data : payload;
    }

    function fillSelect(select, options, blankLabel) {
        const current = select.value;
        select.innerHTML = `<option value="">${escapeHtml(blankLabel)}</option>` + options
            .map((option) => `<option value="${escapeHtml(option.value)}">${escapeHtml(option.label)}</option>`)
            .join('');
        if (options.some((option) => String(option.value) === String(current))) {
            select.value = current;
        }
    }

    function query(params) {
        const searchParams = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value !== null && value !== undefined && value !== '') {
                searchParams.set(key, value);
            }
        });
        const value = searchParams.toString();
        return value ? `?${value}` : '';
    }

    function pageItems(page) {
        if (Array.isArray(page)) {
            return page;
        }
        return Array.isArray(page?.content) ? page.content : [];
    }

    function selectedCell() {
        return state.cells.find((cell) => String(cell.id) === String(state.selectedCellId));
    }

    function selectedOptionLabel(select) {
        if (!select || !select.value) {
            return '';
        }
        return select.options[select.selectedIndex]?.textContent?.trim() || '';
    }

    function matchesCellSearch(cell, search) {
        if (!search) {
            return true;
        }
        return cell.wingName.toLowerCase().includes(search)
            || cell.cellName.toLowerCase().includes(search);
    }

    function normalizedSearch() {
        return $('tm2SearchInput').value.trim().toLowerCase();
    }

    function valueOrNull(value) {
        return value === null || value === undefined || value === '' ? null : Number(value);
    }

    function sameOptionalId(left, right) {
        return (left === null || left === undefined ? '' : String(left))
            === (right === null || right === undefined ? '' : String(right));
    }

    function trailingSequence(value) {
        const match = / - (\d+)$/.exec(value || '');
        return match ? Number(match[1]) : null;
    }

    function resetEmployeeSelect(blankLabel) {
        state.employeeRequestId += 1;
        fillSelect($('tm2EmployeeId'), [], blankLabel);
        $('tm2EmployeeId').disabled = true;
    }

    function statusBadge(status) {
        const filled = status === 'FILLED';
        return `<span class="tm-status-badge ${filled ? 'is-filled' : 'is-vacant'}">
            <i class="fa-solid ${filled ? 'fa-circle-check' : 'fa-circle-exclamation'}" aria-hidden="true"></i>
            ${filled ? 'Filled' : 'Vacant'}
        </span>`;
    }

    function loadingBlock(message) {
        return `<div class="tm-state-block">
            <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
            <span>${escapeHtml(message)}</span>
        </div>`;
    }

    function stateBlock(message, isError) {
        return `<div class="tm-state-block ${isError ? 'is-error' : ''}">
            <i class="fa-solid ${isError ? 'fa-triangle-exclamation' : 'fa-circle-info'}" aria-hidden="true"></i>
            <span>${escapeHtml(message)}</span>
        </div>`;
    }

    function emptyRow(colspan) {
        return `<tr class="tm-state-row"><td colspan="${colspan}">
            <div class="tm-table-state">
                <i class="fa-regular fa-folder-open" aria-hidden="true"></i>
                <span>No positions found</span>
            </div>
        </td></tr>`;
    }

    function loadingRow(colspan, message) {
        return `<tr class="tm-state-row"><td colspan="${colspan}">
            <div class="tm-table-state">
                <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
                <span>${escapeHtml(message)}</span>
            </div>
        </td></tr>`;
    }

    function renderTableError(body, colspan, message) {
        body.innerHTML = `<tr class="tm-state-row"><td colspan="${colspan}">
            <div class="tm-table-state is-error">
                <i class="fa-solid fa-triangle-exclamation" aria-hidden="true"></i>
                <span>${escapeHtml(message)}</span>
            </div>
        </td></tr>`;
    }

    function showAlert(message, type) {
        const alert = $('tm2Alert');
        alert.textContent = message;
        alert.className = `alert alert-${type}`;
        window.setTimeout(() => alert.classList.add('d-none'), 4500);
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }
})();

if (false) (() => {
    const contextPath = document.querySelector('meta[name="app-context-path"]')?.content || '';
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    const state = {
        projects: [],
        projectsLoaded: false,
        cells: [],
        teams: [],
        positionTeams: [],
        positions: [],
        designations: [],
        levels: [],
        employees: [],
        positionEmployeeRequestId: 0,
        mappingEmployees: [],
        mappingEmployeeRequestId: 0,
        dataVersion: 0,
        loaded: {
            dashboard: false,
            teams: false,
            positions: false,
            mappings: false,
            tree: false,
            chart: false
        },
        loading: {}
    };

    const $ = (id) => document.getElementById(id);
    const selectedCellId = () => valueOrNull($('tmCellFilter').value);
    const apiUrl = (path) => `${contextPath}${path}`;

    document.addEventListener('DOMContentLoaded', async () => {
        bindEvents();
        setToday();
        renderInitialLoadingState();
        try {
            await loadLookups();
        } catch (error) {
            showAlert(`Some form options could not be loaded: ${error.message}`, 'warning');
        }
        try {
            await loadActiveTab();
        } catch (error) {
            showAlert(error.message, 'danger');
        }
    });

    function bindEvents() {
        $('tmCellFilter').addEventListener('change', handleCellFilterChange);
        $('tmSearchButton').addEventListener('click', runSearch);
        $('tmSearchInput').addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                runSearch();
            }
        });
        $('tmSeedButton').addEventListener('click', seedSampleHierarchy);

        $('tmTeamForm').addEventListener('submit', saveTeam);
        $('tmPositionForm').addEventListener('submit', savePosition);
        $('tmMappingForm').addEventListener('submit', saveMapping);
        $('teamResetButton').addEventListener('click', resetTeamForm);
        $('positionResetButton').addEventListener('click', resetPositionForm);
        $('mappingResetButton').addEventListener('click', resetMappingForm);

        $('teamCellId').addEventListener('change', () => populateTeamSelects(valueOrNull($('teamCellId').value)));
        $('positionCellId').addEventListener('change', handlePositionCellChange);
        $('positionTeamId').addEventListener('change', () => {
            clearFieldError('positionTeamId');
            populatePositionReportingSelect();
            updatePositionName();
        });
        $('designationId').addEventListener('change', async () => {
            try {
                await populatePositionLevelSelect();
                await loadPositionEmployees();
                updatePositionName();
            } catch (error) {
                showAlert(error.message, 'danger');
            }
        });
        $('positionLevelCode').addEventListener('change', async () => {
            updatePositionName();
            try {
                await loadPositionEmployees();
            } catch (error) {
                showAlert(error.message, 'danger');
            }
        });
        $('mappingTeamId').addEventListener('change', async () => {
            populateMappingPositionSelect();
            try {
                await loadMappingEmployees();
            } catch (error) {
                showAlert(error.message, 'danger');
            }
        });
        $('mappingPositionId').addEventListener('change', async () => {
            try {
                await loadMappingEmployees();
            } catch (error) {
                showAlert(error.message, 'danger');
            }
        });

        $('tmTeamsBody').addEventListener('click', handleTeamAction);
        $('tmPositionsBody').addEventListener('click', handlePositionAction);
        $('tmMappingsBody').addEventListener('click', handleMappingAction);

        document.querySelectorAll('#tmTabs [data-bs-toggle="tab"]').forEach((tabButton) => {
            tabButton.addEventListener('shown.bs.tab', async (event) => {
                try {
                    await loadTab(event.target.getAttribute('data-bs-target'));
                } catch (error) {
                    showAlert(error.message, 'danger');
                }
            });
        });
    }

    async function loadLookups() {
        const designations = await request('/api/master/organization-hierarchy/options/designations');
        state.cells = Array.from($('tmCellFilter').options)
            .filter((option) => option.value)
            .map((option) => ({
                id: Number(option.value),
                label: option.textContent.trim(),
                code: null
            }));
        state.designations = designations || [];
        populateCellSelects();
        populateDesignationSelect();
        await populatePositionLevelSelect();
        resetPositionEmployeeSelect('Select Designation and Level First');
        resetMappingEmployeeSelect('Select Position First');
    }

    async function handleCellFilterChange() {
        invalidateData();
        state.teams = [];
        state.positions = [];
        $('tmSearchResults').innerHTML = '';
        resetTeamForm();
        resetPositionForm();
        resetMappingForm();
        try {
            await loadActiveTab();
        } catch (error) {
            showAlert(error.message, 'danger');
        }
    }

    function activeTabTarget() {
        return document.querySelector('#tmTabs .nav-link.active')?.getAttribute('data-bs-target') || '#tmDashboard';
    }

    async function loadActiveTab(force = false) {
        return loadTab(activeTabTarget(), force);
    }

    async function loadTab(target, force = false) {
        switch (target) {
            case '#tmTeams':
                await loadTeams(force);
                populateTeamSelects(valueOrNull($('teamCellId').value));
                break;
            case '#tmPositions':
                await loadPositions(force);
                populatePositionReportingSelect();
                break;
            case '#tmMappings':
                await Promise.all([
                    loadTeams(force),
                    loadPositions(force),
                    loadMappings(force)
                ]);
                populateMappingTeamSelect();
                populateMappingPositionSelect();
                break;
            case '#tmTree':
                await loadTree(force);
                break;
            case '#tmChart':
                await loadChart(force);
                break;
            default:
                await loadDashboard(force);
        }
    }

    function invalidateData(...keys) {
        state.dataVersion += 1;
        const targets = keys.length ? keys : Object.keys(state.loaded);
        targets.forEach((key) => {
            state.loaded[key] = false;
        });
    }

    function invalidateAfterMutation(path) {
        if (path.includes('/employee-team-mappings')) {
            invalidateData('dashboard', 'positions', 'mappings', 'tree', 'chart');
        } else if (path.includes('/positions')) {
            invalidateData('dashboard', 'positions', 'mappings', 'tree', 'chart');
        } else {
            invalidateData();
        }
    }

    async function loadOnce(key, force, loader) {
        if (!force && state.loaded[key]) {
            return;
        }
        const version = state.dataVersion;
        const activeLoad = state.loading[key];
        if (activeLoad?.version === version) {
            return activeLoad.promise;
        }
        const pending = Promise.resolve()
            .then(() => loader(version))
            .then(() => {
                if (version === state.dataVersion) {
                    state.loaded[key] = true;
                }
            })
            .catch((error) => {
                if (version !== state.dataVersion) {
                    return;
                }
                state.loaded[key] = false;
                throw error;
            })
            .finally(() => {
                if (state.loading[key]?.promise === pending) {
                    delete state.loading[key];
                }
            });
        state.loading[key] = { version, promise: pending };
        return pending;
    }

    async function request(path, options = {}) {
        const headers = {
            Accept: 'application/json',
            ...(options.body ? { 'Content-Type': 'application/json' } : {})
        };
        if (csrfToken && csrfHeader && options.method && options.method !== 'GET') {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch(apiUrl(path), {
            cache: options.method && options.method !== 'GET' ? 'default' : 'no-store',
            ...options,
            headers: { ...headers, ...(options.headers || {}) }
        });
        const payload = await response.json().catch(() => ({ message: response.statusText }));
        if (!response.ok) {
            throw new Error(payload.message || 'Request failed');
        }
        return Object.prototype.hasOwnProperty.call(payload, 'data') ? payload.data : payload;
    }

    async function loadDashboard(force = false) {
        return loadOnce('dashboard', force, async (version) => {
            const cellId = selectedCellId();
            const dashboard = await request(`/api/master/organization-hierarchy/dashboard${query({ cellId })}`);
            if (version !== state.dataVersion) return;
            $('tmTotalPositions').textContent = dashboard.totalPositions || 0;
            $('tmFilledPositions').textContent = dashboard.filledPositions || 0;
            $('tmVacantPositions').textContent = dashboard.vacantPositions || 0;
            renderStrength(dashboard.teamStrength || []);
        });
    }

    async function loadTeams(force = false) {
        return loadOnce('teams', force, async (version) => {
            const page = await request(`/api/master/teams${query({ cellId: selectedCellId(), size: 500 })}`);
            if (version !== state.dataVersion) return;
            state.teams = pageItems(page);
            renderTeamsTable();
        }).catch((error) => {
            state.teams = [];
            renderTableError($('tmTeamsBody'), 5, 'Unable to load teams.');
            throw error;
        });
    }

    async function loadPositions(force = false) {
        const body = $('tmPositionsBody');
        return loadOnce('positions', force, async (version) => {
            body.innerHTML = loadingRow(7, 'Loading positions...');
            const page = await request(`/api/master/positions${query({
                cellId: selectedCellId(),
                size: 300
            })}`);
            if (version !== state.dataVersion) return;
            state.positions = pageItems(page);
            renderPositionsTable();
        }).catch((error) => {
            state.positions = [];
            renderTableError(body, 7, 'Unable to load positions.');
            throw error;
        });
    }

    async function loadMappings(force = false) {
        return loadOnce('mappings', force, async (version) => {
            const page = await request(`/api/master/employee-team-mappings${query({
                cellId: selectedCellId(),
                size: 200
            })}`);
            if (version !== state.dataVersion) return;
            renderMappingsTable(pageItems(page));
        }).catch((error) => {
            renderTableError($('tmMappingsBody'), 6, 'Unable to load employee mappings.');
            throw error;
        });
    }

    async function loadTree(force = false) {
        return loadOnce('tree', force, async (version) => {
            const root = $('tmTreeRoot');
            root.innerHTML = '<div class="tm-empty">Loading hierarchy...</div>';
            const tree = await request(`/api/master/organization-hierarchy/tree${query({ cellId: selectedCellId() })}`);
            if (version !== state.dataVersion) return;
            renderHierarchy(root, tree, false);
        });
    }

    async function loadChart(force = false) {
        return loadOnce('chart', force, async (version) => {
            const root = $('tmChartRoot');
            root.innerHTML = '<div class="tm-empty">Loading organization chart...</div>';
            const chart = await request(`/api/master/organization-hierarchy/chart${query({ cellId: selectedCellId() })}`);
            if (version !== state.dataVersion) return;
            renderHierarchy(root, chart, true);
        });
    }

    async function runSearch() {
        const search = $('tmSearchInput').value.trim();
        if (!search) {
            $('tmSearchResults').innerHTML = '';
            return;
        }
        try {
            const results = await request(`/api/master/organization-hierarchy/search${query({
                cellId: selectedCellId(),
                search
            })}`);
            renderSearchResults(results || []);
        } catch (error) {
            showAlert(error.message, 'danger');
        }
    }

    async function seedSampleHierarchy() {
        const cellId = selectedCellId();
        if (!cellId) {
            showAlert('Select a cell before generating sample hierarchy.', 'warning');
            return;
        }
        if (!state.projectsLoaded) {
            state.projects = await request('/api/master/organization-hierarchy/options/projects') || [];
            state.projectsLoaded = true;
        }
        const projectId = state.projects.find((project) => String(project.cellId) === String(cellId))?.id;
        if (!projectId) {
            showAlert('The selected cell has no active project for sample hierarchy generation.', 'warning');
            return;
        }
        try {
            await request(`/api/master/organization-hierarchy/projects/${projectId}/seed-sample`, { method: 'POST' });
            showAlert('Sample hierarchy generated successfully.', 'success');
            await loadLookups();
            invalidateData();
            await loadActiveTab();
        } catch (error) {
            showAlert(error.message, 'danger');
        }
    }

    async function saveTeam(event) {
        event.preventDefault();
        const teamId = valueOrNull($('teamId').value);
        const payload = {
            teamName: $('teamName').value.trim(),
            teamType: $('teamType').value,
            parentTeamId: valueOrNull($('parentTeamId').value),
            projectId: null,
            cellId: valueOrNull($('teamCellId').value),
            displayOrder: Number($('teamDisplayOrder').value || 0),
            status: 'ACTIVE'
        };
        await saveEntity(
            teamId ? `/api/master/teams/${teamId}` : '/api/master/teams',
            teamId ? 'PUT' : 'POST',
            payload,
            'Team saved successfully.'
        );
        resetTeamForm();
    }

    async function savePosition(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const positionId = valueOrNull($('positionId').value);
        const cellId = valueOrNull($('positionCellId').value);
        const teamId = valueOrNull($('positionTeamId').value);
        clearFieldError('positionCellId');
        clearFieldError('positionTeamId');

        let valid = true;
        if (!cellId) {
            setFieldError('positionCellId', 'Cell is required.');
            valid = false;
        }
        if (!teamId) {
            setFieldError('positionTeamId', 'Team is required.');
            valid = false;
        }
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            valid = false;
        }
        if (!valid) {
            return;
        }
        form.classList.remove('was-validated');
        updatePositionName();
        const payload = {
            positionName: $('positionName').value.trim(),
            cellId,
            teamId,
            designationId: valueOrNull($('designationId').value),
            levelCode: $('positionLevelCode').value || null,
            reportingPositionId: valueOrNull($('reportingPositionId').value),
            employeeId: valueOrNull($('employeeId').value),
            displayOrder: Number($('positionDisplayOrder').value || 0),
            status: 'ACTIVE'
        };
        await saveEntity(
            positionId ? `/api/master/positions/${positionId}` : '/api/master/positions',
            positionId ? 'PUT' : 'POST',
            payload,
            'Position saved successfully.'
        );
        resetPositionForm();
    }

    async function saveMapping(event) {
        event.preventDefault();
        const mappingId = valueOrNull($('mappingId').value);
        const teamId = valueOrNull($('mappingTeamId').value);
        const positionId = valueOrNull($('mappingPositionId').value);
        if (!teamId) {
            showAlert('Select a team before saving the mapping.', 'warning');
            return;
        }
        if (!positionId) {
            showAlert('Select a position before saving the mapping.', 'warning');
            return;
        }
        const payload = {
            teamId,
            positionId,
            employeeId: valueOrNull($('mappingEmployeeId').value),
            effectiveDate: $('effectiveDate').value,
            status: 'ACTIVE'
        };
        await saveEntity(
            mappingId ? `/api/master/employee-team-mappings/${mappingId}` : '/api/master/employee-team-mappings',
            mappingId ? 'PUT' : 'POST',
            payload,
            'Mapping saved successfully.'
        );
        resetMappingForm();
    }

    async function saveEntity(path, method, payload, message) {
        try {
            await request(path, { method, body: JSON.stringify(payload) });
            showAlert(message, 'success');
            invalidateAfterMutation(path);
            await loadActiveTab();
        } catch (error) {
            showAlert(error.message, 'danger');
        }
    }

    async function handleTeamAction(event) {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const id = button.dataset.id;
        if (button.dataset.action === 'edit-team') {
            const team = await request(`/api/master/teams/${id}`);
            $('teamId').value = team.teamId;
            $('teamName').value = team.teamName || '';
            $('teamType').value = team.teamType || 'DEVELOPMENT';
            $('teamCellId').value = team.cellId || '';
            populateTeamSelects(team.cellId, team.teamId);
            $('parentTeamId').value = team.parentTeamId || '';
            $('teamDisplayOrder').value = team.displayOrder || 0;
        }
        if (button.dataset.action === 'deactivate-team') {
            await deactivate(`/api/master/teams/${id}`, 'Team deactivated successfully.');
        }
    }

    async function handlePositionAction(event) {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const id = button.dataset.id;
        if (button.dataset.action === 'edit-position') {
            const position = await request(`/api/master/positions/${id}`);
            $('positionId').value = position.positionId;
            $('positionName').value = position.positionName || '';
            $('positionCellId').value = position.cellId || '';
            await loadPositionTeams(position.cellId, position.teamId);
            $('designationId').value = position.designationId || '';
            await populatePositionLevelSelect(position.levelCode);
            populatePositionReportingSelect(position.reportingPositionId);
            updatePositionName();
            await loadPositionEmployees(position.employeeId);
            $('positionDisplayOrder').value = position.displayOrder || 0;
            clearPositionScopeErrors();
        }
        if (button.dataset.action === 'deactivate-position') {
            await deactivate(`/api/master/positions/${id}`, 'Position deactivated successfully.');
        }
    }

    async function handleMappingAction(event) {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const id = button.dataset.id;
        if (button.dataset.action === 'edit-mapping') {
            const mapping = await request(`/api/master/employee-team-mappings/${id}`);
            $('mappingId').value = mapping.mappingId;
            $('mappingTeamId').value = mapping.teamId || '';
            populateMappingPositionSelect(mapping.positionId);
            await loadMappingEmployees(mapping.employeeId);
            $('effectiveDate').value = mapping.effectiveDate || today();
        }
        if (button.dataset.action === 'deactivate-mapping') {
            await deactivate(`/api/master/employee-team-mappings/${id}`, 'Mapping deactivated successfully.');
        }
    }

    async function deactivate(path, message) {
        try {
            await request(path, { method: 'DELETE' });
            showAlert(message, 'success');
            invalidateAfterMutation(path);
            await loadActiveTab();
        } catch (error) {
            showAlert(error.message, 'danger');
        }
    }

    function renderStrength(rows) {
        const body = $('tmStrengthBody');
        if (!rows.length) {
            body.innerHTML = emptyRow(5);
            return;
        }
        body.innerHTML = rows.map((row, index) => `
            <tr>
                <td class="tm-cell-serial">${index + 1}</td>
                <td><span class="tm-cell-primary">${escapeHtml(row.teamName)}</span></td>
                <td>${teamTypeBadge(row.teamType)}</td>
                <td class="text-center tm-cell-number">${row.filledPositions || 0} / ${row.totalPositions || 0}</td>
                <td class="text-center tm-cell-number tm-cell-vacant">${row.vacantPositions || 0}</td>
            </tr>
        `).join('');
    }

    function renderTeamsTable() {
        const body = $('tmTeamsBody');
        if (!state.teams.length) {
            body.innerHTML = emptyRow(5);
            return;
        }
        body.innerHTML = state.teams.map((team, index) => `
            <tr>
                <td class="tm-cell-serial">${index + 1}</td>
                <td><span class="tm-cell-primary">${escapeHtml(team.teamName)}</span></td>
                <td>${escapeHtml(team.cellName || '-')}</td>
                <td>${teamTypeBadge(team.teamType)}</td>
                <td class="tm-cell-actions">
                    <button class="btn btn-sm btn-outline-primary tm-action-btn" data-action="edit-team" data-id="${team.teamId}" title="Edit team" aria-label="Edit ${escapeHtml(team.teamName)}">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger tm-action-btn" data-action="deactivate-team" data-id="${team.teamId}" title="Deactivate team" aria-label="Deactivate ${escapeHtml(team.teamName)}">
                        <i class="fa-solid fa-ban"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    }

    function renderPositionsTable() {
        const body = $('tmPositionsBody');
        if (!state.positions.length) {
            body.innerHTML = emptyRow(7);
            return;
        }
        body.innerHTML = state.positions.map((position, index) => `
            <tr>
                <td class="tm-cell-serial">${index + 1}</td>
                <td>
                    <span class="tm-cell-primary">${escapeHtml(position.positionName)}</span>
                    <span class="tm-cell-secondary">${escapeHtml(designationLevel(position))}</span>
                </td>
                <td>${escapeHtml(position.cellName || '-')}</td>
                <td>${escapeHtml(position.teamName || '-')}</td>
                <td>
                    <span class="tm-cell-primary ${position.positionStatus === 'VACANT' ? 'tm-text-vacant' : ''}">
                        ${escapeHtml(position.displayName)}
                    </span>
                    ${position.employeeCode ? `<span class="tm-cell-secondary">Employee Code: ${escapeHtml(position.employeeCode)}</span>` : ''}
                </td>
                <td class="text-center">${statusBadge(position.positionStatus)}</td>
                <td class="tm-cell-actions">
                    <button class="btn btn-sm btn-outline-primary tm-action-btn" data-action="edit-position" data-id="${position.positionId}" title="Edit position" aria-label="Edit ${escapeHtml(position.positionName)}">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger tm-action-btn" data-action="deactivate-position" data-id="${position.positionId}" title="Deactivate position" aria-label="Deactivate ${escapeHtml(position.positionName)}">
                        <i class="fa-solid fa-ban"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    }

    function renderMappingsTable(rows) {
        const body = $('tmMappingsBody');
        if (!rows.length) {
            body.innerHTML = emptyRow(6);
            return;
        }
        body.innerHTML = rows.map((mapping, index) => `
            <tr>
                <td class="tm-cell-serial">${index + 1}</td>
                <td>
                    <span class="tm-cell-primary ${mapping.employeeId ? '' : 'tm-text-vacant'}">${escapeHtml(mapping.employeeName || 'Vacant')}</span>
                    ${mapping.employeeCode ? `<span class="tm-cell-secondary">Employee Code: ${escapeHtml(mapping.employeeCode)}</span>` : ''}
                </td>
                <td>${escapeHtml(mapping.teamName)}</td>
                <td>
                    <span class="tm-cell-primary">${escapeHtml(mapping.positionName)}</span>
                    <span class="tm-cell-secondary">${escapeHtml(designationLevel(mapping))}</span>
                </td>
                <td class="text-center text-nowrap">${escapeHtml(formatDisplayDate(mapping.effectiveDate))}</td>
                <td class="tm-cell-actions">
                    <button class="btn btn-sm btn-outline-primary tm-action-btn" data-action="edit-mapping" data-id="${mapping.mappingId}" title="Edit mapping" aria-label="Edit mapping for ${escapeHtml(mapping.employeeName || 'vacant position')}">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger tm-action-btn" data-action="deactivate-mapping" data-id="${mapping.mappingId}" title="Deactivate mapping" aria-label="Deactivate mapping for ${escapeHtml(mapping.employeeName || 'vacant position')}">
                        <i class="fa-solid fa-ban"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    }

    function renderSearchResults(results) {
        const root = $('tmSearchResults');
        if (!results.length) {
            root.innerHTML = '<div class="tm-empty">No results found</div>';
            return;
        }
        root.innerHTML = results.map((result) => `
            <div class="tm-search-item ${result.vacant ? 'is-vacant' : ''}">
                <strong>${escapeHtml(result.title)}</strong>
                <span>${escapeHtml([result.subtitle, result.teamName, result.cellName].filter(Boolean).join(' | '))}</span>
            </div>
        `).join('');
    }

    function renderHierarchy(container, rootNode, chartMode) {
        container.innerHTML = '';
        if (!rootNode) {
            container.innerHTML = '<div class="tm-empty">No hierarchy found</div>';
            return;
        }
        container.appendChild(createHierarchyNode(rootNode, chartMode, 0));
    }

    function createHierarchyNode(node, chartMode, depth) {
        const wrapper = document.createElement('div');
        wrapper.className = `${chartMode ? 'tm-chart-branch' : 'tm-tree-branch'} depth-${Math.min(depth, 6)}`;

        const header = document.createElement('button');
        header.type = 'button';
        header.className = `tm-node ${node.vacant ? 'is-vacant' : ''} node-${String(node.nodeType || '').toLowerCase()}`;
        header.setAttribute('aria-expanded', String(Boolean(node.children?.length)));
        header.innerHTML = `
            <span class="tm-node-icon">${nodeIcon(node)}</span>
            <span class="tm-node-copy">
                <strong>${escapeHtml(node.label)}</strong>
                <small>${escapeHtml(node.subtitle || node.designationName || '')}</small>
            </span>
            ${node.children?.length ? '<i class="fa-solid fa-chevron-down tm-node-toggle"></i>' : ''}
        `;

        const children = document.createElement('div');
        children.className = chartMode ? 'tm-chart-children' : 'tm-tree-children';
        (node.children || []).forEach((child) => children.appendChild(createHierarchyNode(child, chartMode, depth + 1)));

        header.addEventListener('click', () => {
            if (!node.children?.length) return;
            const collapsed = !children.hidden;
            children.hidden = collapsed;
            header.classList.toggle('is-node-collapsed', collapsed);
            header.setAttribute('aria-expanded', String(!collapsed));
        });

        wrapper.appendChild(header);
        if (node.children?.length) {
            wrapper.appendChild(children);
        }
        return wrapper;
    }

    function populateCellSelects() {
        const options = state.cells.map((cell) => ({
            value: cell.id,
            label: cell.code ? `${cell.code} - ${cell.label}` : cell.label
        }));
        fillSelect($('teamCellId'), options, 'Select Cell');
        fillSelect($('positionCellId'), options, 'Select Cell');
        resetPositionTeamSelect('Select Cell First');

        const currentFilter = $('tmCellFilter').value;
        $('tmCellFilter').innerHTML = '<option value="">All Cells</option>' + options
            .map((option) => `<option value="${option.value}">${escapeHtml(option.label)}</option>`)
            .join('');
        $('tmCellFilter').value = currentFilter;
    }

    async function handlePositionCellChange() {
        clearPositionScopeErrors();
        $('tmPositionForm').classList.remove('was-validated');
        const cellId = valueOrNull($('positionCellId').value);
        resetPositionTeamSelect(cellId ? 'Loading teams...' : 'Select Cell First');
        populatePositionReportingSelect();
        updatePositionName();
        if (!cellId) {
            return;
        }
        try {
            await loadPositionTeams(cellId);
        } catch (error) {
            showAlert(error.message, 'danger');
        }
    }

    async function loadPositionTeams(cellId, selectedTeamId) {
        const teamSelect = $('positionTeamId');
        state.positionTeams = [];
        teamSelect.disabled = true;
        fillSelect(teamSelect, [], 'Loading teams...');

        const teams = await request(`/master/team-management/teams-by-cell/${cellId}`);
        state.positionTeams = Array.isArray(teams) ? teams : [];
        if (!state.positionTeams.length) {
            fillSelect(teamSelect, [], 'No teams found');
            populatePositionReportingSelect();
            updatePositionName();
            return;
        }

        fillSelect(teamSelect, state.positionTeams.map((team) => ({
            value: team.id,
            label: team.teamName
        })), 'Select Team');
        teamSelect.disabled = false;
        if (selectedTeamId && state.positionTeams.some((team) => String(team.id) === String(selectedTeamId))) {
            teamSelect.value = selectedTeamId;
        }
        populatePositionReportingSelect();
        updatePositionName();
    }

    function resetPositionTeamSelect(blankLabel) {
        state.positionTeams = [];
        fillSelect($('positionTeamId'), [], blankLabel);
        $('positionTeamId').disabled = true;
    }

    function populateDesignationSelect() {
        fillSelect($('designationId'), state.designations.map((designation) => ({
            value: designation.id,
            label: designation.label
        })), 'Select Designation');
    }

    async function populatePositionLevelSelect(selectedLevelCode) {
        const designationId = valueOrNull($('designationId').value);
        if (!designationId) {
            state.levels = [];
            fillSelect($('positionLevelCode'), [], 'Select Level');
            $('positionLevelCode').disabled = true;
            resetPositionEmployeeSelect('Select Designation and Level First');
            updatePositionName();
            return;
        }
        state.levels = await request(`/api/master/organization-hierarchy/options/levels${query({ designationId })}`);
        const levelOptions = (state.levels || []).map((level) => ({
            value: level.code,
            label: level.code ? `${level.code} - ${level.label}` : level.label
        }));
        $('positionLevelCode').disabled = false;
        fillSelect($('positionLevelCode'), levelOptions, 'Select Level');
        if (selectedLevelCode && levelOptions.some((option) => String(option.value) === String(selectedLevelCode))) {
            $('positionLevelCode').value = selectedLevelCode;
        }
        updatePositionName();
    }

    async function loadPositionEmployees(selectedEmployeeId) {
        const designationId = valueOrNull($('designationId').value);
        const levelCode = $('positionLevelCode').value;
        if (!designationId || !levelCode) {
            resetPositionEmployeeSelect('Select Designation and Level First');
            return;
        }

        const requestId = ++state.positionEmployeeRequestId;
        const select = $('employeeId');
        select.disabled = true;
        fillSelect(select, [], 'Loading matching employees...');
        try {
            const page = await request(`/api/master/organization-hierarchy/options/employees${query({
                designationId,
                levelCode,
                size: 500
            })}`);
            if (requestId !== state.positionEmployeeRequestId
                    || String(designationId) !== String(valueOrNull($('designationId').value))
                    || String(levelCode) !== String($('positionLevelCode').value)) {
                return;
            }
            state.employees = pageItems(page);
            const employeeOptions = state.employees.map((employee) => ({
                value: employee.id,
                label: employee.code ? `${employee.label} (${employee.code})` : employee.label
            }));
            select.disabled = false;
            fillSelect(select, employeeOptions, employeeOptions.length ? 'Vacant' : 'Vacant (No matching employees)');
            if (selectedEmployeeId
                    && employeeOptions.some((option) => String(option.value) === String(selectedEmployeeId))) {
                select.value = selectedEmployeeId;
            }
        } catch (error) {
            if (requestId === state.positionEmployeeRequestId) {
                resetPositionEmployeeSelect('Unable to load employees');
            }
            throw error;
        }
    }

    function resetPositionEmployeeSelect(blankLabel) {
        state.positionEmployeeRequestId += 1;
        state.employees = [];
        fillSelect($('employeeId'), [], blankLabel);
        $('employeeId').disabled = true;
    }

    async function loadMappingEmployees(selectedEmployeeId) {
        const positionId = valueOrNull($('mappingPositionId').value);
        if (!positionId) {
            resetMappingEmployeeSelect('Select Position First');
            return;
        }

        const requestId = ++state.mappingEmployeeRequestId;
        const select = $('mappingEmployeeId');
        select.disabled = true;
        fillSelect(select, [], 'Loading matching employees...');
        try {
            const page = await request(`/api/master/organization-hierarchy/options/employees${query({
                positionId,
                size: 500
            })}`);
            if (requestId !== state.mappingEmployeeRequestId
                    || String(positionId) !== String(valueOrNull($('mappingPositionId').value))) {
                return;
            }
            state.mappingEmployees = pageItems(page);
            const employeeOptions = state.mappingEmployees.map((employee) => ({
                value: employee.id,
                label: employee.code ? `${employee.label} (${employee.code})` : employee.label
            }));
            select.disabled = false;
            fillSelect(select, employeeOptions, employeeOptions.length ? 'Vacant' : 'Vacant (No matching employees)');
            if (selectedEmployeeId
                    && employeeOptions.some((option) => String(option.value) === String(selectedEmployeeId))) {
                select.value = selectedEmployeeId;
            }
        } catch (error) {
            if (requestId === state.mappingEmployeeRequestId) {
                resetMappingEmployeeSelect('Unable to load employees');
            }
            throw error;
        }
    }

    function resetMappingEmployeeSelect(blankLabel) {
        state.mappingEmployeeRequestId += 1;
        state.mappingEmployees = [];
        fillSelect($('mappingEmployeeId'), [], blankLabel);
        $('mappingEmployeeId').disabled = true;
    }

    function populateTeamSelects(cellId, excludeTeamId) {
        const filteredTeams = state.teams.filter((team) => !cellId || String(team.cellId) === String(cellId));
        const teamOptions = filteredTeams
            .filter((team) => !excludeTeamId || String(team.teamId) !== String(excludeTeamId))
            .map((team) => ({ value: team.teamId, label: team.teamName }));
        fillSelect($('parentTeamId'), teamOptions, 'No Parent');
    }

    function populatePositionReportingSelect(selectedReportingId) {
        const cellId = valueOrNull($('positionCellId').value);
        const positionId = valueOrNull($('positionId').value);
        const teamId = valueOrNull($('positionTeamId').value);
        const reportingOptions = state.positions
            .filter((position) => !cellId || String(position.cellId) === String(cellId))
            .filter((position) => !teamId || !position.teamId || String(position.teamId) === String(teamId))
            .filter((position) => !positionId || String(position.positionId) !== String(positionId))
            .map((position) => ({
                value: position.positionId,
                label: `${position.positionName} - ${position.displayName}`
            }));
        fillSelect($('reportingPositionId'), reportingOptions, 'No Reporting Position');
        if (selectedReportingId) {
            $('reportingPositionId').value = selectedReportingId;
        }
        updatePositionName();
    }

    function updatePositionName() {
        const designationLabel = selectedOptionLabel($('designationId'));
        if (!designationLabel) {
            $('positionName').value = '';
            return;
        }

        const teamLabel = selectedOptionLabel($('positionTeamId'));
        const levelCode = $('positionLevelCode').value;
        const sequence = String(nextPositionSequence()).padStart(2, '0');
        const parts = [teamLabel, designationLabel, levelCode].filter(Boolean);
        const suffix = ` - ${sequence}`;
        const base = parts.join(' - ');
        $('positionName').value = `${base.slice(0, 150 - suffix.length).trim()}${suffix}`;
    }

    function nextPositionSequence() {
        const cellId = valueOrNull($('positionCellId').value);
        const teamId = valueOrNull($('positionTeamId').value);
        const designationId = valueOrNull($('designationId').value);
        const levelCode = $('positionLevelCode').value || null;
        const currentPositionId = valueOrNull($('positionId').value);

        const current = currentPositionId
            ? state.positions.find((position) => String(position.positionId) === String(currentPositionId))
            : null;
        if (current && positionMatchesNameContext(current, cellId, teamId, designationId, levelCode)) {
            const currentSequence = trailingSequence(current.positionName);
            if (currentSequence) {
                return currentSequence;
            }
        }

        const matchingPositions = state.positions.filter((position) => {
            if (currentPositionId && String(position.positionId) === String(currentPositionId)) {
                return false;
            }
            return positionMatchesNameContext(position, cellId, teamId, designationId, levelCode);
        });
        const usedSequences = matchingPositions
            .map((position) => trailingSequence(position.positionName))
            .filter((sequence) => sequence !== null);
        return usedSequences.length ? Math.max(...usedSequences) + 1 : matchingPositions.length + 1;
    }

    function positionMatchesNameContext(position, cellId, teamId, designationId, levelCode) {
        return (!cellId || String(position.cellId) === String(cellId))
            && sameOptionalId(position.teamId, teamId)
            && sameOptionalId(position.designationId, designationId)
            && String(position.levelCode || '') === String(levelCode || '');
    }

    function sameOptionalId(left, right) {
        return (left === null || left === undefined ? '' : String(left))
            === (right === null || right === undefined ? '' : String(right));
    }

    function trailingSequence(value) {
        const match = / - (\d+)$/.exec(value || '');
        return match ? Number(match[1]) : null;
    }

    function selectedOptionLabel(select) {
        if (!select || !select.value) {
            return '';
        }
        return select.options[select.selectedIndex]?.textContent?.trim() || '';
    }

    function populateMappingTeamSelect() {
        const cellId = selectedCellId();
        const teamOptions = state.teams
            .filter((team) => !cellId || String(team.cellId) === String(cellId))
            .map((team) => ({ value: team.teamId, label: team.teamName }));
        fillSelect($('mappingTeamId'), teamOptions, 'Select Team');
    }

    function populateMappingPositionSelect(selectedPositionId) {
        const teamId = valueOrNull($('mappingTeamId').value);
        const select = $('mappingPositionId');
        if (!teamId) {
            fillSelect(select, [], 'Select Team First');
            select.disabled = true;
            return;
        }

        const selectedTeam = state.teams.find((team) => String(team.teamId) === String(teamId));
        const compatiblePositions = state.positions
            .filter((position) => positionMatchesMappingTeam(position, selectedTeam))
            .sort((left, right) => mappingPositionPriority(left, teamId) - mappingPositionPriority(right, teamId)
                || Number(left.displayOrder || 0) - Number(right.displayOrder || 0)
                || String(left.positionName || '').localeCompare(String(right.positionName || '')));
        const positionOptions = compatiblePositions.map((position) => ({
                value: position.positionId,
                label: mappingPositionLabel(position, teamId)
            }));
        select.disabled = false;
        fillSelect(select, positionOptions, positionOptions.length ? 'Select Position' : 'No Compatible Positions');
        if (selectedPositionId) {
            select.value = selectedPositionId;
        }
    }

    function positionMatchesMappingTeam(position, team) {
        if (!team) {
            return false;
        }
        if (team.cellId && position.cellId && String(team.cellId) !== String(position.cellId)) {
            return false;
        }
        return !(team.projectId && position.projectId
            && String(team.projectId) !== String(position.projectId));
    }

    function mappingPositionPriority(position, selectedTeamId) {
        if (String(position.teamId || '') === String(selectedTeamId)) {
            return 0;
        }
        return position.teamId ? 2 : 1;
    }

    function mappingPositionLabel(position, selectedTeamId) {
        const baseLabel = `${position.positionName} - ${position.displayName}`;
        if (!position.teamId) {
            return `${baseLabel} (Unassigned)`;
        }
        if (String(position.teamId) !== String(selectedTeamId)) {
            return `${baseLabel} (Current Team: ${position.teamName || 'Other Team'})`;
        }
        return baseLabel;
    }

    function resetTeamForm() {
        $('tmTeamForm').reset();
        $('teamId').value = '';
        $('teamCellId').value = selectedCellId() || '';
        populateTeamSelects(valueOrNull($('teamCellId').value));
    }

    function resetPositionForm() {
        $('tmPositionForm').reset();
        $('tmPositionForm').classList.remove('was-validated');
        $('positionId').value = '';
        $('positionDisplayOrder').value = 0;
        resetPositionTeamSelect('Select Cell First');
        populatePositionReportingSelect();
        populatePositionLevelSelect();
        resetPositionEmployeeSelect('Select Designation and Level First');
        clearPositionScopeErrors();
    }

    function resetMappingForm() {
        $('tmMappingForm').reset();
        $('mappingId').value = '';
        setToday();
        populateMappingTeamSelect();
        populateMappingPositionSelect();
        resetMappingEmployeeSelect('Select Position First');
    }

    function setToday() {
        $('effectiveDate').value = today();
    }

    function fillSelect(select, options, blankLabel) {
        const current = select.value;
        select.innerHTML = `<option value="">${escapeHtml(blankLabel)}</option>` + options
            .map((option) => `<option value="${option.value}">${escapeHtml(option.label)}</option>`)
            .join('');
        if (options.some((option) => String(option.value) === String(current))) {
            select.value = current;
        }
    }

    function query(params) {
        const searchParams = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value !== null && value !== undefined && value !== '') {
                searchParams.set(key, value);
            }
        });
        const value = searchParams.toString();
        return value ? `?${value}` : '';
    }

    function pageItems(page) {
        if (Array.isArray(page)) {
            return page;
        }
        return Array.isArray(page?.content) ? page.content : [];
    }

    function valueOrNull(value) {
        return value === null || value === undefined || value === '' ? null : Number(value);
    }

    function showAlert(message, type) {
        const alert = $('tmAlert');
        alert.textContent = message;
        alert.className = `alert alert-${type}`;
        window.setTimeout(() => alert.classList.add('d-none'), 4000);
    }

    function setFieldError(fieldId, message) {
        const field = $(fieldId);
        field.classList.add('is-invalid');
        const feedback = $(`${fieldId}Feedback`);
        if (feedback) {
            feedback.textContent = message;
        }
    }

    function clearFieldError(fieldId) {
        $(fieldId).classList.remove('is-invalid');
    }

    function clearPositionScopeErrors() {
        clearFieldError('positionCellId');
        clearFieldError('positionTeamId');
    }

    function designationLevel(item) {
        const designation = item.designationName || 'Designation';
        return item.levelCode ? `${designation} (${item.levelCode})` : designation;
    }

    function statusBadge(status) {
        const vacant = status === 'VACANT';
        return `<span class="tm-status-badge ${vacant ? 'is-vacant' : 'is-filled'}">
            <i class="fa-solid ${vacant ? 'fa-circle-exclamation' : 'fa-circle-check'}" aria-hidden="true"></i>
            ${escapeHtml(vacant ? 'Vacant' : 'Filled')}
        </span>`;
    }

    function teamTypeBadge(teamType) {
        const normalized = String(teamType || '').toUpperCase();
        const labels = {
            DEVELOPMENT: 'Development',
            OM: 'O&M',
            SUPPORT: 'Support'
        };
        return `<span class="tm-type-badge">${escapeHtml(labels[normalized] || teamType || '-')}</span>`;
    }

    function formatDisplayDate(value) {
        const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || '');
        return match ? `${match[3]}-${match[2]}-${match[1]}` : (value || '-');
    }

    function emptyRow(colspan) {
        return `<tr class="tm-state-row"><td colspan="${colspan}">
            <div class="tm-table-state">
                <i class="fa-regular fa-folder-open" aria-hidden="true"></i>
                <span>No records found</span>
            </div>
        </td></tr>`;
    }

    function loadingRow(colspan, message) {
        return `<tr class="tm-state-row"><td colspan="${colspan}">
            <div class="tm-table-state">
                <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
                <span>${escapeHtml(message)}</span>
            </div>
        </td></tr>`;
    }

    function renderTableError(body, colspan, message) {
        body.innerHTML = `<tr class="tm-state-row"><td colspan="${colspan}">
            <div class="tm-table-state is-error">
                <i class="fa-solid fa-triangle-exclamation" aria-hidden="true"></i>
                <span>${escapeHtml(message)}</span>
            </div>
        </td></tr>`;
    }

    function renderInitialLoadingState() {
        $('tmStrengthBody').innerHTML = loadingRow(5, 'Loading team strength...');
        $('tmTeamsBody').innerHTML = loadingRow(5, 'Loading teams...');
        $('tmPositionsBody').innerHTML = loadingRow(7, 'Loading positions...');
        $('tmMappingsBody').innerHTML = loadingRow(6, 'Loading employee mappings...');
    }

    function nodeIcon(node) {
        if (node.nodeType === 'CELL') return '<i class="fa-solid fa-building"></i>';
        if (node.nodeType === 'PROJECT') return '<i class="fa-solid fa-diagram-project"></i>';
        if (node.nodeType === 'TEAM') return '<i class="fa-solid fa-people-group"></i>';
        if (node.vacant) return '<i class="fa-solid fa-user-slash"></i>';
        return '<i class="fa-solid fa-user-tie"></i>';
    }

    function today() {
        return new Date().toISOString().slice(0, 10);
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }
})();
