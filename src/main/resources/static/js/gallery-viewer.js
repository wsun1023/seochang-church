(function () {
    function wrapIndex(index, direction, length) { return length ? (index + direction + length) % length : 0; }
    function swipeDirection(start, end) {
        const dx = end.x - start.x, dy = end.y - start.y;
        return Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy) * 1.5 ? (dx < 0 ? 1 : -1) : 0;
    }
    if (typeof module !== 'undefined' && module.exports) module.exports = { wrapIndex, swipeDirection };
    if (typeof document === 'undefined') return;
    const photos = Array.from(document.querySelectorAll('[data-gallery-photo]'));
    const dialog = document.getElementById('galleryLightbox');
    if (!photos.length || !dialog || typeof dialog.showModal !== 'function') return;
    const image = document.getElementById('lightboxImage');
    const previous = document.getElementById('lightboxPrevious');
    const next = document.getElementById('lightboxNext');
    const stage = document.getElementById('lightboxStage');
    let index = 0;
    let trigger;
    let start;
    let originalOverflow;
    function showPhoto() {
        image.src = photos[index].href;
        image.alt = photos[index].dataset.caption;
        document.getElementById('lightboxCaption').textContent = photos[index].dataset.caption;
        document.getElementById('lightboxCounter').textContent = `${index + 1} / ${photos.length}`;
        previous.disabled = next.disabled = photos.length < 2;
    }
    function move(direction) { index = wrapIndex(index, direction, photos.length); showPhoto(); }
    photos.forEach((photo, number) => photo.addEventListener('click', event => {
        if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault(); index = number; trigger = photo; showPhoto();
        originalOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        dialog.showModal();
    }));
    previous.addEventListener('click', () => move(-1));
    next.addEventListener('click', () => move(1));
    document.getElementById('lightboxClose').addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => { document.body.style.overflow = originalOverflow; trigger?.focus(); });
    dialog.addEventListener('keydown', event => {
        if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') { event.preventDefault(); move(event.key === 'ArrowLeft' ? -1 : 1); }
    });
    stage.addEventListener('touchstart', event => {
        start = event.touches.length === 1 ? { x: event.touches[0].clientX, y: event.touches[0].clientY } : null;
    }, { passive: true });
    stage.addEventListener('touchmove', event => { if (event.touches.length > 1) start = null; }, { passive: true });
    stage.addEventListener('touchcancel', () => { start = null; });
    stage.addEventListener('touchend', event => {
        if (start && event.changedTouches.length === 1) {
            const direction = swipeDirection(start, { x: event.changedTouches[0].clientX, y: event.changedTouches[0].clientY });
            if (direction) move(direction);
        }
        start = null;
    }, { passive: true });
})();
