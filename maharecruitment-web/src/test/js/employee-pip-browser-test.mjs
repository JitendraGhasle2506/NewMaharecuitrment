// Run EmployeePipViewsTest first, then run this script from the repository root.
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
const artifacts = await mkdtemp(join(tmpdir(), 'employee-pip-test-'));
let html = await readFile('maharecruitment-web/target/pip-board-preview.html', 'utf8');
html = html.replace('</head>', '<link rel="stylesheet" href="/portal/assets/vendor/bootstrap/bootstrap.min.css"><style>body{background:#f5f7fb;font-family:system-ui}</style></head>');
const detailHtml = (await readFile('maharecruitment-web/target/pip-detail-preview.html', 'utf8')).replace('</head>', '<link rel="stylesheet" href="/portal/assets/vendor/bootstrap/bootstrap.min.css"><style>body{background:#f5f7fb;font-family:system-ui}</style></head>');
const closedHtml = (await readFile('maharecruitment-web/target/pip-closed-preview.html', 'utf8')).replace('</head>', '<link rel="stylesheet" href="/assets/vendor/bootstrap/bootstrap.min.css"></head>');
const server = createServer(async (request, response) => {
    try {
        const pathname = new URL(request.url, 'http://localhost').pathname;
        if (pathname === '/portal/reporting-manager/pip/team-preview') {
            response.setHeader('Content-Type', 'application/json');
            response.end(JSON.stringify([{ id: 1, label: 'Ananya Deshmukh (EMP001)' }, { id: 5, label: 'Member Five (EMP005)' }]));
            return;
        }
        if (['/portal/reporting-manager/pip', '/portal/reporting-manager/pip/100', '/portal/reporting-manager/pip/101'].includes(pathname)) {
            response.setHeader('Content-Type', 'text/html; charset=utf-8');
            response.end(pathname.endsWith('/100') ? detailHtml : pathname.endsWith('/101') ? closedHtml : html);
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
    await send('Page.navigate', { url: `http://127.0.0.1:${server.address().port}/portal/reporting-manager/pip` });
    const ready = () => evaluate(`new Promise((resolve, reject) => {
        let n=0; const timer=setInterval(()=>{
            if(document.readyState === 'complete' && document.querySelector('.pip-page')) {clearInterval(timer);resolve(true);}
            else if(++n>200){clearInterval(timer);reject(new Error('PIP page not ready'));}
        },25);
    })`);
    const screenshots = async (page) => {
        for(const width of [1440,768,375,320]) {
            await send('Emulation.setDeviceMetricsOverride',{width,height:1080,deviceScaleFactor:1,mobile:false});
            await evaluate('new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))');
            assert.equal(await evaluate('document.documentElement.scrollWidth <= window.innerWidth + 1'),true,`${page} overflow at ${width}px`);
            const shot=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:true});
            await writeFile(join(artifacts,`${page}-${width}.png`),Buffer.from(shot.data,'base64'));
        }
    };
    await ready();
    assert.equal(await evaluate(`document.querySelectorAll('.select2-container').length`),1);
    await evaluate(`window.jQuery('#employeeId').select2('open'); document.querySelector('.select2-search__field').value='EMP002'; document.querySelector('.select2-search__field').dispatchEvent(new Event('input',{bubbles:true}));`);
    await evaluate('new Promise(resolve=>setTimeout(resolve,50))');
    assert.equal(await evaluate(`document.querySelector('.select2-results').textContent.includes('Team <Member>')`),true);
    await evaluate(`window.jQuery('#employeeId').select2('close'); window.jQuery('#employeeId').val('2').trigger('change'); document.getElementById('objectives').value='Improve delivery';`);
    assert.equal(await evaluate(`document.getElementById('objectives').form.checkValidity()`),false);
    await evaluate(`document.getElementById('reason').value='Repeated missed deadlines';`);
    assert.equal(await evaluate(`document.getElementById('objectives').form.checkValidity()`),true);
    await screenshots('board');
    await send('Page.navigate',{url:`http://127.0.0.1:${server.address().port}/portal/reporting-manager/pip/100`});
    await ready();
    assert.equal(await evaluate(`document.getElementById('exitDate').disabled`),true);
    await evaluate(`document.getElementById('decision').value='CONTINUE'; document.getElementById('decision').dispatchEvent(new Event('change')); document.getElementById('decisionFeedback').value='Meets expectations'; document.getElementById('confirmed').checked=true;`);
    assert.equal(await evaluate(`document.getElementById('pipDecisionForm').checkValidity()`),true);
    await evaluate(`document.getElementById('decision').value='EXIT'; document.getElementById('decision').dispatchEvent(new Event('change'));`);
    assert.equal(await evaluate(`document.getElementById('pipDecisionForm').checkValidity()`),false);
    assert.equal(await evaluate(`document.getElementById('exitDate').required`),true);
    await evaluate(`document.getElementById('exitDate').value=document.getElementById('exitDate').max;`);
    assert.equal(await evaluate(`document.getElementById('pipDecisionForm').checkValidity()`),true);
    await evaluate(`document.getElementById('confirmed').checked=false;`);
    assert.equal(await evaluate(`document.getElementById('pipDecisionForm').checkValidity()`),false);
    assert.equal(await evaluate(`document.querySelector('.pip-review .pip-feedback').textContent.includes('<script>')`),true);
    await screenshots('detail');
    await send('Page.navigate',{url:`http://127.0.0.1:${server.address().port}/portal/reporting-manager/pip/101`});
    await ready();
    assert.equal(await evaluate(`document.querySelectorAll('form').length`),0);
    assert.equal(await evaluate(`document.body.textContent.includes('Exit recorded')`),true);
    assert.deepEqual(exceptions,[]);
    console.log(`PASS: Searchable subordinate picker, review/outcome forms, confirmation and exit-date rules, read-only completed history, safe feedback and 320-1440px layouts. Screenshots: ${artifacts}`);
} finally {
    if(socket)socket.close(); browserProcess.kill(); server.closeAllConnections(); server.close();
}