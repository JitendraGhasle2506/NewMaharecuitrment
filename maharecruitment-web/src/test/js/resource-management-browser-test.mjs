// Node 22+, Chrome/Edge. Synthetic API only; no application database is used.
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
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', '/usr/bin/chromium'
].find(existsSync);
assert.ok(browser, 'Set CHROME_BIN to Chrome or Edge');
const artifacts = await mkdtemp(join(tmpdir(), 'resource-management-'));
let page = (await readFile(join(base, 'templates/employee/resource-management.html'), 'utf8'))
    .replace(/\(v=(\d+)\)/g, '?v=$1').replace(/th:href="@\{([^}]+)\}"/g, 'href="$1"')
    .replace(/th:src="@\{([^}]+)\}"/g, 'src="$1"')
    .replace(/th:attr="[^"]+"/, 'data-api="/resources/api"')
    .replace('</head>', '<meta name="_csrf" content="test-token"><meta name="_csrf_header" content="X-CSRF-TOKEN"><link rel="stylesheet" href="/assets/vendor/bootstrap/bootstrap.min.css"><style>body{font-family:system-ui;background:#f4f7fb}</style></head>');
const resources = Array.from({ length: 16 }, (_, index) => ({ employeeId: index + 2,
    name: index === 0 ? 'Ananya Deshmukh' : index === 1 ? '<img src=x onerror=alert(1)>' : `Resource ${index + 2}`,
    code: `EMP${index + 2}`, designation: 'Software Engineer', department: 'Technology',
    employeeType: index === 15 ? 'EXTERNAL' : index === 14 ? 'MAHAIT' : 'INTERNAL',
    manager: index < 3 ? 'My Team Lead' : null, managerEmployeeId: index < 3 ? 1 : null, direct: index === 0 }));
