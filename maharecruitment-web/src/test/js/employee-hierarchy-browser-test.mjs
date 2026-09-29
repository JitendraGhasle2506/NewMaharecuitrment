// Dependency-free browser smoke test. Run from the repository root with Node 22+.
// Uses synthetic data only; never starts the application or connects to its database.
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { readFile, mkdtemp, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawn } from 'node:child_process';

const base = resolve('maharecruitment-web/src/main/resources');
const browser = process.env.CHROME_BIN || [
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    '/usr/bin/chromium', '/usr/bin/google-chrome'
].find(existsSync);
assert.ok(browser, 'Set CHROME_BIN to a Chrome/Chromium executable');
const artifacts = await mkdtemp(join(tmpdir(), 'employee-hierarchy-test-'));
const names = ['Ananya Deshmukh', 'Rohan Patil', 'Sara Shah', 'Dev Mehta', 'Mira Joshi', 'Aarav Kulkarni', 'Nisha Rao'];
const children = new Map([[1, [2, 3, 4]], [2, [5, 6]], [3, [7]]]);
const totalSubordinates = new Map([[1, 6], [2, 2], [3, 1]]);
let failNextTree = false;
let apiRequests = 0;
function node(id, expanded = false) {
    const reports = children.get(id) || [];
    return { employeeId: id, employeeName: names[id - 1], employeeCode: `EMP00${id}`,
        designation: id === 1 ? 'Head of Department' : id < 5 ? 'Team Manager' : 'Software Engineer',
        department: 'Technology', profilePhoto: id === 2 || id === 4 ? `/api/employees/hierarchy/photo/${id}` : null,
        totalChildren: reports.length, hasChildren: !!reports.length,
        totalSubordinates: totalSubordinates.get(id) || 0,
        filterMatch: true, nextOffset: reports.length && !expanded ? 0 : null,
        children: expanded ? reports.map(id => node(id)) : [] };
}
let page = await readFile(join(base, 'templates/hr/employee-hierarchy.html'), 'utf8');
page = page.replace(/th:href="@\{([^}]+)\}"/g, 'href="$1"')
    .replace(/th:src="@\{([^}]+)\}"/g, 'src="$1"')
    .replace(/th:attr="[^"]+"/, 'data-api="/api/employees/hierarchy" data-context="/" data-photo-api="/api/employees/hierarchy/photo"')
    .replace('</head>', '<style>body{font-family:system-ui,sans-serif;margin:24px;background:#f4f7fb}.btn{color:#087ba4;text-decoration:none;font-size:13px}</style></head>');
const dashboard = await readFile(join(base, 'templates/employee/dashboard.html'), 'utf8');
const dashboardPage = dashboard.replace(/th:href="@\{([^}]+)\}"/g, 'href="$1"');
const teamTemplate = await readFile(join(base, 'templates/employee/employee-hierarchy.html'), 'utf8');
const backLink = teamTemplate.match(/<a class="eh-secondary"[\s\S]*?<\/a>/)[0]
    .replace(/th:href="@\{([^}]+)\}"/g, 'href="$1"');
const teamPage = page.replace('data-api="/api/employees/hierarchy"',
    'data-self-mode="true" data-api="/api/employees/hierarchy"')
    .replace('<section id="employeeHierarchy"', `${backLink}<section id="employeeHierarchy"`);
