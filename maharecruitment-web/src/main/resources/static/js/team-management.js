(() => {
    const contextPath = document.querySelector('meta[name="app-context-path"]')?.content || '';
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    const state = {
        projects: [],
        cells: [],
        teams: [],
        positionTeams: [],
        positions: [],
        designations: [],
        levels: [],
        employees: [],
        positionEmployeeRequestId: 0,
        mappingEmployees: [],
        mappingEmployeeRequestId: 0
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
        await refreshAll();
    });

    function bindEvents() {
        $('tmCellFilter').addEventListener('change', refreshAll);
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
                    const target = event.target.getAttribute('data-bs-target');
                    if (target === '#tmTree') {
                        await loadTree();
                    } else if (target === '#tmChart') {
                        await loadChart();
                    } else if (target === '#tmPositions') {
                        await loadPositions();
                        populatePositionReportingSelect();
                        populateMappingPositionSelect();
                    }
                } catch (error) {
                    showAlert(error.message, 'danger');
                }
            });
        });
    }

    async function loadLookups() {
        const [projects, cells, designations] = await Promise.all([
            request('/api/master/organization-hierarchy/options/projects'),
            request('/api/master/organization-hierarchy/options/cells'),
            request('/api/master/organization-hierarchy/options/designations')
        ]);
        state.projects = projects || [];
        state.cells = cells || [];
        state.designations = designations || [];
        populateCellSelects();
        populateDesignationSelect();
        await populatePositionLevelSelect();
        resetPositionEmployeeSelect('Select Designation and Level First');
        resetMappingEmployeeSelect('Select Position First');
    }

    async function refreshAll() {
        const results = await Promise.allSettled([
            loadDashboard(),
            loadTeams(),
            loadPositions(),
            loadMappings(),
            loadTree(),
            loadChart()
        ]);
        populateTeamSelects(valueOrNull($('teamCellId').value));
        populatePositionReportingSelect();
        populateMappingTeamSelect();
        populateMappingPositionSelect();

        const failedResult = results.find((result) => result.status === 'rejected');
        if (failedResult) {
            showAlert(failedResult.reason?.message || 'Some team-management data could not be loaded.', 'danger');
        }
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

    async function loadDashboard() {
        try {
            const cellId = selectedCellId();
            const dashboard = await request(`/api/master/organization-hierarchy/dashboard${query({ cellId })}`);
            $('tmTotalPositions').textContent = dashboard.totalPositions || 0;
            $('tmFilledPositions').textContent = dashboard.filledPositions || 0;
            $('tmVacantPositions').textContent = dashboard.vacantPositions || 0;
            renderStrength(dashboard.teamStrength || []);
        } catch (error) {
            showAlert(error.message, 'danger');
        }
    }

    async function loadTeams() {
        try {
            const page = await request(`/api/master/teams${query({ cellId: selectedCellId(), size: 500 })}`);
            state.teams = pageItems(page);
            renderTeamsTable();
        } catch (error) {
            state.teams = [];
            renderTableError($('tmTeamsBody'), 4, 'Unable to load teams.');
            throw error;
        }
    }

    async function loadPositions() {
        const body = $('tmPositionsBody');
        body.innerHTML = loadingRow(6, 'Loading positions...');
        try {
            const page = await request(`/api/master/positions${query({
                cellId: selectedCellId(),
                size: 300
            })}`);
            state.positions = pageItems(page);
            renderPositionsTable();
        } catch (error) {
            state.positions = [];
            renderTableError(body, 6, 'Unable to load positions.');
            throw error;
        }
    }

    async function loadMappings() {
        try {
            const page = await request(`/api/master/employee-team-mappings${query({
                cellId: selectedCellId(),
                size: 200
            })}`);
            renderMappingsTable(pageItems(page));
        } catch (error) {
            renderTableError($('tmMappingsBody'), 5, 'Unable to load employee mappings.');
            throw error;
        }
    }

    async function loadTree() {
        const cellId = selectedCellId();
        const tree = await request(`/api/master/organization-hierarchy/tree${query({ cellId })}`);
        renderHierarchy($('tmTreeRoot'), tree, false);
    }

    async function loadChart() {
        const cellId = selectedCellId();
        const chart = await request(`/api/master/organization-hierarchy/chart${query({ cellId })}`);
        renderHierarchy($('tmChartRoot'), chart, true);
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
        const projectId = state.projects.find((project) => String(project.cellId) === String(cellId))?.id;
        if (!projectId) {
            showAlert('The selected cell has no active project for sample hierarchy generation.', 'warning');
            return;
        }
        try {
            await request(`/api/master/organization-hierarchy/projects/${projectId}/seed-sample`, { method: 'POST' });
            showAlert('Sample hierarchy generated successfully.', 'success');
            await loadLookups();
            await refreshAll();
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
            await refreshAll();
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
            await refreshAll();
        } catch (error) {
            showAlert(error.message, 'danger');
        }
    }

    function renderStrength(rows) {
        const body = $('tmStrengthBody');
        if (!rows.length) {
            body.innerHTML = emptyRow(4);
            return;
        }
        body.innerHTML = rows.map((row) => `
            <tr>
                <td>${escapeHtml(row.teamName)}</td>
                <td>${escapeHtml(row.teamType)}</td>
                <td class="text-end">${row.filledPositions || 0}/${row.totalPositions || 0}</td>
                <td class="text-end text-danger fw-semibold">${row.vacantPositions || 0}</td>
            </tr>
        `).join('');
    }

    function renderTeamsTable() {
        const body = $('tmTeamsBody');
        if (!state.teams.length) {
            body.innerHTML = emptyRow(4);
            return;
        }
        body.innerHTML = state.teams.map((team) => `
            <tr>
                <td>${escapeHtml(team.teamName)}</td>
                <td>${escapeHtml(team.cellName || '-')}</td>
                <td>${escapeHtml(team.teamType)}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-primary" data-action="edit-team" data-id="${team.teamId}" title="Edit">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" data-action="deactivate-team" data-id="${team.teamId}" title="Deactivate">
                        <i class="fa-solid fa-ban"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    }

    function renderPositionsTable() {
        const body = $('tmPositionsBody');
        if (!state.positions.length) {
            body.innerHTML = emptyRow(6);
            return;
        }
        body.innerHTML = state.positions.map((position) => `
            <tr>
                <td>
                    <div class="fw-semibold">${escapeHtml(position.positionName)}</div>
                    <small class="text-muted">${escapeHtml(designationLevel(position))}</small>
                </td>
                <td>${escapeHtml(position.cellName || '-')}</td>
                <td>${escapeHtml(position.teamName || '-')}</td>
                <td class="${position.positionStatus === 'VACANT' ? 'text-danger fw-semibold' : ''}">
                    ${escapeHtml(position.displayName)}
                </td>
                <td>${badge(position.positionStatus, position.positionStatus === 'VACANT' ? 'danger' : 'success')}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-primary" data-action="edit-position" data-id="${position.positionId}" title="Edit">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" data-action="deactivate-position" data-id="${position.positionId}" title="Deactivate">
                        <i class="fa-solid fa-ban"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    }

    function renderMappingsTable(rows) {
        const body = $('tmMappingsBody');
        if (!rows.length) {
            body.innerHTML = emptyRow(5);
            return;
        }
        body.innerHTML = rows.map((mapping) => `
            <tr>
                <td>${escapeHtml(mapping.employeeName || 'Vacant')}</td>
                <td>${escapeHtml(mapping.teamName)}</td>
                <td>
                    <div class="fw-semibold">${escapeHtml(mapping.positionName)}</div>
                    <small class="text-muted">${escapeHtml(designationLevel(mapping))}</small>
                </td>
                <td>${escapeHtml(mapping.effectiveDate)}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-primary" data-action="edit-mapping" data-id="${mapping.mappingId}" title="Edit">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" data-action="deactivate-mapping" data-id="${mapping.mappingId}" title="Deactivate">
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

    function badge(label, type) {
        return `<span class="badge text-bg-${type}">${escapeHtml(label)}</span>`;
    }

    function emptyRow(colspan) {
        return `<tr><td colspan="${colspan}" class="text-center text-muted">No records found</td></tr>`;
    }

    function loadingRow(colspan, message) {
        return `<tr><td colspan="${colspan}" class="text-center text-muted">${escapeHtml(message)}</td></tr>`;
    }

    function renderTableError(body, colspan, message) {
        body.innerHTML = `<tr><td colspan="${colspan}" class="text-center text-danger">${escapeHtml(message)}</td></tr>`;
    }

    function renderInitialLoadingState() {
        $('tmTeamsBody').innerHTML = loadingRow(4, 'Loading teams...');
        $('tmPositionsBody').innerHTML = loadingRow(6, 'Loading positions...');
        $('tmMappingsBody').innerHTML = loadingRow(5, 'Loading employee mappings...');
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