const mapped = new Set([2, 3, 4]);
const actions = [];
let failNextRead = false;
const server = createServer(async (request, response) => {
    const url = new URL(request.url, 'http://localhost');
    try {
        if (url.pathname.startsWith('/resources/api')) {
            response.setHeader('Content-Type', 'application/json');
            if (request.method === 'POST') {
                assert.equal(request.headers['x-csrf-token'], 'test-token');
                let body = '';
                for await (const chunk of request) body += chunk;
                const [, employee, action] = url.pathname.match(/\/(\d+)\/(map|release|reassign)$/);
                actions.push({ id: Number(employee), action, body: body ? JSON.parse(body) : null });
                if (action === 'release') mapped.delete(Number(employee));
                else {
                    mapped.add(Number(employee));
                    const resource = resources.find(item => item.employeeId === Number(employee));
                    resource.managerEmployeeId = JSON.parse(body).managerEmployeeId;
                    resource.manager = resource.managerEmployeeId === 2 ? 'Ananya Deshmukh' : 'Me · Manager';
                }
                response.end(JSON.stringify({ success: true, message: 'Assignment saved successfully.' }));
                return;
            }
            if (failNextRead) {
                failNextRead = false;
                response.statusCode = 503;
                response.end(JSON.stringify({ message: 'Temporary failure. Please refresh.' }));
                return;
            }
            const free = url.searchParams.get('view') === 'free';
            const selectedType = url.searchParams.get('employeeType') || 'INTERNAL_MAHAIT';
            const matchesType = resource => selectedType === 'ALL' || (selectedType === 'INTERNAL_MAHAIT'
                ? ['INTERNAL', 'MAHAIT'].includes(resource.employeeType) : resource.employeeType === selectedType);
            const query = (url.searchParams.get('q') || '').toLowerCase();
            const found = resources.filter(resource => mapped.has(resource.employeeId) !== free)
                .filter(resource => !free || matchesType(resource))
                .filter(resource => `${resource.name} ${resource.code}`.toLowerCase().includes(query));
            const size = Number(url.searchParams.get('size') || 12);
            const pageIndex = Math.min(Number(url.searchParams.get('page') || 0), Math.max(0, Math.ceil(found.length / size) - 1));
            response.end(JSON.stringify({ resources: found.slice(pageIndex * size, (pageIndex + 1) * size),
                managers: [{ employeeId: 1, name: 'Me · Manager' }, { employeeId: 2, name: 'Ananya Deshmukh' }],
                total: found.length, page: pageIndex, size, mappedCount: mapped.size, directCount: 1,
                freeCount: resources.filter(resource => !mapped.has(resource.employeeId) && matchesType(resource)).length }));
        } else if (url.pathname === '/') {
            response.setHeader('Content-Type', 'text/html'); response.end(page);
        } else if (/^\/(css|js|assets)\//.test(url.pathname)) {
            response.setHeader('Content-Type', url.pathname.endsWith('.js') ? 'text/javascript' : 'text/css');
            response.end(await readFile(join(base, 'static', url.pathname)));
        } else { response.statusCode = 404; response.end(); }
    } catch (error) { response.statusCode = 500; response.end(JSON.stringify({ message: String(error) })); }
});
await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
const origin = `http://127.0.0.1:${server.address().port}`;
const child = spawn(browser, ['--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
    '--remote-debugging-port=0', `--user-data-dir=${join(artifacts, 'profile')}`, 'about:blank'],
{ windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'] });
let socket;
try {
    const debugUrl = await new Promise((resolve, reject) => {
        const timeout = setTimeout(() => reject(new Error('Browser startup timed out')), 20000);
        child.once('error', reject);
        child.stderr.on('data', data => {
            const match = data.toString().match(/DevTools listening on (ws:\/\/\S+)/);
            if (match) { clearTimeout(timeout); resolve(match[1]); }
        });
    });
    socket = new WebSocket(debugUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    let id = 0;
    const pending = new Map();
    const errors = [];
    socket.onmessage = event => {
        const message = JSON.parse(event.data);
        if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails);
        const call = pending.get(message.id);
        if (!call) return;
        clearTimeout(call.timeout); pending.delete(message.id);
        if (message.error) call.reject(new Error(JSON.stringify(message.error))); else call.resolve(message.result);
    };
    const cdp = (method, params = {}, sessionId) => new Promise((resolve, reject) => {
        const callId = ++id;
        const timeout = setTimeout(() => { pending.delete(callId); reject(new Error(`Timeout: ${method}`)); }, 20000);
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
    const waitFor = expression => evaluate(`new Promise((resolve,reject)=>{let tries=0;const timer=setInterval(()=>{
        if(${expression}){clearInterval(timer);resolve(true)}else if(++tries>200){clearInterval(timer);reject(new Error('Condition timed out'))}},25)})`);
    const click = selector => evaluate(`document.querySelector(${JSON.stringify(selector)}).click()`);
    await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });
    await send('Page.navigate', { url: origin });
    await waitFor(`document.querySelectorAll('#rmRows tr').length===3`);
    assert.equal(await evaluate(`document.querySelectorAll('#rmRows img').length`), 0, 'Resource names are escaped');
    for (const width of [1440, 1024, 768, 390, 320]) {
        await send('Emulation.setDeviceMetricsOverride', { width, height: 1000, deviceScaleFactor: 1, mobile: width < 600 });
        await evaluate('new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))');
        assert.ok(await evaluate('document.documentElement.scrollWidth<=window.innerWidth'), `${width}: no horizontal overflow`);
        const screenshot = await send('Page.captureScreenshot', { format: 'png' });
        await writeFile(join(artifacts, `resources-${width}.png`), Buffer.from(screenshot.data, 'base64'));
    }
    await click('#rmRows button[data-action="release"]');
    await waitFor(`document.getElementById('rmDialog').open`);
    await click('#rmCancel');
    assert.equal(actions.length, 0, 'Cancel leaves mapping unchanged');
    await click('#rmRows button[data-employee-id="3"][data-action="reassign"]');
    await waitFor(`document.getElementById('rmDialog').open`);
    assert.equal(await evaluate(`document.getElementById('rmManager').value`), '1', 'Existing reporting manager is preselected');
    await evaluate(`document.getElementById('rmManager').value='2'`);
    await click('#rmConfirm');
    await waitFor(`!document.getElementById('rmDialog').open && document.querySelector('button[data-employee-id="3"]').closest('tr').textContent.includes('Ananya Deshmukh')`);
    assert.equal(actions[0].action, 'reassign');
    assert.equal(actions[0].body.managerEmployeeId, 2);
    await click('#rmRows button[data-action="release"]');
    await click('#rmConfirm');
    await waitFor(`!document.getElementById('rmDialog').open && document.getElementById('rmMappedCount').textContent==='2'`);
    assert.equal(actions[1].action, 'release');
    await click('#rmBrowse');
    await waitFor(`document.querySelectorAll('#rmRows tr').length===12 && !document.getElementById('rmNext').disabled`);
    assert.equal(await evaluate(`document.getElementById('rmEmployeeType').value`), 'INTERNAL_MAHAIT');
    assert.equal(await evaluate(`document.getElementById('rmTypeFilter').hidden`), false);
    assert.equal(await evaluate(`document.getElementById('rmRows').textContent.includes('EXTERNAL')`), false);
    await click('#rmNext');
    await waitFor(`document.getElementById('rmRange').textContent.startsWith('13')`);
    const changeType = async value => {
        await evaluate(`document.getElementById('rmEmployeeType').value=${JSON.stringify(value)};document.getElementById('rmEmployeeType').dispatchEvent(new Event('change'))`);
    };
    await changeType('EXTERNAL');
    await waitFor(`document.getElementById('rmRange').textContent==='1–1 of 1 resources' && document.getElementById('rmRows').textContent.includes('EXTERNAL')`);
    assert.equal(await evaluate(`document.getElementById('rmPrevious').disabled`), true, 'Changing employee type resets pagination');
    await changeType('MAHAIT');
    await waitFor(`document.querySelectorAll('#rmRows tr').length===1 && document.getElementById('rmRows').textContent.includes('MAHAIT')`);
    await changeType('INTERNAL');
    await waitFor(`document.querySelectorAll('#rmRows tr').length===12 && document.getElementById('rmNext').disabled`);
    assert.equal(await evaluate(`document.getElementById('rmRows').textContent.includes('MAHAIT')`), false);
    await changeType('ALL');
    await waitFor(`document.getElementById('rmRange').textContent.endsWith('of 14 resources')`);
    for (const width of [1440, 390, 320]) {
        await send('Emulation.setDeviceMetricsOverride', { width, height: 1000, deviceScaleFactor: 1, mobile: width < 600 });
        await evaluate('new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))');
        assert.ok(await evaluate('document.documentElement.scrollWidth<=window.innerWidth'), `${width}: employee type filter fits screen`);
        const screenshot = await send('Page.captureScreenshot', { format: 'png' });
        await writeFile(join(artifacts, `free-resources-${width}.png`), Buffer.from(screenshot.data, 'base64'));
    }
    await changeType('INTERNAL_MAHAIT');
    await waitFor(`document.getElementById('rmRange').textContent==='1–12 of 13 resources'`);
    await click('#rmNext');
    await waitFor(`document.getElementById('rmRange').textContent.startsWith('13')`);
    await click('#rmPrevious');
    await waitFor(`document.getElementById('rmRange').textContent.startsWith('1–')`);
    await click('#rmRows button[data-employee-id="5"]');
    await waitFor(`document.getElementById('rmDialog').open`);
    await evaluate(`document.getElementById('rmManager').value='2'`);
    await click('#rmConfirm');
    await waitFor(`!document.getElementById('rmDialog').open && document.getElementById('rmMappedCount').textContent==='3'`);
    assert.equal(actions[2].action, 'map');
    assert.equal(actions[2].body.managerEmployeeId, 2);
    await evaluate(`document.getElementById('rmSearch').value='missing';document.getElementById('rmSearch').dispatchEvent(new Event('input'))`);
    await waitFor(`document.getElementById('rmState').textContent.includes('No matching resources')`);
    failNextRead = true;
    await click('#rmRefresh');
    await waitFor(`document.getElementById('rmState').textContent.includes('Temporary failure')`);
    await click('#rmRefresh');
    await waitFor(`document.getElementById('rmState').textContent.includes('No matching resources')`);
    assert.deepEqual(errors, [], 'No browser runtime errors');
    console.log(`PASS: desktop/mobile layouts, default Internal + MAHAIT, all employee-type filters, pagination reset, safe rendering, reassignment, release, mapping, CSRF header, search and retry. Screenshots: ${artifacts}`);
} finally {
    socket?.close(); child.kill(); server.closeAllConnections(); server.close();
}
