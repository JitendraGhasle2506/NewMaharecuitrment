// Run EmployeeExitTemplateTest first, then run this script from the repository root.
// Serves only synthetic rendered data and static assets; no application database is used.
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { readFile, mkdtemp, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { join, resolve, extname, sep } from 'node:path';
import { tmpdir } from 'node:os';
import { spawn } from 'node:child_process';

const base = resolve('maharecruitment-web/src/main/resources/static');
const browser = process.env.CHROME_BIN || [
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
].find(existsSync);
assert.ok(browser, 'Set CHROME_BIN to a Chromium browser');
const artifacts = await mkdtemp(join(tmpdir(), 'employee-exit-test-'));
let html = await readFile('artifacts/hr-employee-exit-preview.html', 'utf8');
html = html.replace('</head>', '<link rel="stylesheet" href="/portal/assets/vendor/bootstrap/bootstrap.min.css"><style>body{background:#f5f7fb;font-family:system-ui}</style></head>');
const server = createServer(async (request, response) => {
    try {
        const pathname = new URL(request.url, 'http://localhost').pathname;
        if (pathname === '/portal/hr/employees/5/exit/team-preview') {
            response.setHeader('Content-Type', 'application/json');
            response.end(JSON.stringify([{ id: 1, label: 'Ananya Deshmukh (EMP001)' }, { id: 5, label: 'Member Five (EMP005)' }]));
            return;
        }
        if (pathname === '/portal/hr/employees/5/exit') {
            response.setHeader('Content-Type', 'text/html; charset=utf-8');
            response.end(html);
            return;
        }
        const file = resolve(base, `.${pathname.replace(/^\/portal/, '')}`);
        if (!file.startsWith(base + sep)) { response.writeHead(403).end(); return; }
        response.setHeader('Content-Type', ({ '.css': 'text/css', '.js': 'text/javascript' })[extname(file)] || 'application/octet-stream');
        response.end(await readFile(file));
    } catch { response.writeHead(404).end(); }
});
await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
const browserProcess = spawn(browser, ['--headless=new', '--disable-gpu', '--no-first-run',
    '--no-default-browser-check', '--remote-debugging-port=0', `--user-data-dir=${join(artifacts, 'profile')}`, 'about:blank'],
    { windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'] });
let socket;
try {
    const debugUrl = await new Promise((resolve, reject) => {
        const timeout = setTimeout(() => reject(new Error('Browser startup timed out')), 20000);
        browserProcess.once('error', reject);
        browserProcess.stderr.on('data', data => {
            const match = data.toString().match(/DevTools listening on (ws:\/\/\S+)/);
            if (match) { clearTimeout(timeout); resolve(match[1]); }
        });
    });
    socket = new WebSocket(debugUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    const pending = new Map(), exceptions = [];
    let id = 0;
    socket.onmessage = event => {
        const message = JSON.parse(event.data);
        if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails.text);
        const task = pending.get(message.id);
        if (!task) return;
        pending.delete(message.id); clearTimeout(task.timeout);
        message.error ? task.reject(new Error(JSON.stringify(message.error))) : task.resolve(message.result);
    };
    const cdp = (method, params = {}, sessionId) => new Promise((resolve, reject) => {
        const callId = ++id;
        const timeout = setTimeout(() => { pending.delete(callId); reject(new Error(method + ' timed out')); }, 20000);
        pending.set(callId, { resolve, reject, timeout });
        socket.send(JSON.stringify({ id: callId, method, params, sessionId }));
    });
    const { targetId } = await cdp('Target.createTarget', { url: 'about:blank' });
    const { sessionId } = await cdp('Target.attachToTarget', { targetId, flatten: true });
    const send = (method, params) => cdp(method, params, sessionId);
    const evaluate = async expression => {
        const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
        if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails));
        return result.result.value;
    };
    await send('Runtime.enable');
    await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1080, deviceScaleFactor: 1, mobile: false });
    await send('Page.navigate', { url: `http://127.0.0.1:${server.address().port}/portal/hr/employees/5/exit` });
    await evaluate(`new Promise((resolve, reject) => {
        let n = 0; const timer = setInterval(() => {
            if (document.getElementById('confirmed')) { clearInterval(timer); resolve(true); }
            else if (++n > 200) { clearInterval(timer); reject(new Error('Exit form not ready')); }
        }, 25);
    })`);
    assert.equal(await evaluate(`document.querySelector('form').checkValidity()`), false);
    await evaluate(`window.submissions = 0;
        document.querySelector('form').addEventListener('submit', event => {event.preventDefault(); window.submissions++;});
        document.querySelector('button[type=submit]').click();`);
    assert.equal(await evaluate('window.submissions'), 0);
    await evaluate(`document.getElementById('reason').value = 'Employee left the organisation'; document.getElementById('confirmed').checked = true;`);
    assert.equal(await evaluate(`document.querySelector('form').checkValidity()`), true);
    assert.equal(await evaluate(`document.getElementById('reason').maxLength`), 1000);
    assert.equal(await evaluate(`(() => {
        const field = document.getElementById('lastWorkingDate');
        const next = new Date(field.max + 'T00:00:00Z'); next.setUTCDate(next.getUTCDate() + 1);
        field.value = next.toISOString().slice(0,10); return field.checkValidity();
    })()`), false);
    await evaluate(`document.getElementById('lastWorkingDate').value = document.getElementById('lastWorkingDate').max; document.getElementById('confirmed').checked = false;`);
    assert.equal(await evaluate(`document.querySelector('form').checkValidity()`), false);
    await evaluate(`document.getElementById('confirmed').checked = true; document.querySelector('button[type=submit]').click();`);
    assert.equal(await evaluate('window.submissions'), 1);
    for (const width of [1440, 768, 375, 320]) {
        await send('Emulation.setDeviceMetricsOverride', { width, height: 1080, deviceScaleFactor: 1, mobile: false });
        await evaluate('new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))');
        assert.equal(await evaluate('document.documentElement.scrollWidth <= window.innerWidth + 1'), true, `Overflow at ${width}px`);
        const screenshot = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true });
        await writeFile(join(artifacts, `${width}.png`), Buffer.from(screenshot.data, 'base64'));
    }
    assert.deepEqual(exceptions, []);
    console.log(`PASS: Exit form validation, required confirmation, past/today date limits, safe rendering, and 320-1440px responsive layouts. Screenshots: ${artifacts}`);
} finally {
    if (socket) socket.close();
    browserProcess.kill();
    server.closeAllConnections();
    server.close();
}