const tg = window.Telegram?.WebApp;
const statusEl = document.getElementById('status');
const playBtn = document.getElementById('playBtn');
const profileBtn = document.getElementById('profileBtn');

let accessToken = null;
let player = null;
let socket = null;

function setStatus(text) {
    statusEl.textContent = text;
}

async function telegramLogin() {
    tg?.ready();
    tg?.expand();

    const initData = tg?.initData || '';
    if (!initData) {
        setStatus('Открыто не из Telegram. Используется dev-режим интерфейса.');
        return;
    }

    const response = await fetch('/v1/auth/telegram/webapp', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ initData })
    });

    if (!response.ok) {
        setStatus('Ошибка авторизации Telegram Web App.');
        return;
    }

    const data = await response.json();
    accessToken = data.accessToken;
    player = data;
    setStatus(`Привет, ${data.displayName}. Готов к дуэли?`);
}

async function connectRealtime() {
    if (!accessToken || !player) {
        setStatus('Сначала нужна авторизация Telegram Web App.');
        return;
    }

    const ticketResponse = await fetch(`/v1/realtime/session/dev/${player.playerId}`, { method: 'POST' });
    const ticketData = await ticketResponse.json();

    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    socket = new WebSocket(`${protocol}://${location.host}/ws?ticket=${ticketData.wsTicket}`);

    socket.onopen = () => {
        setStatus('Ищем соперника...');
        socket.send(JSON.stringify({ type: 'queue.join', payload: { mode: 'ranked_duel' } }));
    };

    socket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        if (message.type === 'queue.status') {
            setStatus('Ты в очереди. Ждем второго игрока...');
        } else if (message.type === 'mvp.note') {
            setStatus(message.payload.message);
        } else if (message.type === 'welcome') {
            console.log('WS welcome', message.payload);
        } else {
            console.log('WS event', message);
        }
    };

    socket.onclose = () => setStatus('Realtime соединение закрыто.');
}

playBtn.addEventListener('click', connectRealtime);
profileBtn.addEventListener('click', () => {
    if (!player) {
        setStatus('Профиль появится после Telegram авторизации.');
        return;
    }
    setStatus(`Профиль: @${player.handle}, referral: ${player.referralCode}`);
});

telegramLogin().catch(() => setStatus('Ошибка запуска Web App.'));
