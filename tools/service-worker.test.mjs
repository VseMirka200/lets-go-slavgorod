import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { existsSync, readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

test('installation precaches existing pages and assets', async () => {
    const handlers = {};
    let shell = [];
    let skipped = false;
    const self = { addEventListener: (name, fn) => { handlers[name] = fn; }, skipWaiting() { skipped = true; } };
    const caches = { open: async () => ({ addAll: async paths => { shell = paths; } }) };
    runInNewContext(readFileSync('docs/sw.js', 'utf8'), { self, caches });
    let completion;
    handlers.install({ waitUntil: promise => { completion = promise; } });
    await completion;
    assert.equal(skipped, true);
    for (const path of shell) assert.ok(existsSync(`docs/${path}`), `Отсутствует ресурс ${path}`);
    assert.ok(shell.includes('./feedback.html'));
    assert.ok(shell.includes('./assets/scripts/features/site-search.js'));
});

test('activation removes only obsolete application caches', async () => {
    const handlers = {};
    const removed = [];
    const self = { location: { origin: 'https://example.org' }, clients: { claim() {} }, skipWaiting() {}, addEventListener: (name, fn) => { handlers[name] = fn; } };
    const caches = { keys: async () => ['other-app', 'lets-go-slavgorod-web-v40', 'lets-go-slavgorod-web-v41'], delete: async key => removed.push(key) };
    runInNewContext(readFileSync('docs/sw.js', 'utf8'), { self, caches, URL });
    let completion;
    handlers.activate({ waitUntil: promise => { completion = promise; } });
    await completion;
    assert.deepEqual(removed, ['lets-go-slavgorod-web-v40']);
});

test('does not cache failed HTTP responses', async () => {
    const handlers = {};
    let writes = 0;
    const self = { location: { origin: 'https://example.org' }, addEventListener: (name, fn) => { handlers[name] = fn; } };
    const caches = { match: async () => null, open: async () => ({ put: () => { writes++; } }) };
    const response = { ok: false };
    runInNewContext(readFileSync('docs/sw.js', 'utf8'), { self, caches, URL, fetch: async () => response });
    let completion;
    handlers.fetch({ request: { method: 'GET', url: 'https://example.org/fail' }, respondWith: promise => { completion = promise; } });
    assert.equal(await completion, response);
    assert.equal(writes, 0);
});

test('navigation can use cached shell when route query changes offline', async () => {
    const handlers = {};
    const self = { location: { origin: 'https://example.org' }, addEventListener: (name, fn) => { handlers[name] = fn; } };
    const cached = { ok: true };
    let options;
    const caches = { match: async (_request, value) => { options = value; return cached; } };
    runInNewContext(readFileSync('docs/sw.js', 'utf8'), { self, caches, URL });
    let completion;
    handlers.fetch({ request: { method: 'GET', mode: 'navigate', url: 'https://example.org/schedule.html?route=1' }, respondWith: promise => { completion = promise; } });
    assert.equal(await completion, cached);
    assert.equal(options.ignoreSearch, true);
});
