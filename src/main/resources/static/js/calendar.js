(function () {
    const categories = { mass: '미사·전례', meeting: '행사·모임', other: '기타·공지' };
    function categoryOf(event) {
        const color = (event.color || event.backgroundColor || '').toLowerCase();
        return color === '#3788d8' ? 'mass' : color === '#28a745' ? 'meeting' : 'other';
    }
    function filterEvents(events, category) {
        return category === 'all' ? events : events.filter(event => categoryOf(event) === category);
    }
    function formatEventRange(event) {
        if (!event.start) return '일정 시간이 지정되지 않았습니다.';
        const date = new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' });
        if (event.allDay) {
            const last = event.end ? new Date(event.end) : new Date(event.start);
            if (event.end) last.setDate(last.getDate() - 1);
            const startText = date.format(event.start);
            return (last > event.start ? startText + ' ~ ' + date.format(last) : startText) + ' · 종일';
        }
        const time = new Intl.DateTimeFormat('ko-KR', { hour: 'numeric', minute: '2-digit' });
        const start = date.format(event.start) + ' ' + time.format(event.start);
        if (!event.end) return start;
        return start + ' ~ ' + (event.start.toDateString() === event.end.toDateString() ? '' : date.format(event.end) + ' ') + time.format(event.end);
    }
    if (typeof module !== 'undefined' && module.exports) module.exports = { categoryOf, filterEvents, formatEventRange };
    if (typeof document === 'undefined') return;
    function createCalendar(element, options = {}) {
        const error = document.getElementById('calendarError');
        if (!window.FullCalendar) { error.hidden = false; return; }
        const modal = options.eventClick ? null : new bootstrap.Modal(document.getElementById('eventModal'));
        let category = 'all';
        let cache = null;
        const calendar = new FullCalendar.Calendar(element, {
            initialView: 'dayGridMonth', locale: 'ko', headerToolbar: false, height: 'auto',
            firstDay: 0, nowIndicator: true, dayMaxEvents: 2, fixedWeekCount: false,
            eventDisplay: 'block', views: { dayGridMonth: { displayEventTime: false } },
            displayEventTime: true, eventTimeFormat: { hour: '2-digit', minute: '2-digit', hour12: false },
            buttonText: { today: '오늘', month: '월간', week: '주간', list: '목록' },
            allDayText: '종일', noEventsContent: '이 기간에는 표시할 일정이 없어요.',
            moreLinkText: count => '+' + count + '개 더보기',
            datesSet: info => {
                document.getElementById('calendarTitle').textContent = info.view.title;
                document.querySelectorAll('[data-calendar-view]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.calendarView === info.view.type)));
            },
            events: function (range, success, failure) {
                const key = range.startStr + '|' + range.endStr;
                if (!options.editable && cache && cache.key === key) { error.hidden = true; success(filterEvents(cache.events, category)); return; }
                fetch('/api/calendar/events?' + new URLSearchParams({ start: range.startStr, end: range.endStr }))
                    .then(response => { if (!response.ok) throw new Error('Calendar request failed'); return response.json(); })
                    .then(events => {
                        if (!Array.isArray(events)) throw new Error('Invalid calendar response');
                        cache = { key, events }; error.hidden = true;
                        success(filterEvents(events, category));
                    }).catch(reason => { error.hidden = false; failure(reason); });
            },
            loading: busy => { document.getElementById('calendarLoading').hidden = !busy; },
            eventsSet: events => { document.getElementById('calendarCount').textContent = '표시 중인 일정 ' + events.length + '개'; },
            eventDidMount: info => { info.el.title = info.event.title; },
            eventClick: info => {
                info.jsEvent.preventDefault();
                document.getElementById('eventTitle').textContent = info.event.title;
                document.getElementById('eventCategory').textContent = categories[categoryOf(info.event)];
                document.getElementById('eventTime').textContent = formatEventRange(info.event);
                document.getElementById('eventDescription').textContent = info.event.extendedProps.description || '등록된 상세 설명이 없습니다.';
                modal.show();
            },
            ...options
        });
        document.getElementById('calendarPrevious').addEventListener('click', () => calendar.prev());
        document.getElementById('calendarNext').addEventListener('click', () => calendar.next());
        document.getElementById('calendarToday').addEventListener('click', () => calendar.today());
        document.querySelectorAll('[data-calendar-view]').forEach(button => button.addEventListener('click', () => calendar.changeView(button.dataset.calendarView)));
        document.querySelectorAll('[data-calendar-category]').forEach(button => button.addEventListener('click', () => {
            category = button.dataset.calendarCategory;
            document.querySelectorAll('[data-calendar-category]').forEach(item => item.setAttribute('aria-pressed', String(item === button)));
            calendar.refetchEvents();
        }));
        document.getElementById('calendarRetry').addEventListener('click', () => { cache = null; calendar.refetchEvents(); });
        calendar.render();
        return calendar;
    }
    window.ChurchCalendar = { create: createCalendar };
    document.addEventListener('DOMContentLoaded', function () {
        const element = document.getElementById('calendar');
        if (!element || element.closest('[data-calendar-admin]')) return;
        createCalendar(element);
        const input = document.getElementById('icalUrl');
        if (!input) return;
        input.value = window.location.origin + '/api/calendar/ical';
        document.getElementById('copyIcalUrl').addEventListener('click', async () => {
            const status = document.getElementById('calendarCopyStatus');
            try {
                if (!navigator.clipboard) throw new Error('Clipboard unavailable');
                await navigator.clipboard.writeText(input.value);
                status.textContent = '구독 주소를 복사했습니다. 캘린더 앱의 URL 구독에 붙여넣어 주세요.';
            } catch (_) {
                input.focus(); input.select(); input.setSelectionRange(0, input.value.length);
                status.textContent = '주소를 선택했습니다. 길게 누르거나 Ctrl+C로 복사해 주세요.';
            }
        });
    });
})();