const server = createServer(async (request, response) => {
    const url = new URL(request.url, 'http://localhost');
    try {
        if (url.pathname === '/api/employees/hierarchy/photo/2') {
            response.setHeader('Content-Type', 'image/svg+xml');
            response.end(await readFile(join(base, 'static/img/employee-default-avatar.svg')));
        } else if (url.pathname === '/api/employees/hierarchy/photo/4') {
            response.statusCode = 404; response.end();
        } else if (url.pathname.startsWith('/api/')) {
            apiRequests++;
            let data;
            if (url.pathname.endsWith('/options')) data = {
                hods: [{ id: 1, label: names[0] }], departments: [{ id: 10, label: 'Technology' }],
                designations: [{ id: 20, label: 'Software Engineer' }]
            };
            else if (url.pathname.endsWith('/search')) data = url.searchParams.get('q') === 'missing' ? [] : [
                { employeeId: 6, employeeName: names[5], employeeCode: 'EMP006', path: [node(1), node(2), node(6)] }
            ];
            else {
                if (failNextTree) {
                    failNextTree = false;
                    response.writeHead(500, { 'Content-Type': 'application/json' });
                    response.end(JSON.stringify({ success: false }));
                    return;
                }
                data = node(Number(url.searchParams.get('nodeId') || 1), true);
            }
            response.setHeader('Content-Type', 'application/json');
            response.end(JSON.stringify({ success: true, data }));
        } else if (url.pathname === '/') {
            response.setHeader('Content-Type', 'text/html'); response.end(page);
        } else if (url.pathname === '/employee/dashboard') {
            response.setHeader('Content-Type', 'text/html'); response.end(dashboardPage);
        } else if (url.pathname === '/employee/employee-hierarchy') {
            response.setHeader('Content-Type', 'text/html'); response.end(teamPage);
        } else if (['/js/employee-hierarchy.js', '/css/employee-hierarchy.css', '/img/employee-default-avatar.svg'].includes(url.pathname)) {
            response.setHeader('Content-Type', url.pathname.endsWith('.js') ? 'text/javascript'
                : url.pathname.endsWith('.svg') ? 'image/svg+xml' : 'text/css');
            response.end(await readFile(join(base, 'static', url.pathname)));
        } else { response.statusCode = 404; response.end(); }
    } catch (error) { response.statusCode = 500; response.end(String(error)); }
});
await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
const origin = `http://127.0.0.1:${server.address().port}`;
const processHandle = spawn(browser, ['--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
    '--remote-debugging-port=0', `--user-data-dir=${join(artifacts, 'profile')}`, 'about:blank'],
{ windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'] });
let socket, cdp;
try {
    const debugUrl = await new Promise((resolve, reject) => {
        const timeout = setTimeout(() => reject(new Error('Browser did not start')), 20000);
        processHandle.once('error', reject);
        processHandle.stderr.on('data', data => {
            const match = data.toString().match(/DevTools listening on (ws:\/\/\S+)/);
            if (match) { clearTimeout(timeout); resolve(match[1]); }
        });
    });
    socket = new WebSocket(debugUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    let id = 0;
    const pending = new Map(), exceptions = [];
    socket.onmessage = event => {
        const message = JSON.parse(event.data);
        if (message.method === 'Runtime.exceptionThrown') {
            const details = message.params.exceptionDetails;
            exceptions.push(details.exception?.description || `${details.text} at ${details.url}:${details.lineNumber}`);
        }
        if (!pending.has(message.id)) return;
        const { resolve, reject, timeout } = pending.get(message.id);
        clearTimeout(timeout); pending.delete(message.id);
        if (message.error) reject(new Error(JSON.stringify(message.error))); else resolve(message.result);
    };
    cdp = (method, params = {}, sessionId) => new Promise((resolve, reject) => {
        const callId = ++id;
        const timeout = setTimeout(() => { pending.delete(callId); reject(new Error(`CDP timeout: ${method}`)); }, 20000);
        pending.set(callId, { resolve, reject, timeout });
        socket.send(JSON.stringify({ id: callId, method, params, sessionId }));
    });
    const { targetId } = await cdp('Target.createTarget', { url: 'about:blank' });
    const { sessionId } = await cdp('Target.attachToTarget', { targetId, flatten: true });
    const send = (method, params) => cdp(method, params, sessionId);
    await send('Runtime.enable');
    const evaluate = async expression => {
        const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
        if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails));
        return result.result.value;
    };
    const assertSubordinates = async (id, total, message) => assert.equal(
        await evaluate(`document.querySelector('.eh-card[data-id="${id}"] .eh-subordinate-count').textContent`),
        `Total subordinates: ${total}`, message);
    const waitFor = expression => evaluate(`new Promise((resolve, reject) => {
        let attempts = 0; const timer = setInterval(() => {
            if (${expression}) { clearInterval(timer); resolve(true); }
            else if (++attempts > 200) { clearInterval(timer); reject(new Error('Condition timed out')); }
        }, 25);
    })`);
    await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1080, deviceScaleFactor: 1, mobile: false });
    await send('Page.navigate', { url: `${origin}/?hodEmployeeId=1` });
    await waitFor(`document.querySelectorAll('.eh-card').length === 4`);
    assert.equal(await evaluate(`Array.from(document.querySelectorAll('label')).some(label =>
        ['Department', 'Designation', 'Reporting type'].includes(label.textContent.trim()))`), false,
    'Advanced hierarchy filters are not rendered');
    assert.equal(await evaluate(`document.querySelector('.eh-root .eh-name-value').textContent`), names[0]);
    assert.equal(await evaluate(`document.querySelector('.eh-root .eh-designation-value').textContent`), 'Head of Department');
    await assertSubordinates(1, 6, 'HOD total includes indirect, unloaded subordinates');
    await assertSubordinates(2, 2, 'Collapsed manager shows the full total');
    await assertSubordinates(3, 1);
    await assertSubordinates(4, 0, 'Leaf shows zero subordinates');
    assert.equal(await evaluate(`document.querySelectorAll('.eh-card .eh-code, .eh-card .eh-department, .eh-card .eh-node-badge, .eh-card .eh-leaf').length`), 0);
    assert.equal(await evaluate(`Array.from(document.querySelectorAll('.eh-card')).every(card => {
        const text = card.textContent;
        return !text.includes('EMP00') && !text.includes('Technology') && !text.includes('direct reports')
            && getComputedStyle(card).backgroundColor === 'rgba(0, 0, 0, 0)';
    })`), true, 'Transparent nodes show name, designation and subordinate total without IDs or departments');
    await waitFor(`document.querySelector('.eh-root img').naturalWidth > 0`);
    assert.equal(await evaluate(`document.getElementById('ehVisibleCount').textContent`), '4');
    assert.equal(await evaluate(`document.getElementById('ehReportsCount').textContent`), '3');
    assert.equal(await evaluate(`document.getElementById('ehLevelsCount').textContent`), '2');
    const upperLevelPositions = await evaluate(`Object.fromEntries([...document.querySelectorAll('.eh-card')].map(card => {
        const box = card.getBoundingClientRect();
        return [card.dataset.id, {left: box.left, top: box.top, width: box.width}];
    }))`);
    const parentToggleCenter = await evaluate(`(() => {
        const box = document.querySelector('button[data-id="2"][data-action="toggle"]').getBoundingClientRect();
        return {x: box.left + box.width / 2, y: box.top + box.height / 2};
    })()`);
    await evaluate(`document.querySelector('button[data-id="2"][data-action="toggle"]').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 6`);
    const expandedUpperLevelPositions = await evaluate(`Object.fromEntries([...document.querySelectorAll('.eh-card')]
        .filter(card => ['1', '2', '3', '4'].includes(card.dataset.id)).map(card => {
            const box = card.getBoundingClientRect();
            return [card.dataset.id, {left: box.left, top: box.top, width: box.width}];
        }))`);
    for (const employeeId of ['1', '2', '3', '4']) {
        assert.ok(Math.abs(expandedUpperLevelPositions[employeeId].left - upperLevelPositions[employeeId].left) < 1,
            `Expanding descendants must not shift upper-level employee ${employeeId} `
            + `(${upperLevelPositions[employeeId].left} -> ${expandedUpperLevelPositions[employeeId].left})`);
        assert.deepEqual(
            {top: expandedUpperLevelPositions[employeeId].top, width: expandedUpperLevelPositions[employeeId].width},
            {top: upperLevelPositions[employeeId].top, width: upperLevelPositions[employeeId].width},
            `Upper-level dimensions remain fixed for employee ${employeeId}`);
    }
    const expandedParentToggleCenter = await evaluate(`(() => {
        const box = document.querySelector('button[data-id="2"][data-action="toggle"]').getBoundingClientRect();
        return {x: box.left + box.width / 2, y: box.top + box.height / 2};
    })()`);
    assert.ok(Math.abs(expandedParentToggleCenter.x - parentToggleCenter.x) < 1
        && Math.abs(expandedParentToggleCenter.y - parentToggleCenter.y) < 1,
    'The parent toggle stays fixed when its icon changes from plus to minus');
    assert.equal(await evaluate(`(() => {
        const center = selector => {
            const box = document.querySelector(selector).getBoundingClientRect();
            return box.left + box.width / 2;
        };
        const parent = center('.eh-card[data-id="2"]');
        const children = ['5', '6'].map(id => center('.eh-card[data-id="' + id + '"]'));
        return Math.abs((children[0] + children[1]) / 2 - parent) < 1;
    })()`), true, 'Child employees are centered around their selected parent');
    await assertSubordinates(1, 6, 'Expanding a branch does not change the HOD total');
    await assertSubordinates(2, 2, 'Branch loading retains its total');
    const overlap = await evaluate(`(() => {
        const cards = [...document.querySelectorAll('.eh-card')].map(e => e.getBoundingClientRect());
        return cards.some((a,i) => cards.some((b,j) => i < j && a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top));
    })()`);
    assert.equal(overlap, false, 'Employee cards must not overlap');
    assert.equal(await evaluate(`document.querySelectorAll('#ehLines path').length`), 2);
    assert.equal(await evaluate(`document.getElementById('ehLevelsCount').textContent`), '3');
    await evaluate(`document.querySelector('button[data-id="3"][data-action="toggle"]').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 5`);
    assert.equal(await evaluate(`document.querySelector('button[data-id="2"][data-action="toggle"]').getAttribute('aria-expanded')`),
        'false', 'Expanding another branch collapses the previously expanded branch');
    assert.equal(await evaluate(`document.querySelector('button[data-id="3"][data-action="toggle"]').getAttribute('aria-expanded')`),
        'true', 'The newly selected branch remains expanded');
    assert.equal(await evaluate(`document.querySelector('.eh-card[data-id="5"]') === null && document.querySelector('.eh-card[data-id="7"]') !== null`),
        true, 'Only one competing branch is visible at a time');
    assert.equal(await evaluate(`(() => {
        const center = id => {
            const box = document.querySelector('.eh-card[data-id="' + id + '"]').getBoundingClientRect();
            return box.left + box.width / 2;
        };
        return Math.abs(center('3') - center('7')) < 1;
    })()`), true, 'A single child is positioned directly below its selected parent');
    await evaluate(`document.getElementById('ehFit').click()`);
    await waitFor(`document.querySelector('.eh-card[data-id="2"] img').naturalWidth > 0`);
    await waitFor(`document.querySelector('.eh-card[data-id="4"] img').naturalWidth > 0`);
    assert.ok(await evaluate(`document.querySelector('.eh-card[data-id="2"] img').src.endsWith('/api/employees/hierarchy/photo/2')`), 'Employee photos use the protected proxy');
    assert.ok(await evaluate(`document.querySelector('.eh-card[data-id="4"] img').src.endsWith('/img/employee-default-avatar.svg')`), 'Missing photos fall back to the default avatar');
    for (const [label, width, height] of [['desktop', 1440, 1080], ['laptop', 1024, 900], ['tablet', 768, 1024], ['mobile', 390, 844], ['small-mobile', 320, 740]]) {
        await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: width < 600 });
        await evaluate(`new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))`);
        assert.equal(await evaluate(`document.documentElement.scrollWidth <= window.innerWidth`), true, `${label}: no page-level horizontal overflow`);
        assert.equal(await evaluate(`(() => {
            const tools = document.querySelector('.eh-controls').getBoundingClientRect();
            const frame = document.querySelector('.eh-chart-frame').getBoundingClientRect();
            return tools.left >= frame.left && tools.right <= frame.right;
        })()`), true, `${label}: zoom controls stay inside the chart`);
        assert.equal(await evaluate(`Array.from(document.querySelectorAll('.eh-card')).every(card => card.scrollHeight <= card.clientHeight + 2)`), true, `${label}: card contents stay contained`);
        const { cssContentSize } = await send('Page.getLayoutMetrics');
        const screenshot = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true,
            clip: { x: 0, y: 0, width: cssContentSize.width, height: cssContentSize.height, scale: 1 } });
        await writeFile(join(artifacts, `${label}.png`), Buffer.from(screenshot.data, 'base64'));
    }
    await evaluate(`document.querySelector('button[data-id="2"][data-action="toggle"]').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 6`);
    await evaluate(`document.querySelector('button[data-id="2"][data-action="toggle"]').click()`);
    assert.equal(await evaluate(`document.querySelectorAll('.eh-card').length`), 4);
    await assertSubordinates(1, 6, 'Collapsing a branch does not reduce the total');
    await assertSubordinates(2, 2);
    await evaluate(`document.getElementById('ehSearch').value='EMP006'; document.getElementById('ehSearch').dispatchEvent(new Event('input'))`);
    await waitFor(`document.querySelector('#ehResults button')`);
    await evaluate(`document.getElementById('ehSearch').dispatchEvent(new KeyboardEvent('keydown', {key:'ArrowDown', bubbles:true}))`);
    assert.equal(await evaluate(`document.activeElement === document.querySelector('#ehResults button')`), true, 'Search supports keyboard navigation');
    await evaluate(`document.querySelector('#ehResults button').click()`);
    assert.equal(await evaluate(`document.querySelector('.eh-found .eh-name-value').textContent`), names[5]);
    assert.equal(await evaluate(`document.querySelectorAll('.eh-card').length`), 3);
    await assertSubordinates(1, 6, 'Search paths retain full subtree totals');
    await assertSubordinates(2, 2);
    await assertSubordinates(6, 0);
    await evaluate(`document.querySelector('button[data-id="1"][data-action="more"]').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 5`);
    assert.equal(await evaluate(`document.querySelectorAll('.eh-card[data-id="2"]').length`), 1, 'Path and paginated branch merge without duplicates');
    await assertSubordinates(1, 6, 'Merging a page retains the total');
    await evaluate(`document.getElementById('ehClear').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 4 && !document.querySelector('.eh-found')`);
    await evaluate(`document.getElementById('ehReset').click()`);
    assert.equal(await evaluate(`document.getElementById('ehZoom').value`), '100%');
    await evaluate(`document.getElementById('ehZoomIn').click()`);
    assert.equal(await evaluate(`document.getElementById('ehZoom').value`), '125%');
    await evaluate(`document.getElementById('ehZoomOut').click()`);
    assert.equal(await evaluate(`document.getElementById('ehZoom').value`), '100%');
    await evaluate(`document.getElementById('ehSearch').value='missing'; document.getElementById('ehSearchForm').requestSubmit()`);
    await waitFor(`document.querySelector('#ehResults p')`);
    assert.equal(await evaluate(`document.querySelector('#ehResults p').textContent`), 'No matching employee in this hierarchy.');
    await evaluate(`document.getElementById('ehSearch').dispatchEvent(new KeyboardEvent('keydown', {key:'Escape', bubbles:true}))`);
    assert.equal(await evaluate(`document.getElementById('ehResults').hidden`), true);
    await send('Runtime.evaluate', { expression: `document.getElementById('ehFullscreen').click()`, userGesture: true });
    await waitFor(`document.fullscreenElement === document.getElementById('ehWorkspace')`);
    assert.equal(await evaluate(`document.getElementById('ehFullscreen').getAttribute('aria-pressed')`), 'true');
    await evaluate(`document.exitFullscreen()`);
    await waitFor(`!document.fullscreenElement`);
    await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
    await send('Page.navigate', { url: `${origin}/?hodEmployeeId=1` });
    await waitFor(`document.querySelectorAll('.eh-card').length === 4`);
    assert.equal(await evaluate(`document.getElementById('ehZoom').value`), '100%', 'Mobile opens at a readable scale');
    const { cssContentSize: mobileSize } = await send('Page.getLayoutMetrics');
    const readableMobile = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true,
        clip: { x: 0, y: 0, width: mobileSize.width, height: mobileSize.height, scale: 1 } });
    await writeFile(join(artifacts, 'mobile-readable.png'), Buffer.from(readableMobile.data, 'base64'));
    await evaluate(`document.getElementById('ehViewport').scrollIntoView({block:'start'})`);
    const dragStart = await evaluate(`(() => {
        const view = document.getElementById('ehViewport'), rect = view.getBoundingClientRect();
        return {x: rect.left + 35, y: rect.top + 20, scrollLeft: view.scrollLeft};
    })()`);
    await send('Input.dispatchMouseEvent', { type: 'mousePressed', x: dragStart.x, y: dragStart.y, button: 'left', clickCount: 1 });
    await send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: dragStart.x + 70, y: dragStart.y, button: 'left', buttons: 1 });
    await send('Input.dispatchMouseEvent', { type: 'mouseReleased', x: dragStart.x + 70, y: dragStart.y, button: 'left', clickCount: 1 });
    assert.ok(await evaluate(`document.getElementById('ehViewport').scrollLeft`) < dragStart.scrollLeft, 'Drag-to-pan scrolls the chart');
    assert.equal(await evaluate(`document.getElementById('ehViewport').classList.contains('eh-dragging')`), false);
    await send('Page.navigate', { url: `${origin}/` });
    await waitFor(`document.getElementById('ehHod')?.options.length > 1`);
    assert.equal(await evaluate(`document.getElementById('ehEmpty').hidden`), false, 'Empty state shown until a HOD is selected');
    assert.equal(await evaluate(`document.getElementById('ehZoomIn').disabled`), true, 'Empty chart disables zoom');
    failNextTree = true;
    await evaluate(`document.getElementById('ehHod').value='1'; document.getElementById('ehHod').dispatchEvent(new Event('change',{bubbles:true}))`);
    await waitFor(`document.getElementById('ehEmpty').classList.contains('is-error')`);
    assert.equal(await evaluate(`document.getElementById('ehRetry').hidden`), false, 'Failed requests offer retry');
    await evaluate(`document.getElementById('ehRetry').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 4`);
    assert.equal(await evaluate(`document.getElementById('ehViewport').getAttribute('aria-busy')`), 'false');
    const requestsBeforeDashboard = apiRequests;
    await send('Page.navigate', { url: `${origin}/employee/dashboard` });
    await waitFor(`document.readyState === 'complete' && document.querySelector('a[href="/employee/employee-hierarchy"]')`);
    assert.equal(await evaluate(`document.getElementById('employeeHierarchy')`), null,
        'Dashboard does not contain the hierarchy');
    assert.equal(apiRequests, requestsBeforeDashboard, 'Dashboard makes no hierarchy API requests');
    await evaluate(`document.querySelector('a[href="/employee/employee-hierarchy"]').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 4`);
    assert.equal(await evaluate(`location.pathname`), '/employee/employee-hierarchy');
    await evaluate(`document.querySelector('button[data-id="2"][data-action="toggle"]').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 6`);
    assert.equal(await evaluate(`document.documentElement.scrollWidth <= window.innerWidth`), true,
        'Team hierarchy fits the mobile page');
    await evaluate(`document.querySelector('a[href="/employee/dashboard"]').click()`);
    await waitFor(`location.pathname === '/employee/dashboard' && document.querySelector('.quick-actions')`);
    assert.deepEqual(exceptions, [], 'No browser JavaScript exceptions');
    console.log('PASS: Dashboard link opens the team hierarchy on a separate page; branches expand and Back to Dashboard returns correctly.');
    console.log(`PASS: HOD-only selection, minimal transparent nodes, total subordinate counts (including unloaded branches), photo/fallback, expand/collapse, connectors, non-overlap, live/keyboard search, branch merging, zoom, metrics, full screen, drag-to-pan, empty state, readable mobile view, 320–1440px layouts. Screenshots: ${artifacts}`);
} finally {
    if (cdp && socket?.readyState === WebSocket.OPEN) await cdp('Browser.close').catch(() => {});
    socket?.close();
    processHandle.kill();
    server.close();
}
