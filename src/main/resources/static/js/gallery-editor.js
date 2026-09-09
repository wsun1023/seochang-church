(function (root) {
    function makePlan(items, coverKey, removedIds) {
        if (!items.length || items.length > 50) throw new Error('사진은 최소 1장, 최대 50장까지 등록할 수 있습니다.');
        const files = [];
        const tokens = new Map();
        const order = items.map(item => {
            const token = item.file ? `new:${files.push(item.file) - 1}` : `existing:${item.id}`;
            tokens.set(item.key, token);
            return token;
        });
        if (!tokens.has(coverKey)) throw new Error('대표 사진을 선택해주세요.');
        return { files, order, cover: tokens.get(coverKey), removedIds: [...removedIds] };
    }
    function movePhoto(items, key, direction) {
        const index = items.findIndex(item => item.key === key);
        const next = index + direction;
        if (index < 0 || next < 0 || next >= items.length) return items;
        const result = [...items];
        [result[index], result[next]] = [result[next], result[index]];
        return result;
    }
    if (typeof module !== 'undefined' && module.exports) module.exports = { makePlan, movePhoto };
    if (typeof document === 'undefined') return;
    const form = document.getElementById('galleryForm');
    if (!form) return;
    const grid = document.getElementById('photoGrid');
    const input = document.getElementById('imageFiles');
    const feedback = document.getElementById('galleryFeedback');
    const removedPanel = document.getElementById('removedPhotos');
    const progress = document.getElementById('galleryProgress');
    const progressLabel = document.getElementById('galleryProgressLabel');
    const progressBar = document.getElementById('uploadProgress');
    let items = Array.from(grid.querySelectorAll('[data-id]')).map(card => ({
        key: `existing:${card.dataset.id}`, id: card.dataset.id, url: card.dataset.url, name: card.dataset.name
    }));
    let coverKey = form.dataset.coverKey || items[0]?.key;
    let sequence = 0;
    let removed = [];
    let saving = false;
    function error(message) { feedback.textContent = message; feedback.hidden = false; }
    function button(text, action, label) {
        const result = document.createElement('button');
        result.type = 'button'; result.className = 'btn btn-sm btn-outline-secondary';
        result.textContent = text;
        if (label) result.setAttribute('aria-label', label);
        result.addEventListener('click', action);
        return result;
    }
    function render(focusKey, focusAction) {
        grid.replaceChildren();
        if (!items.some(item => item.key === coverKey)) coverKey = items[0]?.key;
        document.getElementById('photoCount').textContent = String(items.length);
        document.getElementById('photoEmpty').hidden = items.length > 0;
        items.forEach((item, index) => {
            const card = document.createElement('article');
            card.className = 'gallery-photo-card' + (item.key === coverKey ? ' is-cover' : '');
            const img = document.createElement('img'); img.src = item.url; img.alt = item.name; img.loading = 'lazy';
            const controls = document.createElement('div'); controls.className = 'gallery-photo-controls';
            const name = document.createElement('p'); name.className = 'gallery-photo-name'; name.textContent = `${index + 1}. ${item.name}`; name.title = item.name;
            const cover = button(item.key === coverKey ? '✓ 대표 사진' : '대표로 선택', () => { coverKey = item.key; render(item.key, 'cover'); });
            cover.className = 'btn btn-sm w-100 mb-2 ' + (item.key === coverKey ? 'btn-primary' : 'btn-outline-primary');
            cover.setAttribute('aria-pressed', String(item.key === coverKey));
            const row = document.createElement('div'); row.className = 'd-flex gap-1 flex-wrap';
            const before = button('←', () => { items = movePhoto(items, item.key, -1); render(item.key, 'before'); }, `${index + 1}번 사진 앞으로`);
            const after = button('→', () => { items = movePhoto(items, item.key, 1); render(item.key, 'after'); }, `${index + 1}번 사진 뒤로`);
            before.disabled = index === 0; after.disabled = index === items.length - 1;
            const remove = button('삭제', () => {
                items = items.filter(photo => photo.key !== item.key);
                if (item.file) URL.revokeObjectURL(item.url); else removed.push(item);
                render();
            }, `${item.name} 삭제 예약`);
            remove.className = 'btn btn-sm btn-outline-danger ms-auto';
            row.append(before, after, remove); controls.append(name, cover, row); card.append(img, controls); grid.append(card);
            if (item.key === focusKey) {
                const target = { cover, before, after }[focusAction];
                if (target && !target.disabled) target.focus(); else cover.focus();
            }
        });
        removedPanel.replaceChildren();
        removed.forEach(item => {
            const row = document.createElement('div'); row.className = 'd-flex gap-2 align-items-center mb-2 small';
            const label = document.createElement('span'); label.textContent = `삭제 예정: ${item.name}`;
            row.append(label, button('삭제 취소', () => {
                if (items.length >= 50) return error('사진은 최대 50장입니다. 다른 사진을 제거한 후 복원해주세요.');
                removed = removed.filter(photo => photo.key !== item.key); items.push(item); render(item.key, 'cover');
            }));
            removedPanel.append(row);
        });
    }
    input.addEventListener('change', () => {
        const selected = Array.from(input.files);
        if (items.length + selected.length > 50) { error('기존 사진을 포함해 최대 50장까지 선택할 수 있습니다.'); input.value = ''; return; }
        if (selected.some(file => !file.type.startsWith('image/'))) { error('이미지 파일만 선택해주세요.'); input.value = ''; return; }
        feedback.hidden = true;
        selected.forEach(file => items.push({ key: `upload:${sequence++}`, file, name: file.name, url: URL.createObjectURL(file) }));
        input.value = ''; render();
    });
    function busy(value) {
        saving = value;
        form.querySelectorAll('button, input, textarea').forEach(control => { control.disabled = value; });
        if (!value) render();
    }
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (saving || !form.reportValidity()) return;
        feedback.hidden = true;
        let plan;
        try { plan = makePlan(items, coverKey, removed.map(item => item.id)); }
        catch (e) { error(e.message); return; }
        const data = new FormData(form);
        data.delete('imageFiles');
        plan.order.forEach(token => data.append('photoOrder', token));
        data.append('coverPhoto', plan.cover);
        plan.removedIds.forEach(id => data.append('deleteFileIds', id));
        busy(true); progress.hidden = false; progressBar.value = 0;
        try {
            for (let i = 0; i < plan.files.length; i++) {
                let file = plan.files[i];
                progressLabel.textContent = `사진 준비 중 ${i + 1} / ${plan.files.length}`;
                if (typeof root.imageCompression === 'function') {
                    try { file = await root.imageCompression(file, { maxSizeMB: 5, maxWidthOrHeight: 1280, useWebWorker: true }); }
                    catch (_) { /* The existing server image validation handles original files. */ }
                }
                data.append('imageFiles', file, plan.files[i].name);
            }
            progressLabel.textContent = '사진첩을 저장하고 있습니다.';
            const xhr = new XMLHttpRequest();
            xhr.open('POST', form.action);
            xhr.timeout = 180000;
            xhr.upload.onprogress = e => { if (e.lengthComputable) progressBar.value = Math.round(e.loaded / e.total * 100); };
            const done = () => { progress.hidden = true; busy(false); };
            xhr.onload = () => {
                if (xhr.status === 200 && /\/gallery\/\d+$/.test(new URL(xhr.responseURL).pathname)) {
                    window.location.assign(xhr.responseURL); return;
                }
                let message = '사진첩을 저장하지 못했습니다. 로그인 상태와 선택한 사진을 확인해주세요.';
                try { message = JSON.parse(xhr.responseText).message || message; } catch (_) {}
                error(message); done();
            };
            xhr.onerror = xhr.ontimeout = () => { error('연결이 원활하지 않습니다. 입력 내용은 유지되며, 잠시 후 다시 저장할 수 있습니다.'); done(); };
            xhr.send(data);
        } catch (_) { error('사진을 준비하지 못했습니다. 다시 시도해주세요.'); progress.hidden = true; busy(false); }
    });
    render();
})(typeof window === 'undefined' ? globalThis : window);
