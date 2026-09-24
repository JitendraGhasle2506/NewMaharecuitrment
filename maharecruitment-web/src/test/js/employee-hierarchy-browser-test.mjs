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
let failNextTree = false;
function node(id, expanded = false) {
    const reports = children.get(id) || [];
    return { employeeId: id, employeeName: names[id - 1], employeeCode: `EMP00${id}`,
        designation: id === 1 ? 'Head of Department' : id < 5 ? 'Team Manager' : 'Software Engineer',
        department: 'Technology', profilePhoto: null, totalChildren: reports.length, hasChildren: !!reports.length,
        filterMatch: true, nextOffset: reports.length && !expanded ? 0 : null,
        children: expanded ? reports.map(id => node(id)) : [] };
}
let page = await readFile(join(base, 'templates/hr/employee-hierarchy.html'), 'utf8');
page = page.replace(/th:href="@\{([^}]+)\}"/g, 'href="$1"')
    .replace(/th:src="@\{([^}]+)\}"/g, 'src="$1"')
    .replace(/th:attr="[^"]+"/, 'data-api="/api/employees/hierarchy" data-context="/"')
    .replace('</head>', '<style>body{font-family:system-ui,sans-serif;margin:24px;background:#f4f7fb}.btn{color:#087ba4;text-decoration:none;font-size:13px}</style></head>');
const server = createServer(async (request, response) => {
    const url = new URL(request.url, 'http://localhost');
    try {
        if (url.pathname.startsWith('/api/')) {
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
        } else if (['/js/employee-hierarchy.js', '/css/employee-hierarchy.css'].includes(url.pathname)) {
            response.setHeader('Content-Type', url.pathname.endsWith('.js') ? 'text/javascript' : 'text/css');
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
        if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails.text);
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
    const waitFor = expression => evaluate(`new Promise((resolve, reject) => {
        let attempts = 0; const timer = setInterval(() => {
            if (${expression}) { clearInterval(timer); resolve(true); }
            else if (++attempts > 200) { clearInterval(timer); reject(new Error('Condition timed out')); }
        }, 25);
    })`);
    await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1080, deviceScaleFactor: 1, mobile: false });
    await send('Page.navigate', { url: `${origin}/?hodEmployeeId=1` });
    await waitFor(`document.querySelectorAll('.eh-card').length === 4`);
    assert.equal(await evaluate(`document.querySelector('.eh-root h3').textContent`), names[0]);
    assert.equal(await evaluate(`document.getElementById('ehVisibleCount').textContent`), '4');
    assert.equal(await evaluate(`document.getElementById('ehReportsCount').textContent`), '3');
    assert.equal(await evaluate(`document.getElementById('ehLevelsCount').textContent`), '2');
    await evaluate(`document.querySelector('button[data-id="2"][data-action="toggle"]').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 6`);
    const overlap = await evaluate(`(() => {
        const cards = [...document.querySelectorAll('.eh-card')].map(e => e.getBoundingClientRect());
        return cards.some((a,i) => cards.some((b,j) => i < j && a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top));
    })()`);
    assert.equal(overlap, false, 'Employee cards must not overlap');
    assert.equal(await evaluate(`document.querySelectorAll('#ehLines path').length`), 2);
    assert.equal(await evaluate(`document.getElementById('ehLevelsCount').textContent`), '3');
    await evaluate(`document.getElementById('ehFit').click()`);
    for (const [label, width, height] of [['desktop', 1440, 1080], ['laptop', 1024, 900], ['tablet', 768, 1024], ['mobile', 390, 844], ['small-mobile', 320, 740]]) {
        await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: width < 600 });
        await evaluate(`new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))`);
        assert.equal(await evaluate(`document.documentElement.scrollWidth <= window.innerWidth`), true, `${label}: no page-level horizontal overflow`);
        assert.equal(await evaluate(`document.getElementById('ehFilterFields').hidden`), width < 768, `${label}: responsive filter disclosure`);
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
    await evaluate(`document.getElementById('ehFilterToggle').click()`);
    assert.equal(await evaluate(`document.getElementById('ehFilterToggle').getAttribute('aria-expanded')`), 'true');
    await evaluate(`document.getElementById('ehDepartment').value='10'; document.getElementById('ehDepartment').dispatchEvent(new Event('change', {bubbles:true}))`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 4`);
    assert.equal(await evaluate(`document.getElementById('ehFilterCount').textContent`), '1');
    await evaluate(`document.getElementById('ehResetFilters').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 4`);
    assert.equal(await evaluate(`document.getElementById('ehFilterCount').hidden`), true);
    await evaluate(`document.querySelector('button[data-id="2"][data-action="toggle"]').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 6`);
    await evaluate(`document.querySelector('button[data-id="2"][data-action="toggle"]').click()`);
    assert.equal(await evaluate(`document.querySelectorAll('.eh-card').length`), 4);
    await evaluate(`document.getElementById('ehSearch').value='EMP006'; document.getElementById('ehSearch').dispatchEvent(new Event('input'))`);
    await waitFor(`document.querySelector('#ehResults button')`);
    await evaluate(`document.getElementById('ehSearch').dispatchEvent(new KeyboardEvent('keydown', {key:'ArrowDown', bubbles:true}))`);
    assert.equal(await evaluate(`document.activeElement === document.querySelector('#ehResults button')`), true, 'Search supports keyboard navigation');
    await evaluate(`document.querySelector('#ehResults button').click()`);
    assert.equal(await evaluate(`document.querySelector('.eh-found h3').textContent`), names[5]);
    assert.equal(await evaluate(`document.querySelectorAll('.eh-card').length`), 3);
    await evaluate(`document.querySelector('button[data-id="1"][data-action="more"]').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 5`);
    assert.equal(await evaluate(`document.querySelectorAll('.eh-card[data-id="2"]').length`), 1, 'Path and paginated branch merge without duplicates');
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
    assert.equal(await evaluate(`document.getElementById('ehFilterFields').hidden`), true);
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
    await waitFor(`document.getElementById('ehHod').options.length > 1`);
    assert.equal(await evaluate(`document.getElementById('ehEmpty').hidden`), false, 'Empty state shown until a HOD is selected');
    assert.equal(await evaluate(`document.getElementById('ehZoomIn').disabled`), true, 'Empty chart disables zoom');
    failNextTree = true;
    await evaluate(`document.getElementById('ehHod').value='1'; document.getElementById('ehHod').dispatchEvent(new Event('change',{bubbles:true}))`);
    await waitFor(`document.getElementById('ehEmpty').classList.contains('is-error')`);
    assert.equal(await evaluate(`document.getElementById('ehRetry').hidden`), false, 'Failed requests offer retry');
    await evaluate(`document.getElementById('ehRetry').click()`);
    await waitFor(`document.querySelectorAll('.eh-card').length === 4`);
    assert.equal(await evaluate(`document.getElementById('ehViewport').getAttribute('aria-busy')`), 'false');
    assert.deepEqual(exceptions, [], 'No browser JavaScript exceptions');
    console.log(`PASS: expand/collapse, connectors, non-overlap, live/keyboard search, branch merging, zoom, filters, metrics, full screen, drag-to-pan, empty state, readable mobile view, 320–1440px layouts. Screenshots: ${artifacts}`);
} finally {
    if (cdp && socket?.readyState === WebSocket.OPEN) await cdp('Browser.close').catch(() => {});
    socket?.close();
    processHandle.kill();
    server.close();
}
