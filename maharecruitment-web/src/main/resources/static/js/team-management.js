(() => {
    'use strict';

    const contextPath = document.querySelector('meta[name="app-context-path"]')?.content || '';
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const state = {
        cells: [],
        teams: [],
        mappings: [],
        selectedCellId: null,
        search: ''
    };

    const $ = (id) => document.getElementById(id);
    const apiUrl = (path) => `${contextPath}${path}`;

    document.addEventListener('DOMContentLoaded', initialize);

    async function initialize() {
        if (!$('tmCellSource')) {
            return;
        }

        state.cells = Array.from($('tmCellSource').options)
                .filter((option) => option.value)
                .map((option) => ({
                    id: Number(option.value),
                    cellName: option.dataset.cell || option.textContent.trim(),
                    wingName: option.dataset.wing || 'Unassigned Wing'
                }));

        bindEvents();
        renderWingTree();
        await loadMappings();

        if (state.cells.length) {
            await selectCell(state.cells[0].id);
        } else {
            renderEmptyTree('No active cells found.');
        }
    }

    function bindEvents() {
        $('tmWingTree').addEventListener('click', (event) => {
            const button = event.target.closest('[data-cell-id]');
            if (button) {
                selectCell(Number(button.dataset.cellId));
            }
        });
        $('tmSearchInput').addEventListener('input', (event) => {
            state.search = event.target.value.trim().toLowerCase();
            renderWingTree();
            renderTeamDirectory();
            renderMappingsTable();
        });
        $('tmMappingForm').addEventListener('submit', saveMapping);
        $('tmMappingResetButton').addEventListener('click', resetMappingForm);
        $('tmEmployeeSelect').addEventListener('change', handleEmployeeSelection);
        $('tmMappingsBody').addEventListener('click', handleMappingAction);
    }

    async function selectCell(cellId) {
        const cell = state.cells.find((item) => item.id === cellId);
        if (!cell) {
            return;
        }

        state.selectedCellId = cellId;
        state.teams = [];
        $('tmSelectedWing').textContent = cell.wingName;
        $('tmSelectedCell').textContent = cell.cellName;
        $('tmMappingContext').textContent = `${cell.wingName} / ${cell.cellName}`;
        $('tmEmptyState').classList.add('d-none');
        $('tmCellWorkspace').classList.remove('d-none');
        renderWingTree();
        renderLoadingStates();
        resetMappingForm();

        try {
            state.teams = await request(`/master/team-management/teams-by-cell/${cellId}`);
            renderWorkspace();
        } catch (error) {
            state.teams = [];
            renderWorkspace();
            showAlert(error.message || 'Unable to load teams.', 'danger');
        }
    }

    async function loadMappings() {
        try {
            const mappings = await request('/api/master/employee-team-assignments');
            state.mappings = Array.isArray(mappings) ? mappings : [];
            renderEmployeeOptions();
            renderWorkspace();
        } catch (error) {
            state.mappings = [];
            renderEmployeeOptions();
            showAlert(error.message || 'Unable to load employee mappings.', 'danger');
        }
    }

    async function saveMapping(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const employeeId = numberOrNull($('tmEmployeeSelect').value);
        const teamId = numberOrNull($('tmTeamSelect').value);

        form.classList.add('was-validated');
        if (!employeeId || !teamId || !form.checkValidity()) {
            return;
        }

        setFormBusy(true);
        try {
            await request(`/api/master/employee-team-assignments/${employeeId}`, {
                method: 'PUT',
                body: JSON.stringify({ teamId })
            });
            await loadMappings();
            resetMappingForm();
            showAlert('Employee mapped to the team successfully.', 'success');
        } catch (error) {
            showAlert(error.message || 'Unable to save the mapping.', 'danger');
        } finally {
            setFormBusy(false);
        }
    }

    async function handleMappingAction(event) {
        const button = event.target.closest('[data-action]');
        if (!button) {
            return;
        }

        const employeeId = Number(button.dataset.id);
        if (button.dataset.action === 'edit') {
            $('tmEmployeeSelect').value = String(employeeId);
            handleEmployeeSelection();
            $('tmMappingForm').scrollIntoView({ behavior: 'smooth', block: 'start' });
            return;
        }

        if (button.dataset.action !== 'remove'
                || !window.confirm('Remove this employee from the cell and team?')) {
            return;
        }

        button.disabled = true;
        try {
            await request(`/api/master/employee-team-assignments/${employeeId}`, { method: 'DELETE' });
            await loadMappings();
            resetMappingForm();
            showAlert('Employee team mapping removed successfully.', 'success');
        } catch (error) {
            showAlert(error.message || 'Unable to remove the mapping.', 'danger');
        } finally {
            button.disabled = false;
        }
    }

    function handleEmployeeSelection() {
        const employeeId = numberOrNull($('tmEmployeeSelect').value);
        const mapping = state.mappings.find((item) => item.employeeId === employeeId);
        $('tmEmployeeId').value = employeeId || '';
        $('tmMappingFormTitle').textContent = mapping?.teamId ? 'Update Mapping' : 'Map Employee';

        if (mapping?.cellId === state.selectedCellId
                && state.teams.some((team) => team.id === mapping.teamId)) {
            $('tmTeamSelect').value = String(mapping.teamId);
        } else {
            $('tmTeamSelect').value = '';
        }

        const current = $('tmCurrentAssignment');
        if (mapping?.teamId) {
            current.classList.remove('d-none');
            current.innerHTML = `<strong>Current assignment</strong><span>${escapeHtml(mapping.cellName || '')} / ${escapeHtml(mapping.teamName || '')}</span>`;
        } else {
            current.classList.add('d-none');
            current.textContent = '';
        }
    }

    function resetMappingForm() {
        const form = $('tmMappingForm');
        form.reset();
        form.classList.remove('was-validated');
        $('tmEmployeeId').value = '';
        $('tmMappingFormTitle').textContent = 'Map Employee';
        $('tmCurrentAssignment').classList.add('d-none');
        $('tmCurrentAssignment').textContent = '';
        renderEmployeeOptions();
        renderTeamOptions();
    }

    function renderWorkspace() {
        if (!state.selectedCellId) {
            return;
        }
        renderTeamOptions();
        renderEmployeeOptions();
        renderTeamDirectory();
        renderMappingsTable();
        updateStats();
    }

    function renderWingTree() {
        const grouped = state.cells.reduce((result, cell) => {
            if (!matchesSearch(cell.wingName, cell.cellName)) {
                return result;
            }
            (result[cell.wingName] ||= []).push(cell);
            return result;
        }, {});
        const wings = Object.entries(grouped).sort(([left], [right]) => left.localeCompare(right));
        $('tmTreeSummary').textContent = `${wings.length} ${plural(wings.length, 'wing')}`;

        if (!wings.length) {
            renderEmptyTree('No matching cells found.');
            return;
        }

        $('tmWingTree').innerHTML = wings.map(([wingName, cells]) => `
            <details class="tm-wing-node" open>
                <summary>
                    <span><i class="fa-solid fa-building-columns" aria-hidden="true"></i>${escapeHtml(wingName)}</span>
                    <small>${cells.length}</small>
                </summary>
                <div class="tm-cell-node-list">
                    ${cells.sort((left, right) => left.cellName.localeCompare(right.cellName)).map((cell) => `
                        <button type="button" class="tm-cell-node ${cell.id === state.selectedCellId ? 'is-active' : ''}"
                                data-cell-id="${cell.id}">
                            <span><i class="fa-solid fa-layer-group" aria-hidden="true"></i>${escapeHtml(cell.cellName)}</span>
                        </button>
                    `).join('')}
                </div>
            </details>
        `).join('');
    }

    function renderTeamOptions() {
        const select = $('tmTeamSelect');
        const current = select.value;
        select.innerHTML = '<option value="">Select Team</option>' + state.teams
                .map((team) => `<option value="${team.id}">${escapeHtml(team.teamName)}</option>`)
                .join('');
        select.disabled = !state.teams.length;
        if (state.teams.some((team) => String(team.id) === current)) {
            select.value = current;
        }
    }

    function renderEmployeeOptions() {
        const select = $('tmEmployeeSelect');
        if (!select) {
            return;
        }
        const current = select.value;
        const activeMappings = state.mappings.filter((mapping) => isActive(mapping.employeeStatus));
        select.innerHTML = '<option value="">Select Employee</option>' + activeMappings.map((mapping) => {
            const assignment = mapping.teamId
                    ? ` - ${mapping.cellName || ''} / ${mapping.teamName || ''}`
                    : ' - Unassigned';
            return `<option value="${mapping.employeeId}">${escapeHtml(employeeLabel(mapping) + assignment)}</option>`;
        }).join('');
        if (activeMappings.some((mapping) => String(mapping.employeeId) === current)) {
            select.value = current;
        }
    }

    function renderTeamDirectory() {
        const target = $('tmTeamDirectory');
        if (!target || !state.selectedCellId) {
            return;
        }
        const teams = state.teams.filter((team) => matchesSearch(team.teamName));
        $('tmTeamDirectorySummary').textContent = `${teams.length} ${plural(teams.length, 'team')}`;
        if (!teams.length) {
            target.innerHTML = stateBlock('fa-people-group', 'No teams found for this cell.');
            return;
        }

        target.innerHTML = teams.map((team) => {
            const memberCount = state.mappings.filter((mapping) => mapping.teamId === team.id).length;
            return `
                <div class="tm-team-item">
                    <div>
                        <strong>${escapeHtml(team.teamName)}</strong>
                        <span>${memberCount} ${plural(memberCount, 'employee')}</span>
                    </div>
                    <span class="tm-type-badge">Team</span>
                </div>
            `;
        }).join('');
    }

    function renderMappingsTable() {
        const body = $('tmMappingsBody');
        if (!body || !state.selectedCellId) {
            return;
        }
        const mappings = state.mappings.filter((mapping) =>
            mapping.cellId === state.selectedCellId
            && matchesSearch(mapping.employeeName, mapping.employeeCode, mapping.designationName,
                    mapping.teamName, mapping.cellName));
        $('tmMappingsSummary').textContent = `${mappings.length} ${plural(mappings.length, 'employee')}`;

        if (!mappings.length) {
            body.innerHTML = emptyRow(6, 'No employees are mapped to this cell.');
            return;
        }

        body.innerHTML = mappings.map((mapping, index) => `
            <tr>
                <td class="tm-cell-serial">${index + 1}</td>
                <td>
                    <span class="tm-cell-primary">${escapeHtml(mapping.employeeName || '')}</span>
                    <span class="tm-cell-secondary">${escapeHtml(mapping.employeeCode || '')}</span>
                </td>
                <td>${escapeHtml(mapping.designationName || '-')}</td>
                <td><span class="tm-cell-primary">${escapeHtml(mapping.teamName || '')}</span></td>
                <td class="text-center">
                    <span class="tm-status-badge ${isActive(mapping.employeeStatus) ? 'is-filled' : 'is-vacant'}">
                        ${escapeHtml(mapping.employeeStatus || 'Unknown')}
                    </span>
                </td>
                <td class="tm-cell-actions">
                    <button class="btn btn-sm btn-outline-primary tm-action-btn" type="button"
                            data-action="edit" data-id="${mapping.employeeId}" title="Edit mapping"
                            aria-label="Edit mapping for ${escapeHtml(mapping.employeeName || 'employee')}">
                        <i class="fa-solid fa-pen" aria-hidden="true"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger tm-action-btn" type="button"
                            data-action="remove" data-id="${mapping.employeeId}" title="Remove mapping"
                            aria-label="Remove mapping for ${escapeHtml(mapping.employeeName || 'employee')}">
                        <i class="fa-solid fa-link-slash" aria-hidden="true"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    }

    function updateStats() {
        const mappedToCell = state.mappings.filter((mapping) => mapping.cellId === state.selectedCellId).length;
        const available = state.mappings.filter((mapping) => !mapping.teamId && isActive(mapping.employeeStatus)).length;
        $('tmTeamCount').textContent = state.teams.length;
        $('tmMappedCount').textContent = mappedToCell;
        $('tmAvailableCount').textContent = available;
    }

    function renderLoadingStates() {
        $('tmTeamDirectory').innerHTML = stateBlock('fa-spinner fa-spin', 'Loading teams...');
        $('tmMappingsBody').innerHTML = emptyRow(6, 'Loading employee mappings...', 'fa-spinner fa-spin');
    }

    function renderEmptyTree(message) {
        $('tmWingTree').innerHTML = stateBlock('fa-circle-info', message);
    }

    function setFormBusy(busy) {
        const button = $('tmMappingForm').querySelector('button[type="submit"]');
        button.disabled = busy;
        button.innerHTML = busy
                ? '<i class="fa-solid fa-spinner fa-spin" aria-hidden="true"></i> Saving'
                : '<i class="fa-solid fa-floppy-disk" aria-hidden="true"></i> Save Mapping';
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

    function showAlert(message, type) {
        const alert = $('tmAlert');
        alert.className = `alert alert-${type}`;
        alert.textContent = message;
        window.clearTimeout(showAlert.timer);
        showAlert.timer = window.setTimeout(() => alert.classList.add('d-none'), 5000);
    }

    function stateBlock(icon, message) {
        return `<div class="tm-state-block"><i class="fa-solid ${icon}" aria-hidden="true"></i><span>${escapeHtml(message)}</span></div>`;
    }

    function emptyRow(columns, message, icon = 'fa-inbox') {
        return `<tr class="tm-state-row"><td colspan="${columns}"><div class="tm-table-state"><i class="fa-solid ${icon}" aria-hidden="true"></i><span>${escapeHtml(message)}</span></div></td></tr>`;
    }

    function matchesSearch(...values) {
        return !state.search || values.some((value) => String(value || '').toLowerCase().includes(state.search));
    }

    function employeeLabel(mapping) {
        const code = mapping.employeeCode ? ` (${mapping.employeeCode})` : '';
        const designation = mapping.designationName ? ` - ${mapping.designationName}` : '';
        return `${mapping.employeeName || ''}${code}${designation}`;
    }

    function isActive(status) {
        return String(status || '').toUpperCase() === 'ACTIVE';
    }

    function numberOrNull(value) {
        const number = Number(value);
        return value && Number.isFinite(number) ? number : null;
    }

    function plural(count, singular) {
        return count === 1 ? singular : `${singular}s`;
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
