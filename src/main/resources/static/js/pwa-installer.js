(() => {
  // 1. Ensure PWA tags exist in head for any page
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

  // 2. Service Worker Registration (Immediate)
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js').catch((err) => {
      console.warn('PWA ServiceWorker registration skipped/failed:', err);
    });
  }

  // 3. Early & Runtime Prompt Management
  window.__pwaPrompt = window.__pwaPrompt || null;
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

  function isStandalone() {
    return window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;
  }

  function isInAppBrowser() {
    const ua = navigator.userAgent || '';
    return /KAKAOTALK|NAVER|Line|Instagram|FB_IAB|FBAN|FBAV/i.test(ua);
  }

  function isIos() {
    const ua = navigator.userAgent || '';
    return /iPad|iPhone|iPod/.test(ua) && !window.MSStream;
  }

  function isAndroid() {
    return /Android/i.test(navigator.userAgent || '');
  }

  function isSecureContextEnv() {
    return window.isSecureContext || location.hostname === 'localhost' || location.hostname === '127.0.0.1';
  }

  // 4. Modal Guide (Replaces raw alert with a responsive, elegant modal)
  function showInstallGuide() {
    const existing = document.getElementById('pwaGuideModal');
    if (existing) existing.remove();

    let badgeHtml = '';
    let stepsHtml = '';
    let noteHtml = '';

    if (isStandalone()) {
      badgeHtml = '<span style="background: #10B981; color: #ffffff; padding: 4px 12px; border-radius: 999px; font-size: 0.78rem; font-weight: 700;">앱 실행 중</span>';
      stepsHtml = `
        <div style="padding: 12px 0; color: #CBD5E1; font-size: 0.95rem; line-height: 1.6;">
          현재 서창동성당 앱으로 접속 중입니다.<br>
          스마트폰 바탕화면의 성당 아이콘을 통해 언제든 편리하게 이용하실 수 있습니다.
        </div>
      `;
    } else if (isInAppBrowser()) {
      badgeHtml = '<span style="background: #FEE500; color: #181600; padding: 4px 12px; border-radius: 999px; font-size: 0.78rem; font-weight: 700;">카카오톡 / 인앱 브라우저</span>';
      stepsHtml = `
        <p style="color: #CBD5E1; font-size: 0.88rem; margin: 10px 0 16px;">
          카카오톡 등 인앱 브라우저에서는 홈 화면 추가가 제한됩니다. 아래 순서대로 <strong>기본 브라우저</strong>로 열어주세요.
        </p>
        <div style="text-align: left; background: rgba(255,255,255,0.06); padding: 14px 16px; border-radius: 12px; font-size: 0.88rem; color: #E2E8F0; line-height: 1.8;">
          <div><strong style="color: #F59E0B;">1.</strong> 우측 상단(또는 하단)의 <strong>[ ⋮ ]</strong> 또는 <strong>[···]</strong> 메뉴 터치</div>
          <div><strong style="color: #F59E0B;">2.</strong> <strong>[다른 브라우저로 열기]</strong> (Chrome 또는 Safari) 선택</div>
          <div><strong style="color: #F59E0B;">3.</strong> 열린 브라우저에서 다시 <strong>[홈 화면에 성당 앱 추가]</strong> 터치</div>
        </div>
      `;
    } else if (isIos()) {
      badgeHtml = '<span style="background: rgba(255,255,255,0.15); color: #ffffff; padding: 4px 12px; border-radius: 999px; font-size: 0.78rem; font-weight: 600;">iOS 사파리(Safari)</span>';
      stepsHtml = `
        <p style="color: #CBD5E1; font-size: 0.88rem; margin: 10px 0 16px;">
          아이폰 사파리 브라우저에서 아래 순서로 진행하시면 바탕화면에 앱이 생성됩니다.
        </p>
        <div style="text-align: left; background: rgba(255,255,255,0.06); padding: 14px 16px; border-radius: 12px; font-size: 0.88rem; color: #E2E8F0; line-height: 1.8;">
          <div><strong style="color: #F59E0B;">1.</strong> 화면 하단 중앙의 <strong>공유 버튼 ( ⎋ )</strong> 터치</div>
          <div><strong style="color: #F59E0B;">2.</strong> 메뉴 목록을 위로 올려 <strong>[홈 화면에 추가 ➕]</strong> 선택</div>
          <div><strong style="color: #F59E0B;">3.</strong> 우측 상단의 <strong>[추가]</strong> 터치</div>
        </div>
      `;
    } else if (isAndroid()) {
      badgeHtml = '<span style="background: #10B981; color: #ffffff; padding: 4px 12px; border-radius: 999px; font-size: 0.78rem; font-weight: 600;">안드로이드 (크롬 / 삼성인터넷)</span>';
      stepsHtml = `
        <p style="color: #CBD5E1; font-size: 0.88rem; margin: 10px 0 16px;">
          브라우저 메뉴를 통해 성당 앱을 홈 화면에 간편하게 추가할 수 있습니다.
        </p>
        <div style="text-align: left; background: rgba(255,255,255,0.06); padding: 14px 16px; border-radius: 12px; font-size: 0.88rem; color: #E2E8F0; line-height: 1.8;">
          <div><strong style="color: #F59E0B;">1.</strong> 브라우저 우측 상단(또는 하단)의 메뉴 <strong>[ ⋮ ]</strong> 터치</div>
          <div><strong style="color: #F59E0B;">2.</strong> <strong>[홈 화면에 추가]</strong> 또는 <strong>[앱 설치]</strong> 선택</div>
          <div><strong style="color: #F59E0B;">3.</strong> <strong>[설치/추가]</strong> 확인을 터치하면 홈 화면에 앱 생성!</div>
        </div>
      `;
      if (!isSecureContextEnv()) {
        noteHtml = `
          <div style="margin-top: 14px; padding: 10px 14px; background: rgba(245, 158, 11, 0.12); border-left: 3px solid #F59E0B; border-radius: 8px; font-size: 0.78rem; color: #FCD34D; text-align: left; line-height: 1.5;">
            💡 <strong>개발/테스트 안내:</strong> 현재 접속 주소가 보안 연결(HTTPS)이 아닌 로컬(HTTP) 환경이어서 브라우저 보안 규정상 메뉴(⋮)를 통한 수동 추가가 지원됩니다. 실서버(HTTPS) 환경에서는 원클릭 설치 팝업이 바로 호출됩니다.
          </div>
        `;
      }
    } else {
      badgeHtml = '<span style="background: rgba(255,255,255,0.15); color: #ffffff; padding: 4px 12px; border-radius: 999px; font-size: 0.78rem; font-weight: 600;">PC 브라우저</span>';
      stepsHtml = `
        <div style="text-align: left; background: rgba(255,255,255,0.06); padding: 14px 16px; border-radius: 12px; font-size: 0.88rem; color: #E2E8F0; line-height: 1.8;">
          <div><strong style="color: #F59E0B;">1.</strong> 주소창 우측 끝의 <strong>[컴퓨터로 다운로드/설치 ⊕]</strong> 아이콘 클릭</div>
          <div><strong style="color: #F59E0B;">2.</strong> 또는 브라우저 메뉴 <strong>[ ⋮ ] ➔ [서창동성당 앱 설치]</strong> 선택</div>
        </div>
      `;
    }

    const modal = document.createElement('div');
    modal.id = 'pwaGuideModal';
    modal.setAttribute('role', 'dialog');
    modal.setAttribute('aria-modal', 'true');
    modal.innerHTML = `
      <div style="
        position: fixed; inset: 0; background: rgba(15, 23, 42, 0.75); backdrop-filter: blur(8px);
        -webkit-backdrop-filter: blur(8px); z-index: 10000;
        display: flex; align-items: center; justify-content: center; padding: 20px;
        animation: pwaModalFade 0.25s ease-out;
      ">
        <style>
          @keyframes pwaModalFade { from { opacity: 0; } to { opacity: 1; } }
          @keyframes pwaCardZoom { from { transform: scale(0.94); opacity: 0; } to { transform: scale(1); opacity: 1; } }
        </style>
        <div style="
          background: #0F172A; color: #F8FAFC; border-radius: 20px; padding: 26px 22px; width: 100%; max-width: 420px;
          text-align: center; box-shadow: 0 24px 48px rgba(0,0,0,0.6); border: 1px solid rgba(255,255,255,0.14);
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
          animation: pwaCardZoom 0.25s ease-out;
        ">
          <div style="margin-bottom: 12px; display: flex; justify-content: center;">
            <img src="/images/icons/icon-192.png" alt="서창동성당" style="width: 58px; height: 58px; border-radius: 14px; box-shadow: 0 4px 14px rgba(0,0,0,0.35);">
          </div>
          <div style="margin-bottom: 10px;">${badgeHtml}</div>
          <h3 style="font-size: 1.15rem; font-weight: 700; margin: 0 0 4px; color: #FFFFFF;">홈 화면에 서창동성당 앱 추가</h3>
          ${stepsHtml}
          ${noteHtml}
          <button id="pwaGuideCloseBtn" type="button" style="
            width: 100%; margin-top: 20px; background: #F59E0B; color: #0F172A; border: none; border-radius: 12px;
            padding: 13px; font-weight: 700; font-size: 0.95rem; cursor: pointer; transition: background 0.2s;
          ">확인</button>
        </div>
      </div>
    `;

    document.body.appendChild(modal);
    const closeBtn = modal.querySelector('#pwaGuideCloseBtn');
    const dismissModal = () => modal.remove();
    closeBtn.addEventListener('click', dismissModal);
    modal.firstElementChild.addEventListener('click', (e) => {
      if (e.target === modal.firstElementChild) dismissModal();
    });
    const keyHandler = (e) => {
      if (e.key === 'Escape') {
        dismissModal();
        window.removeEventListener('keydown', keyHandler);
      }
    };
    window.addEventListener('keydown', keyHandler);
  }

  // 5. Trigger PWA Installation or Fallback Guide
  function triggerInstall() {
    if (window.__pwaPrompt) {
      window.__pwaPrompt.prompt();
      window.__pwaPrompt.userChoice.then((choice) => {
        if (choice && choice.outcome === 'accepted') {
          markDismissed();
          const banner = document.getElementById('pwaInstallBanner');
          if (banner) banner.remove();
        }
        window.__pwaPrompt = null;
      }).catch(() => {
        showInstallGuide();
      });
    } else {
      showInstallGuide();
    }
  }

  // 6. Bottom Banner UI
  function createInstallBanner() {
    if (isStandalone() || isDismissed() || document.getElementById('pwaInstallBanner')) return;

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

    actionBtn.addEventListener('click', triggerInstall);
  }

  // 7. Event Listeners
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    window.__pwaPrompt = e;
    createInstallBanner();
  });

  window.addEventListener('pwa-prompt-ready', () => {
    createInstallBanner();
  });

  // Attach button click listeners
  function initInstallButtons() {
    document.querySelectorAll('[data-pwa-install]').forEach((el) => {
      if (el.dataset.pwaBound === 'true') return;
      el.dataset.pwaBound = 'true';
      el.addEventListener('click', (e) => {
        e.preventDefault();
        triggerInstall();
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initInstallButtons();
      if (window.__pwaPrompt || isIos()) {
        setTimeout(createInstallBanner, 1500);
      }
    });
  } else {
    initInstallButtons();
    if (window.__pwaPrompt || isIos()) {
      setTimeout(createInstallBanner, 1500);
    }
  }
})();
