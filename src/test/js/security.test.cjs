const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');

test('CSRF header goes only to same-origin unsafe requests', () => {
    const calls = [];
    const window = { location: { href: 'https://church.test/boards', origin: 'https://church.test' }, fetch: (...args) => calls.push(args) };
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/csrf.js', 'utf8'), {
        window, document: { querySelector: () => ({ content: 'token' }) }, URL, Headers, Request
    });
    window.fetch('/api/boards/1/like', { method: 'POST', headers: { 'Content-Type': 'application/json' } });
    assert.equal(calls[0][1].headers.get('X-CSRF-TOKEN'), 'token');
    assert.equal(calls[0][1].headers.get('Content-Type'), 'application/json');
    window.fetch('https://external.test/upload', { method: 'POST' });
    assert.equal(calls[1][1].headers, undefined);
    window.fetch('/boards');
    assert.equal(calls[2][1].headers, undefined);
    window.fetch(new Request('https://church.test/api/boards/1/comments', { method: 'DELETE' }));
    assert.equal(calls[3][1].headers.get('X-CSRF-TOKEN'), 'token');
});
