const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { test } = require('node:test');

const script = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/auditor-application-review.js'), 'utf8');

function setup() {
    const buttons = [{ value: 'APPROVE' }, { value: 'SEND_BACK' }];
    const remarks = { value: '', focus() { this.focused = true; } };
    const state = { submitted: 0, prompts: [], inputs: [], buttons, remarks };
    const form = {
        addEventListener(type, handler) { state.handler = handler; },
        querySelector() { return remarks; },
        querySelectorAll() { return buttons; },
        appendChild(input) { state.inputs.push(input); }
    };
    vm.runInNewContext(script, {
        document: {
            addEventListener(type, handler) { handler(); },
            getElementById() { return form; },
            createElement() { return {}; }
        },
        Swal: { fire(options) {
            state.prompts.push(options);
            return new Promise(resolve => { state.answer = resolve; });
        } },
        HTMLFormElement: { prototype: { submit() { state.submitted++; } } }
    });
    state.click = (index = 0) => state.handler({ submitter: buttons[index], preventDefault() {} });
    return state;
}

test('approval waits for confirmation and submits only once despite repeated clicks', async () => {
    const state = setup();
    const first = state.click();
    await state.click();
    assert.equal(state.prompts.length, 1);
    assert.match(state.prompts[0].text, /final approval/);
    assert.match(state.prompts[0].text, /cannot be changed or submitted again/);
    assert.equal(state.submitted, 0);
    state.answer({ isConfirmed: true });
    await first;
    await state.click();
    assert.equal(state.submitted, 1);
    assert.equal(state.inputs.length, 1);
    assert.equal(state.inputs[0].name, 'decision');
    assert.equal(state.inputs[0].value, 'APPROVE');
    assert.ok(state.buttons.every(button => button.disabled));
});

test('cancelling leaves the form editable and allows a later confirmation', async () => {
    const state = setup();
    const first = state.click();
    state.answer({ isConfirmed: false });
    await first;
    assert.equal(state.submitted, 0);
    assert.equal(state.inputs.length, 0);
    assert.ok(state.buttons.every(button => !button.disabled));
    const second = state.click();
    assert.equal(state.prompts.length, 2);
    state.answer({ isConfirmed: true });
    await second;
    assert.equal(state.submitted, 1);
});

test('send back requires remarks and preserves the selected decision on submission', async () => {
    const state = setup();
    const invalid = state.click(1);
    assert.equal(state.prompts[0].title, 'Remarks required');
    state.answer({});
    await invalid;
    assert.equal(state.submitted, 0);
    assert.ok(state.remarks.focused);
    state.remarks.value = 'Please correct the details';
    const valid = state.click(1);
    state.answer({ isConfirmed: true });
    await valid;
    await state.click(1);
    assert.equal(state.submitted, 1);
    assert.equal(state.inputs[0].value, 'SEND_BACK');
});
