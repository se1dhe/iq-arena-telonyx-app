const tg = window.Telegram?.WebApp;
const statusEl = document.getElementById('status');
const playBtn = document.getElementById('playBtn');
const arenaTab = document.getElementById('arenaTab');
const leaderboardTab = document.getElementById('leaderboardTab');
const profileTab = document.getElementById('profileTab');
const ratingLine = document.getElementById('ratingLine');
const scoreboardEl = document.getElementById('scoreboard');
const questionBox = document.getElementById('questionBox');
const roundLabel = document.getElementById('roundLabel');
const questionText = document.getElementById('questionText');
const timerEl = document.getElementById('timer');
const answersEl = document.getElementById('answers');
const explanationEl = document.getElementById('explanation');
const leaderboardPanel = document.getElementById('leaderboardPanel');
const leaderboardList = document.getElementById('leaderboardList');
const profilePanel = document.getElementById('profilePanel');
const profileName = document.getElementById('profileName');
const profileHandle = document.getElementById('profileHandle');
const profileReferral = document.getElementById('profileReferral');

let accessToken = null;
let player = null;
let socket = null;
let currentMatchId = null;
let currentRound = null;
let currentDeadline = null;
let timerInterval = null;
let playersById = new Map();
let selectedAnswer = null;
let activeView = 'arena';

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
    renderProfile();
    await loadLeaderboard();
    setStatus(`Привет, ${data.displayName}. Готов к дуэли?`);
}

async function connectRealtime() {
    showView('arena');
    if (!accessToken || !player) {
        setStatus('Сначала нужна авторизация Telegram Web App.');
        return;
    }

    resetArena();
    playBtn.disabled = true;
    const ticketResponse = await fetch('/v1/realtime/session', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${accessToken}` }
    });
    const ticketData = await ticketResponse.json();

    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    socket = new WebSocket(`${protocol}://${location.host}/ws?ticket=${ticketData.wsTicket}`);

    socket.onopen = () => {
        setStatus('Ищем соперника...');
        playBtn.textContent = 'Поиск...';
        socket.send(JSON.stringify({ type: 'queue.join', payload: { mode: 'ranked_duel' } }));
    };

    socket.onmessage = (event) => handleWsMessage(JSON.parse(event.data));
    socket.onclose = () => {
        playBtn.disabled = false;
        playBtn.textContent = currentMatchId ? 'Играть еще раз' : 'Найти соперника';
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
        setStatus(message.payload.status === 'idle' ? 'Поиск остановлен.' : 'Ты в очереди. Если соперника нет, подключим тренировочного.');
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
    if (message.type === 'rating.updated') {
        onRatingUpdated(message.payload);
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
    setHidden(ratingLine, true);
    setStatus('Соперник найден. Матч начинается!');
    setHidden(scoreboardEl, false);
    renderScoreboard(payload.players.map(p => ({ playerId: p.playerId, score: 0 })));
}

function onRoundOpen(payload) {
    currentRound = payload.round;
    currentDeadline = new Date(payload.deadlineAt).getTime();
    selectedAnswer = null;
    setStatus(`Раунд ${payload.round}. Выбери ответ быстрее соперника.`);
    setHidden(explanationEl, true);
    explanationEl.textContent = '';
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

    if (payload.explanation) {
        explanationEl.textContent = payload.explanation;
        setHidden(explanationEl, false);
    }
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
        setStatus(`Победа! ${tiebreakText(payload.tiebreak)}`);
    } else {
        setStatus(`Поражение. ${tiebreakText(payload.tiebreak)} Реванш?`);
    }
    loadLeaderboard();
}

function tiebreakText(reason) {
    if (reason === 'correct_count') {
        return 'Решило число правильных ответов.';
    }
    if (reason === 'correct_response_ms') {
        return 'Решила суммарная скорость правильных ответов.';
    }
    if (reason === 'draw') {
        return 'Все тай-брейки равны.';
    }
    return 'Решили очки за матч.';
}

function resetArena() {
    currentMatchId = null;
    currentRound = null;
    currentDeadline = null;
    selectedAnswer = null;
    playersById = new Map();
    setHidden(scoreboardEl, true);
    setHidden(questionBox, true);
    setHidden(ratingLine, true);
    scoreboardEl.innerHTML = '';
    answersEl.innerHTML = '';
    explanationEl.textContent = '';
    stopTimer();
}

function onRatingUpdated(payload) {
    const row = payload.ratings?.find(r => r.playerId === player?.playerId);
    if (!row) {
        return;
    }
    const sign = row.delta > 0 ? '+' : '';
    ratingLine.textContent = `Рейтинг ${row.newRating} (${sign}${row.delta}) · игр: ${row.gamesPlayed}`;
    setHidden(ratingLine, false);
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

async function loadLeaderboard() {
    const response = await fetch('/v1/leaderboards/global');
    if (!response.ok) {
        leaderboardList.innerHTML = '<div class="empty-state">Топ пока недоступен.</div>';
        return;
    }
    const rows = await response.json();
    if (!rows.length) {
        leaderboardList.innerHTML = '<div class="empty-state">Сыграй первый матч и займи место в топе.</div>';
        return;
    }
    leaderboardList.innerHTML = rows.slice(0, 10).map(row => `
        <div class="leaderboard-row">
            <strong>${row.rank}</strong>
            <span>${escapeHtml(row.displayName)}</span>
            <b>${row.iqRating}</b>
            <small>${row.gamesPlayed} игр</small>
        </div>
    `).join('');
}

function renderProfile() {
    if (!player) {
        profileName.textContent = '—';
        profileHandle.textContent = '—';
        profileReferral.textContent = '—';
        return;
    }
    profileName.textContent = player.displayName;
    profileHandle.textContent = `@${player.handle}`;
    profileReferral.textContent = player.referralCode || '—';
}

function showView(view) {
    activeView = view;
    arenaTab.classList.toggle('active', view === 'arena');
    leaderboardTab.classList.toggle('active', view === 'leaderboard');
    profileTab.classList.toggle('active', view === 'profile');
    setHidden(scoreboardEl, view !== 'arena' || !currentMatchId);
    setHidden(questionBox, view !== 'arena' || !currentRound);
    setHidden(leaderboardPanel, view !== 'leaderboard');
    setHidden(profilePanel, view !== 'profile');
    playBtn.classList.toggle('hidden', view !== 'arena');
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
arenaTab.addEventListener('click', () => showView('arena'));
leaderboardTab.addEventListener('click', async () => {
    showView('leaderboard');
    await loadLeaderboard();
});
profileTab.addEventListener('click', () => {
    renderProfile();
    showView('profile');
});

telegramLogin().catch(() => setStatus('Ошибка запуска Web App.'));
