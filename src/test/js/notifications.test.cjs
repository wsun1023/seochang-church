const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const source = fs.readFileSync('src/main/resources/static/js/notifications.js', 'utf8');
const settle = () => new Promise(resolve => setImmediate(resolve));

function badgePage(fetch) {
    const badge = { hidden: true, textContent: '' };
    const link = { dataset: { countUrl: '/api/notifications/unread-count' },
        querySelector: () => badge, setAttribute(name, value) { this[name] = value; } };
    const events = {};
    const document = { hidden: false, querySelectorAll: () => [link], addEventListener(name, handler) { events[name] = handler; } };
    let poll;
    vm.runInNewContext(source, { document, fetch,
        window: { addEventListener(name, handler) { events[name] = handler; } },
        setInterval(handler, delay) { assert.equal(delay, 30000); poll = handler; } });
    return { badge, link, document, events, poll: () => poll() };
}

test('badge shows unread count, caps display at 99+, and hides at zero', async () => {
    let count = 125;
    const page = badgePage(async (url, options) => {
        assert.equal(url, '/api/notifications/unread-count');
        assert.equal(options.cache, 'no-store');
        return { ok: true, status: 200, json: async () => ({ count }) };
    });
    await settle();
    assert.equal(page.badge.textContent, '99+');
    assert.equal(page.badge.hidden, false);
    assert.match(page.link['aria-label'], /125개/);
    count = 0;
    await page.poll();
    assert.equal(page.badge.hidden, true);
});

test('background tabs do not poll and visibility resumes refresh', async () => {
    let calls = 0;
    const page = badgePage(async () => { calls++; return { ok: true, status: 200, json: async () => ({ count: 1 }) }; });
    await settle();
    page.document.hidden = true;
    await page.poll();
    assert.equal(calls, 1);
    page.document.hidden = false;
    await page.events.visibilitychange();
    assert.equal(calls, 2);
    await page.events.pageshow();
    assert.equal(calls, 3);
});

test('sign-out clears badge and stops subsequent requests', async () => {
    let calls = 0;
    let status = 200;
    const page = badgePage(async () => { calls++; return { ok: status === 200, status, json: async () => ({ count: 3 }) }; });
    await settle();
    status = 401;
    await page.poll();
    assert.equal(page.badge.hidden, true);
    await page.poll();
    assert.equal(calls, 2);
});

test('temporary failures and invalid counts preserve the last count', async () => {
    let result = { ok: true, status: 200, json: async () => ({ count: 2 }) };
    const page = badgePage(async () => { if (result === null) throw new Error('offline'); return result; });
    await settle();
    result = null;
    await page.poll();
    assert.equal(page.badge.textContent, '2');
    result = { ok: true, status: 200, json: async () => ({ count: '<script>' }) };
    await page.poll();
    assert.equal(page.badge.textContent, '2');
});
