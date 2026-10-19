(() => {
    'use strict';
    const root = document.getElementById('resourceManagement');
    if (!root) return;
    const element = id => document.getElementById(`rm${id}`);
    const api = root.dataset.api;
    const state = { view: 'mapped', page: 0, size: 12, data: null, request: null, action: null, saving: false };
    let searchTimer;

    function node(tag, className, content) {
        const result = document.createElement(tag);
        if (className) result.className = className;
        if (content != null) result.textContent = content;
        return result;
    }

    async function request(url, options = {}) {
        const response = await fetch(url, { credentials: 'same-origin', ...options });
        const isJson = (response.headers.get('content-type') || '').includes('json');
        if (response.redirected || !isJson) throw new Error('Your session may have expired. Please sign in again.');
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || result.detail || 'Unable to complete the request. Please refresh and try again.');
        return result;
    }

    function feedback(message, error = false) {
        const box = element('Feedback');
        box.textContent = message;
        box.classList.toggle('rm-error', error);
        box.hidden = !message;
    }

    function showState(title, description) {
        const box = element('State');
        box.replaceChildren(node('strong', '', title));
        if (description) box.append(node('span', '', description));
        box.hidden = false;
        element('TableWrap').hidden = true;
    }

    function resourceRow(resource) {
        const row = node('tr');
        const person = node('div', 'rm-person');
        const initials = (resource.name || '?').trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();
        const avatar = node('span', 'rm-avatar', initials);
        avatar.setAttribute('aria-hidden', 'true');
        const identity = node('div');
        identity.append(node('strong', '', resource.name || 'Unnamed resource'),
            node('small', '', [resource.code, resource.designation, resource.employeeType].filter(Boolean).join(' · ') || 'Details unavailable'));
        person.append(avatar, identity);
        const nameCell = node('td');
        nameCell.append(person);
        row.append(nameCell);
        for (const [label, value] of [['Department', resource.department], ['Reporting manager', resource.manager]]) {
            const cell = node('td', '', value || '—');
            cell.dataset.label = label;
            row.append(cell);
        }
        const free = state.view === 'free';
        const status = node('td');
        status.dataset.label = 'Status';
        status.append(node('span', `rm-badge ${free ? 'rm-badge-free' : resource.direct ? '' : 'rm-badge-indirect'}`,
            free ? 'Available' : resource.direct ? 'Direct report' : 'Subordinate’s resource'));
        const action = node('td');
        const actions = node('div', 'rm-row-actions');
        if (!free) {
            const reassign = node('button', 'rm-button rm-outline', 'Change reporting manager');
            reassign.type = 'button';
            reassign.dataset.employeeId = resource.employeeId;
            reassign.dataset.action = 'reassign';
            reassign.setAttribute('aria-label', `Change reporting manager for ${resource.name}`);
            actions.append(reassign);
        }
        const button = node('button', `rm-button ${free ? 'rm-primary' : 'rm-release'}`, free ? 'Map resource' : 'Release');
        button.type = 'button';
        button.dataset.employeeId = resource.employeeId;
        button.dataset.action = free ? 'map' : 'release';
        button.setAttribute('aria-label', `${free ? 'Map' : 'Release'} ${resource.name}`);
        actions.append(button);
        action.append(actions);
        row.append(status, action);
        return row;
    }

    function render(data) {
        state.data = data;
        state.page = data.page;
        for (const [id, value] of [['MappedCount', data.mappedCount], ['DirectCount', data.directCount], ['FreeCount', data.freeCount]]) {
            element(id).textContent = value.toLocaleString();
        }
        const rows = document.createDocumentFragment();
        data.resources.forEach(resource => rows.append(resourceRow(resource)));
        element('Rows').replaceChildren(rows);
        element('State').hidden = data.resources.length > 0;
        element('TableWrap').hidden = data.resources.length === 0;
        if (!data.resources.length) {
            const searching = element('Search').value.trim().length > 0;
            showState(searching ? 'No matching resources' : state.view === 'free' ? 'No free resources right now' : 'Your team is ready to grow',
                searching ? 'Try another name, employee code or department.' : state.view === 'free'
                    ? 'Refresh later to check for newly available resources.' : 'Open Free resources to assign a person to yourself or your team.');
        }
        const start = data.total ? data.page * data.size + 1 : 0;
        element('Range').textContent = `${start}–${Math.min((data.page + 1) * data.size, data.total)} of ${data.total} resources`;
        element('Previous').disabled = data.page === 0;
        element('Next').disabled = (data.page + 1) * data.size >= data.total;
    }

    async function load() {
        state.request?.abort();
        const controller = new AbortController();
        state.request = controller;
        root.setAttribute('aria-busy', 'true');
        element('Previous').disabled = true;
        element('Next').disabled = true;
        showState('Loading resources…');
        const params = new URLSearchParams({ view: state.view, q: element('Search').value.trim(), page: state.page, size: state.size });
        if (state.view === 'free') params.set('employeeType', element('EmployeeType').value);
        try {
            const data = await request(`${api}?${params}`, { signal: controller.signal });
            if (state.request === controller) render(data);
        } catch (error) {
            if (error.name !== 'AbortError' && state.request === controller) {
                showState('Resources could not be loaded', error.message);
                element('Range').textContent = '';
            }
        } finally {
            if (state.request === controller) root.setAttribute('aria-busy', 'false');
        }
    }

    function changeView(view) {
        clearTimeout(searchTimer);
        state.view = view;
        state.page = 0;
        element('TypeFilter').hidden = view !== 'free';
        root.querySelectorAll('[data-view]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.view === view)));
        element('Heading').textContent = view === 'free' ? 'Available resources' : 'Your reporting team';
        element('Description').textContent = view === 'free'
            ? 'People with no reporting assignment. Choose a resource to grow your team.'
            : 'Use Change reporting manager to assign a team resource directly to yourself or a subordinate.';
        load();
    }

    function openAction(resource, type) {
        state.action = { resource, type };
        const mapping = type !== 'release';
        const label = type === 'reassign' ? 'Change reporting manager' : mapping ? 'Map resource' : 'Release resource';
        element('DialogTitle').textContent = label;
        element('DialogDescription').textContent = mapping
            ? `Choose who ${resource.name} will report to directly: yourself or any subordinate in your reporting team.`
            : `Release ${resource.name} from ${resource.manager || 'their reporting manager'}? This removes their primary assignment. Their own team stays assigned to them. Other reporting assignments remain in place.`;
        element('ManagerField').hidden = !mapping;
        element('Manager').required = mapping;
        element('Manager').replaceChildren();
        for (const manager of state.data.managers) {
            if (manager.employeeId === resource.employeeId) continue;
            const option = node('option', '', manager.name);
            option.value = manager.employeeId;
            element('Manager').append(option);
        }
        if (type === 'reassign' && resource.managerEmployeeId != null) {
            element('Manager').value = String(resource.managerEmployeeId);
        }
        element('DialogError').hidden = true;
        element('Confirm').textContent = type === 'reassign' ? 'Save reporting manager' : label;
        element('Confirm').classList.toggle('rm-primary', mapping);
        element('Confirm').classList.toggle('rm-release', !mapping);
        element('Dialog').showModal();
    }

    async function submit(event) {
        event.preventDefault();
        if (state.saving || !state.action) return;
        state.saving = true;
        element('Confirm').disabled = true;
        element('Cancel').disabled = true;
        element('Manager').disabled = true;
        element('DialogError').hidden = true;
        const { resource, type } = state.action;
        const originalLabel = element('Confirm').textContent;
        element('Confirm').textContent = 'Saving…';
        const headers = { 'Content-Type': 'application/json' };
        const csrf = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        if (csrf && csrfHeader) headers[csrfHeader] = csrf;
        try {
            const result = await request(`${api}/${resource.employeeId}/${type}`, {
                method: 'POST', headers,
                ...(type !== 'release' ? { body: JSON.stringify({ managerEmployeeId: Number(element('Manager').value) }) } : {})
            });
            element('Dialog').close();
            feedback(result.message);
            await load();
        } catch (error) {
            element('DialogError').textContent = error.message;
            element('DialogError').hidden = false;
        } finally {
            state.saving = false;
            element('Confirm').disabled = false;
            element('Cancel').disabled = false;
            element('Manager').disabled = false;
            element('Confirm').textContent = originalLabel;
        }
    }

    root.querySelectorAll('[data-view]').forEach(button => button.addEventListener('click', () => changeView(button.dataset.view)));
    element('Browse').addEventListener('click', () => { element('Search').value = ''; changeView('free'); });
    element('Refresh').addEventListener('click', () => { clearTimeout(searchTimer); load(); });
    element('EmployeeType').addEventListener('change', () => {
        clearTimeout(searchTimer);
        state.page = 0;
        load();
    });
    element('Search').addEventListener('input', () => {
        clearTimeout(searchTimer);
        state.page = 0;
        searchTimer = setTimeout(load, 300);
    });
    element('Previous').addEventListener('click', () => { state.page--; load(); });
    element('Next').addEventListener('click', () => { state.page++; load(); });
    element('Rows').addEventListener('click', event => {
        const button = event.target.closest('button[data-employee-id]');
        if (!button || state.saving) return;
        const resource = state.data?.resources.find(item => String(item.employeeId) === button.dataset.employeeId);
        if (resource) openAction(resource, button.dataset.action);
    });
    element('Cancel').addEventListener('click', () => element('Dialog').close());
    element('Dialog').addEventListener('cancel', event => { if (state.saving) event.preventDefault(); });
    element('ActionForm').addEventListener('submit', submit);
    load();
})();
