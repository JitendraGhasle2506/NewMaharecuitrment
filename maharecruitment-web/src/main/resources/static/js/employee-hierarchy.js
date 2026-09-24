(() => {
    'use strict';
    const page = document.getElementById('employeeHierarchy');
    if (!page) return;
    const $ = id => document.getElementById(id);
    const api = page.dataset.api;
    const context = page.dataset.context.replace(/\/$/, '');
    const viewport = $('ehViewport');
    const stage = $('ehStage');
    const surface = $('ehSurface');
    const cards = $('ehCards');
    const lines = $('ehLines');
    const results = $('ehResults');
    const styles = getComputedStyle(page);
    const CARD_WIDTH = parseFloat(styles.getPropertyValue('--eh-card-width')) || 256;
    const CARD_HEIGHT = parseFloat(styles.getPropertyValue('--eh-card-height')) || 232;
    const GAP_X = 32, GAP_Y = 72, PADDING = 44, CONTROL_SPACE = 88;
    const compactScreen = window.matchMedia('(max-width: 767px)');
    let root = null, scale = 1, chartWidth = 0, chartHeight = 0, leftInset = 0;
    let selectedId = null, generation = 0, queryGeneration = 0, searchController = null;
    let filters = null, displayed = new Map(), busy = new Set(), fitMode = false;
    let searchTimer = null, optionsReady = false, filtersManuallyToggled = false, hierarchyController = null;

    function status(message, error = false) {
        $('ehStatus').textContent = message;
        $('ehStatus').classList.toggle('eh-error', error);
    }

    function emptyState(state = 'idle', message = '') {
        const copy = {
            idle: ['YOUR ORGANIZATION, CONNECTED', 'Every great team starts with a connection',
                'Select a head of department above to explore employees and their reporting relationships.'],
            loading: ['BUILDING YOUR CHART', 'Connecting your team…', 'Loading active employees and their reporting relationships.'],
            error: ['LET’S TRY THAT AGAIN', 'The hierarchy could not be loaded', message],
            unavailable: ['NO HOD AVAILABLE', 'Your hierarchy starts with a HOD',
                'Link a HOD user to an active employee record and configure their reporting mappings.']
        }[state];
        $('ehEmpty').classList.toggle('is-loading', state === 'loading');
        $('ehEmpty').classList.toggle('is-error', state === 'error');
        $('ehEmptyEyebrow').textContent = copy[0];
        $('ehEmptyTitle').textContent = copy[1];
        $('ehEmptyText').textContent = copy[2];
        $('ehRetry').hidden = state !== 'error';
    }

    function filterVisibility(open) {
        $('ehFilterFields').hidden = !open;
        $('ehFilterToggle').setAttribute('aria-expanded', String(open));
    }

    function filterSummary() {
        const count = Number(Boolean($('ehDepartment').value)) + Number(Boolean($('ehDesignation').value))
            + Number($('ehType').value !== 'PRIMARY');
        $('ehFilterCount').textContent = count;
        $('ehFilterCount').hidden = !count;
        $('ehResetFilters').disabled = !count;
        $('ehShow').disabled = !$('ehHod').value;
    }

    function summary(visible = 0, levels = 0) {
        $('ehVisibleCount').textContent = root ? visible : '—';
        $('ehReportsCount').textContent = root ? root.totalChildren : '—';
        $('ehLevelsCount').textContent = root ? levels : '—';
        $('ehScope').textContent = root ? `${root.employeeName} · ${$('ehType').selectedOptions[0].text} reporting`
            : 'Choose a HOD to explore their team';
        $('ehScope').title = $('ehScope').textContent;
        ['ehZoomIn', 'ehZoomOut', 'ehReset', 'ehFit', 'ehSearch', 'ehSearchButton'].forEach(id => { $(id).disabled = !root; });
        $('ehClear').hidden = !selectedId && !$('ehSearch').value;
    }

    async function get(url, signal) {
        const response = await fetch(url, { signal, headers: { Accept: 'application/json' }, credentials: 'same-origin' });
        if (response.redirected || !(response.headers.get('content-type') || '').includes('application/json')) {
            throw new Error('Your session may have expired. Sign in and reload this page.');
        }
        if (!response.ok) {
            if (response.status === 403) throw new Error('You need HR access to view this hierarchy.');
            if (response.status === 404) throw new Error('This employee or branch is no longer available. Reload the hierarchy.');
            throw new Error('Unable to load the hierarchy. Please try again.');
        }
        const body = await response.json();
        if (!body.success) throw new Error('Unable to load the hierarchy. Please try again.');
        return body.data;
    }

    function url(extra = {}, suffix = '') {
        const parameters = new URLSearchParams(filters);
        const rootId = parameters.get('rootId');
        parameters.delete('rootId');
        Object.entries(extra).forEach(([key, value]) => parameters.set(key, value));
        return `${api}/${encodeURIComponent(rootId)}${suffix}?${parameters}`;
    }

    function decorate(node) {
        const stack = [node];
        while (stack.length) {
            const current = stack.pop();
            current.children = current.children || [];
            current.expanded = current.children.length > 0;
            stack.push(...current.children);
        }
        return node;
    }

    function cancelSearch() {
        window.clearTimeout(searchTimer);
        queryGeneration++;
        if (searchController) searchController.abort();
        $('ehSearchForm').setAttribute('aria-busy', 'false');
        results.hidden = true;
        results.replaceChildren();
    }

    async function load() {
        const current = ++generation;
        cancelSearch();
        if (hierarchyController) hierarchyController.abort();
        selectedId = null;
        $('ehSearch').value = '';
        busy = new Set();
        filters = { rootId: $('ehHod').value, reportingType: $('ehType').value };
        if ($('ehDepartment').value) filters.departmentId = $('ehDepartment').value;
        if ($('ehDesignation').value) filters.designationId = $('ehDesignation').value;
        filterSummary();
        root = null;
        render();
        viewport.setAttribute('aria-busy', 'false');
        if (!filters.rootId) {
            emptyState();
            status('Select a HOD to view their reporting hierarchy.');
            return;
        }
        viewport.setAttribute('aria-busy', 'true');
        emptyState('loading');
        $('ehShow').disabled = true;
        hierarchyController = new AbortController();
        status('Loading the reporting hierarchy…');
        try {
            const loaded = decorate(await get(url(), hierarchyController.signal));
            if (current !== generation) return;
            root = loaded;
            root.expanded = true;
            render();
            initialView();
            status(root.totalChildren ? 'Expand an employee to explore their direct reports.'
                : 'No subordinate is available for this employee and the selected filters.');
        } catch (error) {
            if (current === generation && error.name !== 'AbortError') {
                root = null; render(); emptyState('error', error.message); status(error.message, true);
            }
        } finally {
            if (current === generation) {
                viewport.setAttribute('aria-busy', 'false');
                $('ehShow').disabled = !$('ehHod').value;
            }
        }
    }

    function element(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
    }

    function button(text, action, id, label) {
        const control = element('button', '', text);
        control.type = 'button';
        control.dataset.action = action;
        control.dataset.id = id;
        control.setAttribute('aria-label', label);
        control.disabled = busy.has(id);
        return control;
    }

    function card(node, x, y) {
        const article = element('article', 'eh-card');
        article.style.left = `${x}px`;
        article.style.top = `${y}px`;
        article.dataset.id = node.employeeId;
        article.tabIndex = -1;
        article.setAttribute('aria-label', `${node.employeeName}, ${node.designation || 'Designation not set'}, ${node.totalChildren} direct reports`);
        article.classList.toggle('eh-root', node === root);
        article.classList.toggle('eh-found', node.employeeId === selectedId);
        article.classList.toggle('eh-ancestor', !node.filterMatch);
        const initials = (node.employeeName || '?').trim().split(/\s+/).slice(0, 2).map(word => word[0]).join('').toUpperCase();
        const avatar = element('div', `eh-avatar eh-avatar-${node.employeeId % 4}`, initials);
        avatar.setAttribute('aria-hidden', 'true');
        if (node.profilePhoto) {
            const photo = element('img');
            photo.alt = '';
            photo.loading = 'lazy';
            // Only this application's photo proxy is accepted; no remote image URLs or filesystem paths.
            const expected = `/api/employees/hierarchy/photo/${node.employeeId}`;
            if (node.profilePhoto === expected) photo.src = context + expected;
            photo.addEventListener('error', () => photo.remove(), { once: true });
            avatar.append(photo);
        }
        const name = element('h3', '', node.employeeName || 'Employee');
        name.title = name.textContent;
        const designation = element('div', 'eh-designation', node.designation || 'Designation not set');
        designation.title = designation.textContent;
        const code = element('span', 'eh-code', node.employeeCode || 'Code not set');
        code.title = code.textContent;
        const top = element('div', 'eh-card-top');
        top.append(element('span', 'eh-node-badge', node === root ? 'SELECTED HOD'
            : node.employeeId === selectedId ? 'SEARCH MATCH' : 'TEAM MEMBER'), code);
        const identity = element('div', 'eh-identity');
        const identityCopy = element('div', 'eh-identity-copy');
        identityCopy.append(name, designation);
        identity.append(avatar, identityCopy);
        const department = element('div', 'eh-department', node.department || 'Department not set');
        department.title = department.textContent;
        article.append(top, identity, department);
        if (!node.filterMatch) article.title = 'Connecting manager retained for the selected filters';
        const footer = element('div', 'eh-card-footer');
        if (node.totalChildren) {
            const toggle = button(busy.has(node.employeeId) ? 'Loading…'
                : `${node.expanded ? '−' : '+'} ${node.totalChildren} direct report${node.totalChildren === 1 ? '' : 's'}`, 'toggle', node.employeeId,
                `${node.expanded ? 'Collapse' : 'Expand'} reports of ${node.employeeName}`);
            toggle.setAttribute('aria-expanded', String(node.expanded));
            footer.append(toggle);
            if (node.expanded && node.nextOffset !== null) {
                footer.append(button(busy.has(node.employeeId) ? 'Loading…' : 'Load more', 'more', node.employeeId,
                    `Load more reports of ${node.employeeName}`));
            }
        } else footer.append(element('span', 'eh-leaf', 'No direct reports'));
        article.append(footer);
        return article;
    }

    // Subtree widths reserve a distinct horizontal interval for every branch.
    // All nodes at a depth share the same row; connector buses stay in the gap between rows.
    function render() {
        const focused = document.activeElement;
        const focusedId = focused && focused.dataset.id;
        const focusedAction = focused && focused.dataset.action;
        cards.replaceChildren();
        lines.replaceChildren();
        displayed = new Map();
        $('ehEmpty').hidden = Boolean(root);
        if (!root) {
            chartWidth = chartHeight = 0;
            stage.style.width = stage.style.height = '0px';
            surface.style.width = surface.style.height = '0px';
            $('ehCount').textContent = '0 employees displayed';
            scale = 1;
            $('ehZoom').value = '100%';
            summary();
            return;
        }
        const ordered = [];
        const stack = [{ node: root, depth: 0 }];
        while (stack.length) {
            const entry = stack.pop();
            entry.children = entry.node.expanded ? entry.node.children : [];
            ordered.push(entry);
            displayed.set(entry.node.employeeId, entry);
            for (let index = entry.children.length - 1; index >= 0; index--) {
                stack.push({ node: entry.children[index], depth: entry.depth + 1 });
            }
        }
        for (let index = ordered.length - 1; index >= 0; index--) {
            const entry = ordered[index];
            entry.width = Math.max(CARD_WIDTH, entry.children.reduce((width, child) =>
                width + displayed.get(child.employeeId).width, 0) + Math.max(0, entry.children.length - 1) * GAP_X);
        }
        ordered[0].start = PADDING;
        let maxDepth = 0;
        const fragment = document.createDocumentFragment();
        for (const entry of ordered) {
            entry.x = entry.start + entry.width / 2;
            entry.y = PADDING + entry.depth * (CARD_HEIGHT + GAP_Y);
            let childStart = entry.start;
            for (const child of entry.children) {
                const childEntry = displayed.get(child.employeeId);
                childEntry.start = childStart;
                childStart += childEntry.width + GAP_X;
            }
            maxDepth = Math.max(maxDepth, entry.depth);
            fragment.append(card(entry.node, entry.x - CARD_WIDTH / 2, entry.y));
        }
        for (const entry of ordered) {
            if (!entry.children.length) continue;
            const childEntries = entry.children.map(child => displayed.get(child.employeeId));
            const bottom = entry.y + CARD_HEIGHT, bus = bottom + GAP_Y / 2;
            let path = `M ${entry.x} ${bottom} V ${bus} M ${childEntries[0].x} ${bus} H ${childEntries.at(-1).x}`;
            for (const child of childEntries) path += ` M ${child.x} ${bus} V ${child.y}`;
            const connector = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            if (entry.node === root) connector.classList.add('eh-root-line');
            connector.setAttribute('d', path);
            lines.append(connector);
        }
        cards.append(fragment);
        chartWidth = ordered[0].width + PADDING * 2;
        chartHeight = (maxDepth + 1) * CARD_HEIGHT + maxDepth * GAP_Y + PADDING * 2;
        stage.style.width = `${chartWidth}px`;
        stage.style.height = `${chartHeight}px`;
        lines.setAttribute('width', chartWidth);
        lines.setAttribute('height', chartHeight);
        $('ehCount').textContent = `${ordered.length} employee${ordered.length === 1 ? '' : 's'} displayed`;
        summary(ordered.length, maxDepth + 1);
        applyScale();
        if (focusedId) {
            const selector = focusedAction ? `button[data-id="${focusedId}"][data-action="${focusedAction}"]`
                : `.eh-card[data-id="${focusedId}"]`;
            cards.querySelector(selector)?.focus({ preventScroll: true });
        }
    }

    function applyScale() {
        leftInset = Math.max(0, (viewport.clientWidth - chartWidth * scale) / 2);
        stage.style.left = `${leftInset}px`;
        stage.style.transform = `scale(${scale})`;
        surface.style.width = `${Math.max(viewport.clientWidth, chartWidth * scale)}px`;
        surface.style.height = `${Math.max(viewport.clientHeight, chartHeight * scale + CONTROL_SPACE)}px`;
        $('ehZoom').value = `${Math.round(scale * 100)}%`;
    }

    function zoom(next) {
        if (!root) return;
        const centerX = (viewport.scrollLeft + viewport.clientWidth / 2 - leftInset) / scale;
        const centerY = (viewport.scrollTop + viewport.clientHeight / 2) / scale;
        scale = Math.max(.02, Math.min(2, next));
        fitMode = false;
        applyScale();
        viewport.scrollLeft = centerX * scale + leftInset - viewport.clientWidth / 2;
        viewport.scrollTop = centerY * scale - viewport.clientHeight / 2;
    }

    function fit() {
        if (!root) return;
        scale = Math.min(1, Math.max(.001, Math.min((viewport.clientWidth - 20) / chartWidth,
            (viewport.clientHeight - CONTROL_SPACE) / chartHeight)));
        fitMode = true;
        applyScale();
        viewport.scrollLeft = viewport.scrollTop = 0;
    }

    function initialView(focusId = null) {
        fit();
        // Keep cards readable on phones and wide trees; exact overview is always available via Fit.
        if (scale < (compactScreen.matches ? 1 : .85)) {
            zoom(compactScreen.matches ? 1 : .85);
            const entry = displayed.get(focusId || root.employeeId);
            viewport.scrollLeft = entry.x * scale + leftInset - viewport.clientWidth / 2;
            viewport.scrollTop = focusId ? Math.max(0, entry.y * scale - viewport.clientHeight / 3) : 0;
        }
    }

    async function loadReports(node) {
        if (busy.has(node.employeeId)) return;
        const current = generation;
        const existingEntry = displayed.get(node.employeeId);
        const screenX = existingEntry.x * scale + leftInset - viewport.scrollLeft;
        busy.add(node.employeeId);
        render();
        try {
            const branch = decorate(await get(url({ nodeId: node.employeeId, offset: node.nextOffset || 0 })));
            if (current !== generation) return;
            const byId = new Map(node.children.map(child => [child.employeeId, child]));
            branch.children.forEach(child => { if (!byId.has(child.employeeId)) byId.set(child.employeeId, child); });
            node.children = [...byId.values()];
            node.totalChildren = branch.totalChildren;
            node.nextOffset = branch.nextOffset;
            node.expanded = true;
            fitMode = false;
            status(node.totalChildren ? `Showing ${node.children.length} of ${node.totalChildren} direct reports of ${node.employeeName}.`
                : 'No subordinate is available for this employee.');
        } catch (error) {
            if (current === generation) status(error.message, true);
        } finally {
            if (current === generation) {
                busy.delete(node.employeeId);
                render();
                const updatedEntry = displayed.get(node.employeeId);
                if (updatedEntry) viewport.scrollLeft = updatedEntry.x * scale + leftInset - screenX;
            }
        }
    }

    cards.addEventListener('click', event => {
        const control = event.target.closest('button[data-action]');
        if (!control) return;
        const node = displayed.get(Number(control.dataset.id))?.node;
        if (!node || busy.has(node.employeeId)) return;
        if (control.dataset.action === 'more' || (!node.expanded && !node.children.length)) {
            loadReports(node);
        } else {
            node.expanded = !node.expanded;
            fitMode = false;
            render();
        }
    });

    async function search(event) {
        event?.preventDefault();
        if (!root) { status('Select a HOD and load a hierarchy before searching.'); return; }
        const term = $('ehSearch').value.trim();
        if (term.length < 2) { status('Enter at least 2 characters to search by name or employee code.'); return; }
        cancelSearch();
        const currentQuery = queryGeneration, current = generation;
        searchController = new AbortController();
        $('ehSearchForm').setAttribute('aria-busy', 'true');
        $('ehClear').hidden = false;
        status('Searching all levels, including collapsed branches…');
        try {
            const matches = await get(url({ q: term }, '/search'), searchController.signal);
            if (current !== generation || currentQuery !== queryGeneration) return;
            results.replaceChildren();
            for (const match of matches) {
                const result = element('button');
                result.append(element('span', '', match.employeeName));
                if (match.employeeCode) result.append(element('small', '', match.employeeCode));
                result.type = 'button';
                result.addEventListener('click', () => showMatch(match));
                results.append(result);
            }
            if (!matches.length) results.append(element('p', '', 'No matching employee in this hierarchy.'));
            results.hidden = false;
            status(matches.length === 20 ? 'Showing the first 20 matches. Refine your search for more specific results.'
                : `${matches.length} matching employee${matches.length === 1 ? '' : 's'} found.`);
        } catch (error) {
            if (error.name !== 'AbortError' && current === generation && currentQuery === queryGeneration) status(error.message, true);
        } finally {
            if (currentQuery === queryGeneration) $('ehSearchForm').setAttribute('aria-busy', 'false');
        }
    }

    function showMatch(match) {
        generation++; // Discard any branch response belonging to the previous view.
        busy = new Set();
        selectedId = match.employeeId;
        const path = match.path.map(decorate);
        for (let index = 0; index < path.length - 1; index++) {
            path[index].children = [path[index + 1]];
            path[index].expanded = true;
        }
        root = path[0];
        results.hidden = true;
        render();
        initialView(match.employeeId);
        status(`Highlighted ${match.employeeName}. Showing the reporting path; use Load more for other reports, or Clear to restore the hierarchy.`);
        cards.querySelector(`.eh-card[data-id="${match.employeeId}"]`)?.focus({ preventScroll: true });
    }

    $('ehFilters').addEventListener('submit', event => { event.preventDefault(); load(); });
    $('ehFilters').addEventListener('change', load);
    $('ehSearchForm').addEventListener('submit', search);
    $('ehSearch').addEventListener('input', () => {
        cancelSearch();
        const term = $('ehSearch').value.trim();
        $('ehClear').hidden = !term && !selectedId;
        if (!term && selectedId) load();
        else if (term.length >= 2) searchTimer = window.setTimeout(() => search(), 350);
    });
    $('ehClear').addEventListener('click', load);
    $('ehZoomIn').addEventListener('click', () => zoom(scale * 1.25));
    $('ehZoomOut').addEventListener('click', () => zoom(scale / 1.25));
    $('ehFit').addEventListener('click', fit);
    $('ehReset').addEventListener('click', () => {
        zoom(1);
        viewport.scrollTop = 0;
        viewport.scrollLeft = Math.max(0, chartWidth / 2 + leftInset - viewport.clientWidth / 2);
    });
    $('ehSearchForm').addEventListener('keydown', event => {
        if (event.key === 'Escape') { cancelSearch(); $('ehSearch').focus(); }
        if (results.hidden || !['ArrowDown', 'ArrowUp'].includes(event.key)) return;
        const choices = [...results.querySelectorAll('button')];
        if (!choices.length) return;
        event.preventDefault();
        const index = choices.indexOf(document.activeElement);
        const next = index < 0 ? (event.key === 'ArrowDown' ? 0 : choices.length - 1)
            : (index + (event.key === 'ArrowDown' ? 1 : -1) + choices.length) % choices.length;
        choices[next].focus();
    });
    document.addEventListener('click', event => {
        if (!$('ehSearchForm').contains(event.target)) results.hidden = true;
    });
    $('ehFilterToggle').addEventListener('click', () => {
        filtersManuallyToggled = true;
        filterVisibility($('ehFilterFields').hidden);
    });
    filterVisibility(!compactScreen.matches);
    compactScreen.addEventListener('change', () => {
        if (!filtersManuallyToggled) filterVisibility(!compactScreen.matches);
    });
    $('ehResetFilters').addEventListener('click', () => {
        $('ehDepartment').value = '';
        $('ehDesignation').value = '';
        $('ehType').value = 'PRIMARY';
        load();
    });
    $('ehRetry').addEventListener('click', () => optionsReady ? load() : initialize());
    $('ehFullscreen').hidden = !document.fullscreenEnabled;
    $('ehFullscreen').addEventListener('click', async () => {
        try {
            if (document.fullscreenElement) await document.exitFullscreen();
            else await $('ehWorkspace').requestFullscreen();
        } catch (error) { status('Full-screen view is unavailable. You can still use the zoom and fit controls.'); }
    });
    document.addEventListener('fullscreenchange', () => {
        const active = document.fullscreenElement === $('ehWorkspace');
        $('ehFullscreen').setAttribute('aria-pressed', String(active));
        $('ehFullscreen').setAttribute('aria-label', active ? 'Exit full screen' : 'Open chart in full screen');
        $('ehFullscreen').title = active ? 'Exit full screen' : 'Full screen';
        $('ehFullscreenLabel').textContent = active ? 'Exit full screen' : 'Full screen';
    });

    // Mouse dragging complements native touch/trackpad scrolling; card controls never start a drag.
    let drag = null;
    viewport.addEventListener('pointerdown', event => {
        if (!root || event.pointerType !== 'mouse' || event.button !== 0 || event.target.closest('.eh-card, button')) return;
        drag = { id: event.pointerId, x: event.clientX, y: event.clientY, left: viewport.scrollLeft, top: viewport.scrollTop };
        viewport.setPointerCapture(event.pointerId);
        viewport.classList.add('eh-dragging');
        event.preventDefault();
    });
    viewport.addEventListener('pointermove', event => {
        if (!drag || event.pointerId !== drag.id) return;
        viewport.scrollLeft = drag.left + drag.x - event.clientX;
        viewport.scrollTop = drag.top + drag.y - event.clientY;
    });
    function endDrag() { drag = null; viewport.classList.remove('eh-dragging'); }
    viewport.addEventListener('pointerup', endDrag);
    viewport.addEventListener('pointercancel', endDrag);
    viewport.addEventListener('lostpointercapture', endDrag);
    new ResizeObserver(() => { if (root) { if (fitMode) fit(); else applyScale(); } }).observe(viewport);

    function options(id, values, placeholder) {
        const select = $(id);
        select.replaceChildren(new Option(placeholder, ''));
        values.forEach(value => select.add(new Option(value.label, value.id)));
    }

    async function initialize() {
        try {
            const data = await get(`${api}/options`);
            options('ehHod', data.hods, 'Select a HOD');
            options('ehDepartment', data.departments, 'All departments');
            options('ehDesignation', data.designations, 'All designations');
            optionsReady = true;
            filterSummary();
            const initial = new URLSearchParams(window.location.search).get('hodEmployeeId');
            if (initial && /^\d+$/.test(initial)) {
                if (![...$('ehHod').options].some(option => option.value === initial)) {
                    $('ehHod').add(new Option(`Employee ${initial}`, initial));
                }
                $('ehHod').value = initial;
                await load();
            } else if (!data.hods.length) {
                emptyState('unavailable');
                status('No active HOD employee is available. Link the HOD user to an active employee record and configure reporting mappings.');
            }
        } catch (error) { emptyState('error', error.message); status(error.message, true); }
    }
    summary();
    initialize();
})();
