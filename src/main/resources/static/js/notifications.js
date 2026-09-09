(() => {
    const links = Array.from(document.querySelectorAll('[data-notification-link]'));
    if (links.length === 0) return;
    let pending = false;
    let signedOut = false;
    async function refresh() {
        if (pending || signedOut || document.hidden) return;
        pending = true;
        try {
            const response = await fetch(links[0].dataset.countUrl, { credentials: 'same-origin', cache: 'no-store' });
            if (response.status === 401) {
                signedOut = true;
                links.forEach(link => { link.querySelector('[data-notification-badge]').hidden = true; link.setAttribute('aria-label', '알림'); });
                return;
            }
            if (!response.ok) return;
            const { count } = await response.json();
            if (!Number.isSafeInteger(count) || count < 0) return;
            links.forEach(link => {
                const badge = link.querySelector('[data-notification-badge]');
                badge.textContent = count > 99 ? '99+' : String(count);
                badge.hidden = count === 0;
                link.setAttribute('aria-label', count ? `알림, 읽지 않은 알림 ${count}개` : '알림, 읽지 않은 알림 없음');
            });
        } catch (_) {
            // Keep the last known count if the network is temporarily unavailable.
        } finally { pending = false; }
    }
    refresh();
    setInterval(refresh, 30000);
    document.addEventListener('visibilitychange', refresh);
    window.addEventListener('pageshow', refresh);
})();
