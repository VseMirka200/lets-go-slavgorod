import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

test('поиск открывается и находит раздел сайта', () => {
    const handlers = {};
    const trigger = { addEventListener: (event, callback) => { handlers[event] = callback; } };
    const input = { value: '', addEventListener: (event, callback) => { handlers[`input-${event}`] = callback; }, focus() {} };
    const results = { children: [], replaceChildren() { this.children = []; }, appendChild(child) { this.children.push(child); } };
    const button = { addEventListener() {} };
    const dialog = {
        open: false,
        setAttribute() {},
        querySelector(selector) { return selector === '#search-input' ? input : selector === '#results' ? results : button; },
        showModal() { this.open = true; },
        close() { this.open = false; }
    };
    const document = {
        querySelector: () => trigger,
        createElement: (name) => name === 'dialog' ? dialog : {},
        body: { appendChild() {} },
        addEventListener() {}
    };
    runInNewContext(readFileSync('docs/assets/scripts/features/site-search.js', 'utf8'), { document });
    handlers.click();
    assert.equal(dialog.open, true);
    input.value = 'обратная';
    handlers['input-input']();
    assert.equal(results.children.length, 1);
    assert.equal(results.children[0].href, 'feedback.html');
});
