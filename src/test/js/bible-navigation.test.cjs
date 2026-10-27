const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const template = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/bible.html'), 'utf8');
const script = [...template.matchAll(/<script>([\s\S]*?)<\/script>/g)].at(-1)[1];

function page(read, authenticated = false) {
    const elements = {
        testamentSelect: { value: '' }, bookSelect: { value: '' }, chapterSelect: { value: '' },
        progressBooksContainer: { innerHTML: '' }, progressStatusFilter: { value: 'all' }
    };
    const context = vm.createContext({
        document: { addEventListener() {}, getElementById: id => elements[id] },
        localStorage: { getItem: () => null }, console
    });
    vm.runInContext(script, context);
    context.initialRead = read;
    context.authenticated = authenticated;
    vm.runInContext(`
        readChaptersSet = new Set(initialRead);
        isAuthenticated = authenticated;
        populateBooks = function () {};
        populateChapters = function () { document.getElementById('chapterSelect').value = String(currentChapter); };
        switchBibleView = function (mode) { globalThis.openedMode = mode; };
        loadBible = function () { globalThis.requestedChapter = currentChapter; };
        expandedBookKey = '1-1';
        renderProgressTable();
    `, context);
    return { context, elements };
}

test('the rendered read button continues at chapter 4 after chapters 1–3 for guests and members', () => {
    for (const authenticated of [false, true]) {
        const { context, elements } = page(['1-1-1', '1-1-2', '1-1-3'], authenticated);
        const button = elements.progressBooksContainer.innerHTML.match(/onclick="(openChapterFromProgress\([^"]+)"/);
        assert.ok(button, 'read action exists on the expanded book');
        vm.runInContext(button[1], context);
        assert.equal(context.requestedChapter, 4);
        assert.equal(elements.chapterSelect.value, '4');
        assert.equal(elements.testamentSelect.value, '1');
        assert.equal(elements.bookSelect.value, '1');
        assert.equal(context.openedMode, 'read');
        assert.equal(vm.runInContext('readChaptersSet.size', context), 3);
    }
});

test('skips no gaps: chapters 1 and 3 read continue at chapter 2', () => {
    const { context } = page(['1-1-1', '1-1-3']);
    vm.runInContext('openChapterFromProgress(1, 1)', context);
    assert.equal(context.requestedChapter, 2);
});

test('an unread book starts at chapter 1 and ignores progress in another book', () => {
    const { context } = page(['1-1-1', '1-1-2']);
    vm.runInContext('openChapterFromProgress(2, 1)', context);
    assert.equal(context.requestedChapter, 1);
});

test('a completed book restarts at chapter 1 instead of exceeding its chapter count', () => {
    const { context } = page(Array.from({ length: 50 }, (_, i) => `1-1-${i + 1}`));
    vm.runInContext('openChapterFromProgress(1, 1)', context);
    assert.equal(context.requestedChapter, 1);
});

test('an explicitly selected chapter still opens exactly that chapter', () => {
    const { context } = page(['1-1-1', '1-1-2', '1-1-3']);
    vm.runInContext('openChapterFromProgress(1, 1, 2)', context);
    assert.equal(context.requestedChapter, 2);
});
