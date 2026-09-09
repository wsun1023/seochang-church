(() => {
  // Dynamically ensure PWA tags exist in head for any page
  if (!document.querySelector('link[rel="manifest"]')) {
    const link = document.createElement('link');
    link.rel = 'manifest';
    link.href = '/manifest.json';
    document.head.appendChild(link);
  }
  if (!document.querySelector('meta[name="theme-color"]')) {
    const meta = document.createElement('meta');
    meta.name = 'theme-color';
    meta.content = '#0F172A';
    document.head.appendChild(meta);
  }
  if (!document.querySelector('link[rel="apple-touch-icon"]')) {
    const link = document.createElement('link');
    link.rel = 'apple-touch-icon';
    link.href = '/images/icons/icon-192.png';
    document.head.appendChild(link);
  }
  if (!document.querySelector('meta[name="apple-mobile-web-app-capable"]')) {
    const meta = document.createElement('meta');
    meta.name = 'apple-mobile-web-app-capable';
    meta.content = 'yes';
    document.head.appendChild(meta);
  }


  // Register Service Worker
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/sw.js').catch((err) => {
        console.warn('PWA ServiceWorker registration failed:', err);
      });
    });
  }

  let deferredPrompt = null;
  const DISMISS_KEY = 'seochang_pwa_dismissed';
  const DISMISS_DAYS = 7;

  function isDismissed() {
    const ts = localStorage.getItem(DISMISS_KEY);
    if (!ts) return false;
    const diffDays = (Date.now() - parseInt(ts, 10)) / (1000 * 60 * 60 * 24);
    return diffDays < DISMISS_DAYS;
  }

  function markDismissed() {
    localStorage.setItem(DISMISS_KEY, String(Date.now()));
  }

  // Detect iOS Safari
  function isIosSafari() {
    const ua = window.navigator.userAgent;
    const isIos = /iPad|iPhone|iPod/.test(ua) && !window.MSStream;
    const isSafari = /WebKit/.test(ua) && !/CriOS|FxiOS|OPiOS|mercury/i.test(ua);
    const isStandalone = window.navigator.standalone === true || window.matchMedia('(display-mode: standalone)').matches;
    return isIos && isSafari && !isStandalone;
  }

  // Check if already in standalone app mode
  function isStandalone() {
    return window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;
  }

  function createInstallBanner() {
    if (isStandalone() || isDismissed()) return;

    const banner = document.createElement('div');
    banner.id = 'pwaInstallBanner';
    banner.setAttribute('role', 'banner');
    banner.setAttribute('aria-label', '성당 앱 설치 안내');
    banner.innerHTML = `
      <div style="
        position: fixed;
        bottom: 20px;
        left: 50%;
        transform: translateX(-50%);
        width: calc(100% - 32px);
        max-width: 480px;
        background: rgba(15, 23, 42, 0.96);
        backdrop-filter: blur(12px);
        -webkit-backdrop-filter: blur(12px);
        color: #ffffff;
        border-radius: 16px;
        padding: 14px 18px;
        box-shadow: 0 12px 36px rgba(0, 0, 0, 0.45);
        border: 1px solid rgba(255, 255, 255, 0.12);
        z-index: 9999;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        animation: pwaFadeInUp 0.4s ease-out;
      ">
        <style>
          @keyframes pwaFadeInUp {
            from { opacity: 0; transform: translate(-50%, 20px); }
            to { opacity: 1; transform: translate(-50%, 0); }
          }
        </style>
        <div style="display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0;">
          <img src="/images/icons/icon-192.png" alt="서창동성당" style="width: 42px; height: 42px; border-radius: 10px; object-fit: cover; flex-shrink: 0; box-shadow: 0 2px 8px rgba(0,0,0,0.3);">
          <div style="min-width: 0;">
            <div style="font-weight: 700; font-size: 0.95rem; line-height: 1.2; color: #ffffff; margin-bottom: 2px;">서창동성당 앱 설치</div>
            <div style="font-size: 0.8rem; color: #94A3B8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">홈 화면에 추가하고 편하게 사용하세요</div>
          </div>
        </div>
        <div style="display: flex; align-items: center; gap: 8px; flex-shrink: 0;">
          <button id="pwaInstallActionBtn" type="button" style="
            background: #F59E0B;
            color: #0F172A;
            border: none;
            border-radius: 999px;
            padding: 7px 14px;
            font-size: 0.85rem;
            font-weight: 700;
            cursor: pointer;
            white-space: nowrap;
          ">추가</button>
          <button id="pwaCloseBannerBtn" type="button" aria-label="닫기" style="
            background: transparent;
            border: none;
            color: #94A3B8;
            font-size: 1.25rem;
            line-height: 1;
            padding: 4px;
            cursor: pointer;
          ">&times;</button>
        </div>
      </div>
    `;

    document.body.appendChild(banner);

    const actionBtn = banner.querySelector('#pwaInstallActionBtn');
    const closeBtn = banner.querySelector('#pwaCloseBannerBtn');

    closeBtn.addEventListener('click', () => {
      markDismissed();
      banner.remove();
    });

    actionBtn.addEventListener('click', () => {
      if (deferredPrompt) {
        deferredPrompt.prompt();
        deferredPrompt.userChoice.then((choice) => {
          if (choice.outcome === 'accepted') {
            markDismissed();
            banner.remove();
          }
          deferredPrompt = null;
        });
      } else if (isIosSafari()) {
        showIosGuide();
      } else {
        alert('브라우저 메뉴(⋮)에서 [홈 화면에 추가] 또는 [앱 설치]를 선택해 주세요.');
      }
    });
  }

  function showIosGuide() {
    const existing = document.getElementById('pwaIosModal');
    if (existing) existing.remove();

    const modal = document.createElement('div');
    modal.id = 'pwaIosModal';
    modal.innerHTML = `
      <div style="
        position: fixed; inset: 0; background: rgba(0,0,0,0.6); z-index: 10000;
        display: flex; align-items: flex-end; justify-content: center; padding: 16px;
      ">
        <div style="
          background: #ffffff; color: #1E293B; border-radius: 20px; padding: 22px; width: 100%; max-width: 440px;
          text-align: center; box-shadow: 0 20px 40px rgba(0,0,0,0.3); font-family: -apple-system, BlinkMacSystemFont, sans-serif;
        ">
          <img src="/images/icons/icon-192.png" alt="서창동성당" style="width: 56px; height: 56px; border-radius: 12px; margin-bottom: 12px;">
          <h3 style="font-size: 1.15rem; font-weight: 700; margin-bottom: 8px;">홈 화면에 서창동성당 추가하기</h3>
          <p style="font-size: 0.9rem; color: #64748B; margin-bottom: 18px; line-height: 1.5;">
            아이폰 사파리 브라우저 하단의 <strong>공유 버튼</strong><br>
            <span style="font-size: 1.3rem; display: inline-block; margin: 4px 0;">⎋ (또는 네모 위 화살표)</span><br>
            을 누른 후 <strong>[홈 화면에 추가]</strong>를 선택해 주세요.
          </p>
          <button id="pwaIosCloseBtn" type="button" style="
            width: 100%; background: #0F172A; color: #ffffff; border: none; border-radius: 12px;
            padding: 12px; font-weight: 600; font-size: 0.95rem; cursor: pointer;
          ">확인</button>
        </div>
      </div>
    `;
    document.body.appendChild(modal);
    modal.querySelector('#pwaIosCloseBtn').addEventListener('click', () => modal.remove());
    modal.addEventListener('click', (e) => { if (e.target === modal.firstElementChild) modal.remove(); });
  }

  // Handle Chrome / Android prompt
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt = e;
    createInstallBanner();
  });

  // For iOS Safari or browsers where prompt event doesn't fire immediately
  window.addEventListener('DOMContentLoaded', () => {
    if (isIosSafari()) {
      setTimeout(createInstallBanner, 2000);
    }

    // Connect any manual triggers in the UI
    document.querySelectorAll('[data-pwa-install]').forEach((el) => {
      el.addEventListener('click', (e) => {
        e.preventDefault();
        if (deferredPrompt) {
          deferredPrompt.prompt();
          deferredPrompt.userChoice.then((choice) => {
            if (choice.outcome === 'accepted') markDismissed();
            deferredPrompt = null;
          });
        } else if (isIosSafari()) {
          showIosGuide();
        } else {
          alert('브라우저 메뉴(⋮)에서 [홈 화면에 추가] 또는 [앱 설치]를 선택하시면 됩니다.');
        }
      });
    });
  });
})();
