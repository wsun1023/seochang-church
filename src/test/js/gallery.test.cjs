const { test } = require('node:test');
const assert = require('node:assert/strict');
const { makePlan, movePhoto } = require('../../main/resources/static/js/gallery-editor.js');
const { wrapIndex, swipeDirection } = require('../../main/resources/static/js/gallery-viewer.js');

test('reordered existing and new photos map to exact multipart file indexes', () => {
    const a = { key: 'upload:12', file: { name: 'a.jpg' } };
    const b = { key: 'upload:3', file: { name: 'b.jpg' } };
    const saved = { key: 'existing:10', id: '10' };
    const plan = makePlan([b, saved, a], a.key, ['15']);
    assert.deepEqual(plan.order, ['new:0', 'existing:10', 'new:1']);
    assert.deepEqual(plan.files, [b.file, a.file]);
    assert.equal(plan.cover, 'new:1');
    assert.deepEqual(plan.removedIds, ['15']);
});

test('moving photos retains the independently selected cover', () => {
    const items = [{ key: 'a', id: '1' }, { key: 'b', id: '2' }];
    const moved = movePhoto(items, 'b', -1);
    assert.deepEqual(moved.map(item => item.key), ['b', 'a']);
    assert.deepEqual(items.map(item => item.key), ['a', 'b']);
    assert.equal(makePlan(moved, 'a', []).cover, 'existing:1');
    assert.equal(movePhoto(items, 'a', -1), items);
});

test('empty albums, too many photos and missing covers cannot be submitted', () => {
    assert.throws(() => makePlan([], '', []));
    assert.throws(() => makePlan(Array.from({ length: 51 }, (_, i) => ({ key: String(i), id: i })), '0', []));
    assert.throws(() => makePlan([{ key: 'a', id: 1 }], 'deleted', []));
});

test('viewer wraps around ends and single-photo albums stay on the same photo', () => {
    assert.equal(wrapIndex(0, -1, 3), 2);
    assert.equal(wrapIndex(2, 1, 3), 0);
    assert.equal(wrapIndex(0, 1, 1), 0);
});

test('only clear horizontal swipes change photos', () => {
    assert.equal(swipeDirection({ x: 100, y: 100 }, { x: 0, y: 105 }), 1);
    assert.equal(swipeDirection({ x: 100, y: 100 }, { x: 200, y: 95 }), -1);
    assert.equal(swipeDirection({ x: 100, y: 100 }, { x: 105, y: 200 }), 0);
    assert.equal(swipeDirection({ x: 100, y: 100 }, { x: 110, y: 100 }), 0);
});
