const tg = window.Telegram?.WebApp;
const statusEl = document.getElementById('status');
const playBtn = document.getElementById('playBtn');
const profileBtn = document.getElementById('profileBtn');
const scoreboardEl = document.getElementById('scoreboard');
const questionBox = document.getElementById('questionBox');
const roundLabel = document.getElementById('roundLabel');
const questionText = document.getElementById('questionText');
const timerEl = document.getElementById('timer');
const answersEl = document.getElementById('answers');

let accessToken = null;
let player = null;
let socket = null;
let currentMatchId = null;
let currentRound = null;
let currentDeadline = null;
let timerInterval = null;
let playersById = new Map();
let selectedAnswer = null;

function setStatus(text) {
    statusEl.textContent = text;
}

function setHidden(el, hidden) {
    el.classList.toggle('hidden', hidden);
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

    playBtn.disabled = true;
    const ticketResponse = await fetch(`/v1/realtime/session/dev/${player.playerId}`, { method: 'POST' });
    const ticketData = await ticketResponse.json();

    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    socket = new WebSocket(`${protocol}://${location.host}/ws?ticket=${ticketData.wsTicket}`);

    socket.onopen = () => {
        setStatus('Ищем соперника...');
        socket.send(JSON.stringify({ type: 'queue.join', payload: { mode: 'ranked_duel' } }));
    };

    socket.onmessage = (event) => handleWsMessage(JSON.parse(event.data));
    socket.onclose = () => {
        playBtn.disabled = false;
        setStatus('Realtime соединение закрыто.');
        stopTimer();
    };
}

function handleWsMessage(message) {
    if (message.type === 'welcome') {
        console.log('WS welcome', message.payload);
        return;
    }
    if (message.type === 'queue.status') {
        setStatus('Ты в очереди. Ждем второго игрока...');
        return;
    }
    if (message.type === 'match.found') {
        onMatchFound(message.payload);
        return;
    }
    if (message.type === 'round.open') {
        onRoundOpen(message.payload);
        return;
    }
    if (message.type === 'answer.accepted') {
        setStatus(`Ответ принят. +${message.payload.points} очков.`);
        lockAnswers();
        return;
    }
    if (message.type === 'answer.rejected') {
        setStatus(`Ответ отклонен: ${message.payload.reason}`);
        return;
    }
    if (message.type === 'round.reveal') {
        onRoundReveal(message.payload);
        return;
    }
    if (message.type === 'match.result') {
        onMatchResult(message.payload);
        return;
    }
    if (message.type === 'error') {
        setStatus(message.payload.message || 'Ошибка WebSocket.');
        return;
    }
    console.log('WS event', message);
}

function onMatchFound(payload) {
    currentMatchId = payload.matchId;
    playersById = new Map(payload.players.map(p => [p.playerId, p]));
    setStatus('Соперник найден. Матч начинается!');
    setHidden(scoreboardEl, false);
    renderScoreboard(payload.players.map(p => ({ playerId: p.playerId, score: 0 })));
}

function onRoundOpen(payload) {
    currentRound = payload.round;
    currentDeadline = new Date(payload.deadlineAt).getTime();
    selectedAnswer = null;
    setStatus(`Раунд ${payload.round}. Выбери ответ быстрее соперника.`);
    setHidden(questionBox, false);
    roundLabel.textContent = `Раунд ${payload.round} · ${payload.category}`;
    questionText.textContent = payload.prompt;
    answersEl.innerHTML = '';

    payload.options.forEach((option, index) => {
        const button = document.createElement('button');
        button.className = 'answer-btn';
        button.textContent = option;
        button.addEventListener('click', () => sendAnswer(index));
        answersEl.appendChild(button);
    });

    startTimer();
}

function sendAnswer(index) {
    if (!socket || socket.readyState !== WebSocket.OPEN || selectedAnswer !== null) {
        return;
    }
    selectedAnswer = index;
    socket.send(JSON.stringify({
        type: 'round.answer',
        payload: {
            matchId: currentMatchId,
            round: currentRound,
            selectedIndex: index
        }
    }));
    [...answersEl.children].forEach((button, i) => {
        button.disabled = true;
        if (i === index) button.classList.add('answer-selected');
    });
}

function onRoundReveal(payload) {
    stopTimer();
    setStatus(payload.reason === 'timeout' ? 'Время вышло. Показываем ответ.' : 'Раунд завершен.');

    [...answersEl.children].forEach((button, index) => {
        button.disabled = true;
        if (index === payload.correctIndex) {
            button.classList.add('answer-correct');
        } else if (index === selectedAnswer) {
            button.classList.add('answer-wrong');
        }
    });

    renderScoreboard(payload.scoreboard);
}

function onMatchResult(payload) {
    stopTimer();
    renderScoreboard(payload.scoreboard);
    playBtn.disabled = false;
    playBtn.textContent = 'Играть еще раз';

    if (!payload.winnerPlayerId) {
        setStatus('Ничья. Жесткая дуэль!');
    } else if (payload.winnerPlayerId === player.playerId) {
        setStatus('Победа! Ты забрал арену.');
    } else {
        setStatus('Поражение. Реванш?');
    }
}

function renderScoreboard(scoreboard) {
    scoreboardEl.innerHTML = '';
    scoreboard.forEach(row => {
        const p = playersById.get(row.playerId) || { displayName: row.playerId };
        const card = document.createElement('div');
        card.className = 'score-card';
        card.innerHTML = `<div class="score-name">${escapeHtml(p.displayName)}</div><div class="score-value">${row.score}</div>`;
        scoreboardEl.appendChild(card);
    });
}

function startTimer() {
    stopTimer();
    timerInterval = setInterval(() => {
        const leftMs = Math.max(0, currentDeadline - Date.now());
        timerEl.textContent = `${Math.ceil(leftMs / 1000)} сек`;
        if (leftMs <= 0) {
            lockAnswers();
            stopTimer();
        }
    }, 150);
}

function stopTimer() {
    if (timerInterval) {
        clearInterval(timerInterval);
        timerInterval = null;
    }
}

function lockAnswers() {
    [...answersEl.children].forEach(button => button.disabled = true);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
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
