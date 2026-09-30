(() => {
    'use strict';
    const page = document.getElementById('employeeHierarchy');
    if (!page) return;
    const $ = id => document.getElementById(id);
    const api = page.dataset.api;
    const context = page.dataset.context.replace(/\/$/, '');
    const photoApi = page.dataset.photoApi || '';
    const selfMode = page.dataset.selfMode === 'true';
    const horizontalLayout = page.dataset.layout === 'horizontal';
    const viewport = $('ehViewport');
    const stage = $('ehStage');
    const surface = $('ehSurface');
    const cards = $('ehCards');
    const lines = $('ehLines');
    const results = $('ehResults');
    const styles = getComputedStyle(page);
    const CARD_WIDTH = parseFloat(styles.getPropertyValue('--eh-card-width')) || 200;
    const CARD_HEIGHT = parseFloat(styles.getPropertyValue('--eh-card-height')) || 220;
    const GAP_X = parseFloat(styles.getPropertyValue('--eh-gap-x')) || 20;
    const GAP_Y = parseFloat(styles.getPropertyValue('--eh-gap-y')) || 36;
    const PADDING = parseFloat(styles.getPropertyValue('--eh-chart-padding')) || 32;
    const CONTROL_SPACE = 80;
    const compactScreen = window.matchMedia('(max-width: 767px)');
    let root = null, scale = 1, chartWidth = 0, chartHeight = 0, leftInset = 0, topInset = 0;
    let layoutOffsetX = 0, layoutOffsetY = 0;
    let selectedId = null, generation = 0, queryGeneration = 0, searchController = null;
    let filters = null, displayed = new Map(), busy = new Set(), fitMode = false;
    let searchTimer = null, optionsReady = false, hierarchyController = null;
    let branchSelectionGeneration = 0;

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

    function summary(visible = 0, levels = 0) {
        $('ehVisibleCount').textContent = root ? visible : '—';
        $('ehReportsCount').textContent = root ? root.totalChildren : '—';
        $('ehLevelsCount').textContent = root ? levels : '—';
        $('ehScope').textContent = root ? `${root.employeeName} · Primary reporting`
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
            if (response.status === 403) throw new Error('You do not have access to this hierarchy.');
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
        branchSelectionGeneration++;
        selectedId = null;
        layoutOffsetX = 0;
        layoutOffsetY = 0;
        $('ehSearch').value = '';
        busy = new Set();
        filters = { rootId: $('ehHod').value, reportingType: 'PRIMARY' };
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
            collapseCompetingBranches(root);
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

    const reportingChain = $('employeeReportingChain');
    async function loadReportingChain() {
        const retry = $('ehChainRetry');
        retry.hidden = true;
        reportingChain.setAttribute('aria-busy', 'true');
        $('ehChainStatus').hidden = false;
        $('ehChainStatus').textContent = 'Loading your reporting chain...';
        try {
            const chain = await get(reportingChain.dataset.api);
            const members = chain.map(member => {
                const item = element('li', member.currentEmployee ? 'is-current' : '');
                item.append(element('strong', '', member.employeeName),
                    element('span', '', member.designation || 'Designation not available'));
                if (member.currentEmployee) item.append(element('small', '', 'You'));
                return item;
            });
            $('ehChainMembers').replaceChildren(...members);
            $('ehChainStatus').hidden = chain.length > 1;
            $('ehChainStatus').textContent = 'No reporting manager is assigned above you.';
        } catch (error) {
            $('ehChainStatus').textContent = error.message;
            retry.hidden = false;
        } finally {
            reportingChain.setAttribute('aria-busy', 'false');
        }
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
        article.setAttribute('aria-label', `${node.employeeName}, ${node.designation || 'Designation not set'}, ${node.totalSubordinates} total subordinates`);
        article.classList.toggle('eh-root', node === root);
        article.classList.toggle('eh-found', node.employeeId === selectedId);
        article.classList.toggle('eh-ancestor', !node.filterMatch);
        const avatar = element('div', 'eh-avatar');
        avatar.setAttribute('aria-hidden', 'true');
        const photo = element('img');
        photo.alt = '';
        photo.loading = 'lazy';
        photo.decoding = 'async';
        const fallbackPhoto = `${context}/img/employee-default-avatar.svg`;
        // Accept only this application's photo proxy; never display remote URLs or filesystem paths.
        const expected = photoApi ? `${photoApi}/${node.employeeId}` : '';
        photo.src = expected && node.profilePhoto === expected ? context + expected : fallbackPhoto;
        photo.addEventListener('error', () => { photo.src = fallbackPhoto; }, { once: true });
        avatar.append(photo);
        const name = element('h3');
        name.append(element('span', 'eh-name-label', 'Name: '),
            element('span', 'eh-name-value', node.employeeName || 'Employee'));
        name.title = name.textContent;
        const designation = element('div', 'eh-designation');
        designation.append(element('span', 'eh-position-label', 'Position: '),
            element('span', 'eh-designation-value', node.designation || 'Designation not set'));
        designation.title = designation.textContent;
        const subordinates = element('div', 'eh-subordinate-count');
        subordinates.append('Total subordinates: ', element('strong', '', node.totalSubordinates));
        subordinates.title = 'Direct and indirect subordinates in the selected hierarchy, including collapsed and unloaded branches.';
        const identity = element('div', 'eh-identity');
        const identityCopy = element('div', 'eh-identity-copy');
        identityCopy.append(name, designation, subordinates);
        identity.append(avatar, identityCopy);
        article.append(identity);
        if (node.totalChildren) {
            const footer = element('div', 'eh-card-footer');
            const toggle = button(busy.has(node.employeeId) ? '…' : node.expanded ? '−' : '+', 'toggle', node.employeeId,
                `${node.expanded ? 'Collapse' : 'Expand'} reports of ${node.employeeName}`);
            toggle.title = toggle.getAttribute('aria-label');
            toggle.setAttribute('aria-expanded', String(node.expanded));
            footer.append(toggle);
            if (node.expanded && node.nextOffset !== null) {
                const more = button('…', 'more', node.employeeId, `Load more reports of ${node.employeeName}`);
                more.title = more.getAttribute('aria-label');
                footer.append(more);
            }
            article.append(footer);
        }
        return article;
    }

    function loadedPathTo(employeeId) {
        const path = [];
        function visit(node) {
            path.push(node);
            if (node.employeeId === employeeId) return true;
            for (const child of node.children) {
                if (visit(child)) return true;
            }
            path.pop();
            return false;
        }
        return root && visit(root) ? path : [];
    }

    function collapseCompetingBranches(node) {
        const activePath = new Set(loadedPathTo(node.employeeId));
        const stack = root ? [root] : [];
        while (stack.length) {
            const current = stack.pop();
            if (current !== root && !activePath.has(current)) current.expanded = false;
            stack.push(...current.children);
        }
    }

    function collapseBranch(node) {
        const stack = [node];
        while (stack.length) {
            const current = stack.pop();
            current.expanded = false;
            stack.push(...current.children);
        }
    }

    function entryAnchor(entry) {
        return horizontalLayout
            ? { x: entry.x + CARD_WIDTH / 2, y: entry.y + CARD_HEIGHT / 2 }
            : { x: entry.x, y: entry.y };
    }

    function renderKeepingNodeFixed(node) {
        const existingEntry = displayed.get(node.employeeId);
        const anchor = existingEntry && entryAnchor(existingEntry);
        const screenPosition = anchor ? {
            x: anchor.x * scale + leftInset - viewport.scrollLeft,
            y: anchor.y * scale + topInset - viewport.scrollTop
        } : null;
        render();
        restoreNodeScreenPosition(node, screenPosition);
    }

    function restoreNodeScreenPosition(node, screenPosition) {
        const updatedEntry = displayed.get(node.employeeId);
        if (!screenPosition || !updatedEntry) return;
        const anchor = entryAnchor(updatedEntry);
        viewport.scrollLeft = anchor.x * scale + leftInset - screenPosition.x;
        viewport.scrollTop = anchor.y * scale + topInset - screenPosition.y;
        const remainingX = screenPosition.x - (anchor.x * scale + leftInset - viewport.scrollLeft);
        const remainingY = screenPosition.y - (anchor.y * scale + topInset - viewport.scrollTop);
        if (Math.abs(remainingX) < .5 && Math.abs(remainingY) < .5) return;
        layoutOffsetX += remainingX;
        layoutOffsetY += remainingY;
        applyScale();
        viewport.scrollLeft = anchor.x * scale + leftInset - screenPosition.x;
        viewport.scrollTop = anchor.y * scale + topInset - screenPosition.y;
    }

    // Horizontal subtrees reserve their full vertical span before placement so
    // sibling branches cannot overlap. Scroll compensation keeps the acted-on manager fixed.
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
        let maxDepth = 0;
        let minLeft, maxRight;
        if (horizontalLayout) {
            for (let index = ordered.length - 1; index >= 0; index--) {
                const entry = ordered[index];
                entry.subtreeHeight = entry.children.length
                    ? entry.children.reduce((height, child) => height + displayed.get(child.employeeId).subtreeHeight, 0)
                        + (entry.children.length - 1) * GAP_Y
                    : CARD_HEIGHT;
            }
            const positionSubtree = (entry, top) => {
                entry.x = PADDING + entry.depth * (CARD_WIDTH + GAP_X);
                entry.y = top + (entry.subtreeHeight - CARD_HEIGHT) / 2;
                maxDepth = Math.max(maxDepth, entry.depth);
                let childTop = top;
                for (const child of entry.children) {
                    const childEntry = displayed.get(child.employeeId);
                    positionSubtree(childEntry, childTop);
                    childTop += childEntry.subtreeHeight + GAP_Y;
                }
            };
            positionSubtree(ordered[0], PADDING);
            minLeft = PADDING;
            maxRight = PADDING + (maxDepth + 1) * CARD_WIDTH + maxDepth * GAP_X;
        } else {
            ordered[0].x = 0;
            for (const entry of ordered) {
                entry.y = PADDING + entry.depth * (CARD_HEIGHT + GAP_Y);
                maxDepth = Math.max(maxDepth, entry.depth);
                const groupWidth = entry.children.length * CARD_WIDTH
                    + Math.max(0, entry.children.length - 1) * GAP_X;
                const childStart = entry.x - groupWidth / 2 + CARD_WIDTH / 2;
                entry.children.forEach((child, index) => {
                    displayed.get(child.employeeId).x = childStart + index * (CARD_WIDTH + GAP_X);
                });
            }
            minLeft = Math.min(...ordered.map(entry => entry.x - CARD_WIDTH / 2));
            maxRight = Math.max(...ordered.map(entry => entry.x + CARD_WIDTH / 2));
            const horizontalOffset = PADDING - minLeft;
            ordered.forEach(entry => { entry.x += horizontalOffset; });
        }

        const fragment = document.createDocumentFragment();
        for (const entry of ordered) {
            fragment.append(card(entry.node, horizontalLayout ? entry.x : entry.x - CARD_WIDTH / 2, entry.y));
        }
        for (const entry of ordered) {
            if (!entry.children.length) continue;
            const childEntries = entry.children.map(child => displayed.get(child.employeeId));
            let path;
            if (horizontalLayout) {
                const right = entry.x + CARD_WIDTH;
                const centerY = entry.y + CARD_HEIGHT / 2;
                const bus = right + GAP_X / 2;
                const childCenters = childEntries.map(child => child.y + CARD_HEIGHT / 2);
                path = `M ${right} ${centerY} H ${bus} M ${bus} ${childCenters[0]} V ${childCenters.at(-1)}`;
                childEntries.forEach((child, index) => { path += ` M ${bus} ${childCenters[index]} H ${child.x}`; });
            } else {
                const bottom = entry.y + CARD_HEIGHT, bus = bottom + GAP_Y / 2;
                path = `M ${entry.x} ${bottom} V ${bus} M ${childEntries[0].x} ${bus} H ${childEntries.at(-1).x}`;
                for (const child of childEntries) path += ` M ${child.x} ${bus} V ${child.y}`;
            }
            const connector = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            if (entry.node === root) connector.classList.add('eh-root-line');
            connector.setAttribute('d', path);
            lines.append(connector);
        }
        cards.append(fragment);
        chartWidth = maxRight - minLeft + PADDING * 2;
        chartHeight = horizontalLayout ? ordered[0].subtreeHeight + PADDING * 2
            : (maxDepth + 1) * CARD_HEIGHT + maxDepth * GAP_Y + PADDING * 2;
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
        leftInset = (horizontalLayout ? 0 : Math.max(0, (viewport.clientWidth - chartWidth * scale) / 2)) + layoutOffsetX;
        topInset = (horizontalLayout ? Math.max(0, (viewport.clientHeight - CONTROL_SPACE - chartHeight * scale) / 2) : 0)
            + layoutOffsetY;
        stage.style.left = `${leftInset}px`;
        stage.style.top = `${topInset}px`;
        stage.style.transform = `scale(${scale})`;
        surface.style.width = `${Math.max(viewport.clientWidth, Math.max(0, leftInset) + chartWidth * scale)}px`;
        surface.style.height = `${Math.max(viewport.clientHeight, Math.max(0, topInset) + chartHeight * scale + CONTROL_SPACE)}px`;
        $('ehZoom').value = `${Math.round(scale * 100)}%`;
    }

    function zoom(next) {
        if (!root) return;
        const centerX = (viewport.scrollLeft + viewport.clientWidth / 2 - leftInset) / scale;
        const centerY = (viewport.scrollTop + viewport.clientHeight / 2 - topInset) / scale;
        scale = Math.max(.02, Math.min(2, next));
        fitMode = false;
        applyScale();
        viewport.scrollLeft = centerX * scale + leftInset - viewport.clientWidth / 2;
        viewport.scrollTop = centerY * scale + topInset - viewport.clientHeight / 2;
    }

    function fit() {
        if (!root) return;
        layoutOffsetX = 0;
        layoutOffsetY = 0;
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
            const anchor = entryAnchor(entry);
            viewport.scrollLeft = horizontalLayout && !focusId ? 0 : anchor.x * scale + leftInset - viewport.clientWidth / 2;
            viewport.scrollTop = horizontalLayout
                ? anchor.y * scale + topInset - viewport.clientHeight / 2
                : focusId ? Math.max(0, anchor.y * scale - viewport.clientHeight / 3) : 0;
        }
    }

    async function loadReports(node, exclusiveExpansion = false, branchSelection = branchSelectionGeneration) {
        if (busy.has(node.employeeId)) return;
        const current = generation;
        const existingEntry = displayed.get(node.employeeId);
        const anchor = entryAnchor(existingEntry);
        const screenPosition = {
            x: anchor.x * scale + leftInset - viewport.scrollLeft,
            y: anchor.y * scale + topInset - viewport.scrollTop
        };
        busy.add(node.employeeId);
        render();
        try {
            const branch = decorate(await get(url({ nodeId: node.employeeId, offset: node.nextOffset || 0 })));
            if (current !== generation) return;
            const byId = new Map(node.children.map(child => [child.employeeId, child]));
            branch.children.forEach(child => { if (!byId.has(child.employeeId)) byId.set(child.employeeId, child); });
            node.children = [...byId.values()];
            node.totalChildren = branch.totalChildren;
            node.totalSubordinates = branch.totalSubordinates;
            node.nextOffset = branch.nextOffset;
            if (exclusiveExpansion && branchSelection !== branchSelectionGeneration) return;
            if (exclusiveExpansion) collapseCompetingBranches(node);
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
                restoreNodeScreenPosition(node, screenPosition);
            }
        }
    }

    cards.addEventListener('click', event => {
        const control = event.target.closest('button[data-action]');
        if (!control) return;
        const node = displayed.get(Number(control.dataset.id))?.node;
        if (!node || busy.has(node.employeeId)) return;
        if (control.dataset.action === 'more') {
            loadReports(node);
        } else if (!node.expanded && !node.children.length) {
            loadReports(node, true, ++branchSelectionGeneration);
        } else {
            branchSelectionGeneration++;
            if (node.expanded) {
                collapseBranch(node);
            } else {
                collapseCompetingBranches(node);
                node.expanded = true;
            }
            fitMode = false;
            renderKeepingNodeFixed(node);
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
        branchSelectionGeneration++;
        layoutOffsetX = 0;
        layoutOffsetY = 0;
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
        scale = 1;
        fitMode = false;
        layoutOffsetX = layoutOffsetY = 0;
        applyScale();
        if (horizontalLayout) {
            const anchor = entryAnchor(displayed.get(root.employeeId));
            viewport.scrollLeft = 0;
            viewport.scrollTop = anchor.y + topInset - viewport.clientHeight / 2;
        } else {
            viewport.scrollTop = 0;
            viewport.scrollLeft = Math.max(0, chartWidth / 2 + leftInset - viewport.clientWidth / 2);
        }
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
    new ResizeObserver(() => {
        if (root && viewport.clientWidth) { if (fitMode) fit(); else applyScale(); }
    }).observe(viewport);

    function options(id, values, placeholder) {
        const select = $(id);
        select.replaceChildren(new Option(placeholder, ''));
        values.forEach(value => select.add(new Option(value.label, value.id)));
    }

    async function initialize() {
        try {
            const data = await get(`${api}/options`);
            options('ehHod', data.hods, 'Select a HOD');
            optionsReady = true;
            $('ehShow').disabled = true;
            const initial = new URLSearchParams(window.location.search).get('hodEmployeeId');
            if (selfMode && data.hods.length) {
                $('ehHod').value = String(data.hods[0].id);
                await load();
            } else if (initial && /^\d+$/.test(initial)) {
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
    if (reportingChain) {
        $('ehChainRetry').addEventListener('click', loadReportingChain);
        loadReportingChain();
    }
    initialize();
})();
