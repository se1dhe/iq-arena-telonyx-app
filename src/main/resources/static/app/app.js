const tg = window.Telegram?.WebApp;

const views = {
    arena: document.getElementById('arenaView'),
    leaderboard: document.getElementById('leaderboardView'),
    profile: document.getElementById('profileView')
};

const tabs = {
    arena: document.getElementById('arenaTab'),
    leaderboard: document.getElementById('leaderboardTab'),
    profile: document.getElementById('profileTab')
};

const statusEl = document.getElementById('status');
const playBtn = document.getElementById('playBtn');
const ratingValue = document.getElementById('ratingValue');
const winsValue = document.getElementById('winsValue');
const winrateValue = document.getElementById('winrateValue');
const streakValue = document.getElementById('streakValue');
const rankCrest = document.getElementById('rankCrest');
const playerCardName = document.getElementById('playerCardName');
const ratingLine = document.getElementById('ratingLine');
const scoreboardEl = document.getElementById('scoreboard');
const questionBox = document.getElementById('questionBox');
const roundLabel = document.getElementById('roundLabel');
const questionText = document.getElementById('questionText');
const timerEl = document.getElementById('timer');
const answersEl = document.getElementById('answers');
const explanationEl = document.getElementById('explanation');
const categoryRail = document.getElementById('categoryRail');
const leaderboardList = document.getElementById('leaderboardList');
const profileAvatar = document.getElementById('profileAvatar');
const profileName = document.getElementById('profileName');
const profileHandle = document.getElementById('profileHandle');
const profileReferral = document.getElementById('profileReferral');
const profileCategory = document.getElementById('profileCategory');
const leaderboardPreview = document.getElementById('leaderboardPreview');
const profilePreviewAvatar = document.getElementById('profilePreviewAvatar');
const profilePreviewName = document.getElementById('profilePreviewName');
const profilePreviewTitle = document.getElementById('profilePreviewTitle');
const rankProgress = document.getElementById('rankProgress');
const rankProgressText = document.getElementById('rankProgressText');

const categories = [
    { id: 'mixed', label: 'Общие знания', asset: 'mixed' },
    { id: 'geography', label: 'География', asset: 'geography' },
    { id: 'science', label: 'Наука', asset: 'science' },
    { id: 'history', label: 'История', asset: 'history' },
    { id: 'art', label: 'Искусство', asset: 'art' },
    { id: 'logic', label: 'Логика', asset: 'logic' }
];

const categoryNames = new Map(categories.map(category => [category.id, category.label]));

let accessToken = null;
let player = null;
let socket = null;
let currentMatchId = null;
let currentRound = null;
let currentDeadline = null;
let timerInterval = null;
let playersById = new Map();
let selectedAnswer = null;
let selectedCategory = 'mixed';

function setStatus(text) {
    statusEl.textContent = text;
}

function setHidden(el, hidden) {
    el.classList.toggle('hidden', hidden);
}

function renderCategories() {
    categoryRail.innerHTML = categories.map(category => `
        <button class="category-pill ${category.id === selectedCategory ? 'selected' : ''}" type="button" data-category="${category.id}">
            <i class="cat-icon cat-${category.asset}" aria-hidden="true"></i>
            <strong>${category.label}</strong>
        </button>
    `).join('');

    categoryRail.querySelectorAll('button').forEach(button => {
        button.addEventListener('click', () => {
            selectedCategory = button.dataset.category;
            profileCategory.textContent = categoryNames.get(selectedCategory);
            renderCategories();
        });
    });
}

async function telegramLogin() {
    tg?.ready();
    tg?.expand();

    const initData = tg?.initData || '';
    if (!initData) {
        renderDemoArena();
        await loadLeaderboard();
        return;
    }

    const response = await fetch('/v1/auth/telegram/webapp', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ initData })
    });

    if (!response.ok) {
        setStatus('Telegram авторизация не прошла. Обновите WebApp.');
        return;
    }

    const data = await response.json();
    accessToken = data.accessToken;
    player = data;
    renderProfile();
    await loadLeaderboard();
    setStatus(`${data.displayName}, категория выбрана. Время забрать матч.`);
}

async function connectRealtime() {
    showView('arena');
    if (!accessToken || !player) {
        setStatus('Войдите через Telegram WebApp, затем начните матч.');
        return;
    }

    resetArena();
    playBtn.disabled = true;
    playBtn.querySelector('span').textContent = 'Синхронизация';

    const ticketResponse = await fetch('/v1/realtime/session', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${accessToken}` }
    });

    if (!ticketResponse.ok) {
        playBtn.disabled = false;
        playBtn.querySelector('span').textContent = 'Найти матч';
        setStatus('Не удалось открыть realtime-сессию.');
        return;
    }

    const ticketData = await ticketResponse.json();
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    socket = new WebSocket(`${protocol}://${location.host}/ws?ticket=${ticketData.wsTicket}`);

    socket.onopen = () => {
        setStatus(`Ищем соперника: ${categoryNames.get(selectedCategory)}.`);
        playBtn.querySelector('span').textContent = 'Поиск матча';
        socket.send(JSON.stringify({
            type: 'queue.join',
            payload: { mode: 'ranked_duel', category: selectedCategory }
        }));
    };

    socket.onmessage = (event) => handleWsMessage(JSON.parse(event.data));
    socket.onclose = () => {
        playBtn.disabled = false;
        playBtn.querySelector('span').textContent = currentMatchId ? 'Играть еще раз' : 'Найти матч';
        stopTimer();
    };
}

function handleWsMessage(message) {
    if (message.type === 'welcome') {
        return;
    }
    if (message.type === 'queue.status') {
        const category = categoryNames.get(message.payload.category || selectedCategory);
        setStatus(message.payload.status === 'idle' ? 'Поиск остановлен.' : `Очередь активна. ${category}: готовим дуэль.`);
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
        setStatus(`Ответ принят. Текущий раунд: +${message.payload.points}.`);
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
    }
}

function onMatchFound(payload) {
    currentMatchId = payload.matchId;
    playersById = new Map(payload.players.map(p => [p.playerId, p]));
    selectedCategory = payload.category || selectedCategory;
    renderCategories();
    setHidden(ratingLine, true);
    setStatus('Соперник найден. Арена открыта.');
    setHidden(statusEl, true);
    playBtn.classList.add('hidden');
    setHidden(scoreboardEl, false);
    renderScoreboard(payload.players.map(p => ({ playerId: p.playerId, score: 0 })));
}

function onRoundOpen(payload) {
    currentRound = payload.round;
    currentDeadline = new Date(payload.deadlineAt).getTime();
    selectedAnswer = null;
    setStatus('Раунд открыт. Один ответ, один шанс.');
    setHidden(statusEl, true);
    setHidden(explanationEl, true);
    explanationEl.textContent = '';
    setHidden(questionBox, false);
    roundLabel.textContent = `Раунд ${payload.round}`;
    questionText.textContent = payload.prompt;
    answersEl.innerHTML = '';

    payload.options.forEach((option, index) => {
        const button = document.createElement('button');
        button.className = 'answer-btn';
        button.type = 'button';
        button.innerHTML = `<span>${String.fromCharCode(65 + index)}</span><strong>${escapeHtml(option)}</strong>`;
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
    timerEl.textContent = '0';
    setStatus(payload.reason === 'timeout' ? 'Время вышло. Сервер раскрыл ответ.' : 'Раунд раскрыт. Следующий через мгновение.');

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
    playBtn.querySelector('span').textContent = 'Играть еще раз';
    playBtn.classList.remove('hidden');

    if (!payload.winnerPlayerId) {
        setStatus('Ничья. Все тай-брейки равны.');
    } else if (payload.winnerPlayerId === player.playerId) {
        setStatus(`Победа. ${tiebreakText(payload.tiebreak)}`);
    } else {
        setStatus(`Поражение. ${tiebreakText(payload.tiebreak)} Реванш?`);
    }
    loadLeaderboard();
}

function tiebreakText(reason) {
    if (reason === 'correct_count') return 'Решило число правильных ответов.';
    if (reason === 'correct_response_ms') return 'Решила суммарная скорость правильных ответов.';
    if (reason === 'draw') return 'Все тай-брейки равны.';
    return 'Решили очки за матч.';
}

function resetArena() {
    currentMatchId = null;
    currentRound = null;
    currentDeadline = null;
    selectedAnswer = null;
    playersById = new Map();
    timerEl.textContent = '10';
    playBtn.classList.remove('hidden');
    setHidden(statusEl, false);
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
    if (!row) return;

    const sign = row.delta > 0 ? '+' : '';
    ratingValue.textContent = row.newRating;
    ratingLine.textContent = `IQ ${row.newRating} · ${sign}${row.delta} · ${row.gamesPlayed} игр`;
    setHidden(ratingLine, false);
}

function renderScoreboard(scoreboard) {
    const rows = scoreboard.slice(0, 2).map((row, index) => {
        const p = playersById.get(row.playerId) || { displayName: row.playerId };
        return { ...row, name: p.displayName, rank: index === 0 ? 'IV' : 'III' };
    });
    const [left, right] = rows;
    if (!left || !right) return;
    scoreboardEl.innerHTML = `
        <div class="duelist left">
            <div class="duelist-avatar"><span>${initials(left.name)}</span><b>${left.rank}</b></div>
            <strong>${escapeHtml(left.name)}</strong>
            <em>${left.playerId === player?.playerId ? ratingValue.textContent : '2241'}</em>
        </div>
        <div class="score-focus">
            <b>${left.score}</b><span>:</span><b>${right.score}</b>
            <i aria-hidden="true"></i>
        </div>
        <div class="duelist right">
            <div class="duelist-avatar"><span>${initials(right.name)}</span><b>${right.rank}</b></div>
            <strong>${escapeHtml(right.name)}</strong>
            <em>${right.playerId === player?.playerId ? ratingValue.textContent : '2241'}</em>
        </div>
    `;
}

function renderDemoArena() {
    player = {
        playerId: 'demo-player',
        displayName: 'Интеллектор',
        handle: 'intellector',
        referralCode: 'IQ123',
        iqRating: 2367,
        wins: 128,
        gamesPlayed: 178,
        streak: 7
    };
    selectedCategory = 'mixed';
    renderProfile();
    playersById = new Map([
        ['demo-player', { playerId: 'demo-player', displayName: 'Интеллектор' }],
        ['demo-opponent', { playerId: 'demo-opponent', displayName: 'Эрудит' }]
    ]);
    onMatchFound({
        matchId: 'demo-match',
        category: 'mixed',
        players: [...playersById.values()]
    });
    onRoundOpen({
        round: 1,
        category: 'mixed',
        prompt: 'Какой элемент имеет наибольшую электроотрицательность по шкале Полинга?',
        options: ['Фтор', 'Кислород', 'Хлор', 'Азот'],
        deadlineAt: new Date(Date.now() + 18_000).toISOString()
    });
}

function startTimer() {
    stopTimer();
    timerInterval = setInterval(() => {
        const leftMs = Math.max(0, currentDeadline - Date.now());
        timerEl.textContent = String(Math.ceil(leftMs / 1000));
        timerEl.classList.toggle('danger', leftMs <= 3_000);
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
    timerEl.classList.remove('danger');
}

function lockAnswers() {
    [...answersEl.children].forEach(button => button.disabled = true);
}

async function loadLeaderboard() {
    const response = await fetch('/v1/leaderboards/global');
    if (!response.ok) {
        leaderboardList.innerHTML = '<div class="empty-state">Топ пока недоступен.</div>';
        leaderboardPreview.innerHTML = leaderboardList.innerHTML;
        return;
    }
    const rows = await response.json();
    if (!rows.length) {
        leaderboardList.innerHTML = '<div class="empty-state">Сыграй первый матч и займи первую строку.</div>';
        leaderboardPreview.innerHTML = leaderboardList.innerHTML;
        return;
    }
    const rowMarkup = rows.slice(0, 20).map(row => `
        <div class="leaderboard-row ${row.rank <= 3 ? 'podium' : ''}">
            <strong>${row.rank}</strong>
            <span>${escapeHtml(row.displayName)}</span>
            <b>${row.iqRating}</b>
            <small>${row.gamesPlayed}</small>
        </div>
    `).join('');
    leaderboardList.innerHTML = rowMarkup;
    leaderboardPreview.innerHTML = rows.slice(0, 5).map(row => `
        <div class="leaderboard-row ${player?.playerId === row.playerId ? 'current' : ''} ${row.rank <= 3 ? 'podium' : ''}">
            <strong>${row.rank}</strong>
            <span>${escapeHtml(row.displayName)}</span>
            <b>${row.iqRating}</b>
            <small>${row.gamesPlayed}</small>
        </div>
    `).join('');
}

function renderProfile() {
    const displayName = player?.displayName || 'Интеллектор';
    const handle = player?.handle ? `@${player.handle}` : '@telegram';
    const rating = Number(player?.iqRating || (player ? ratingValue.textContent : 2367) || 2367);
    const games = Number(player?.gamesPlayed || 178);
    const wins = Number(player?.wins || 128);
    const progress = Math.max(0, Math.min(1000, rating % 1000));
    profileName.textContent = displayName;
    playerCardName.textContent = displayName;
    profileHandle.textContent = handle;
    profileReferral.textContent = player?.referralCode || '—';
    profileCategory.textContent = categoryNames.get(selectedCategory);
    profileAvatar.textContent = initials(displayName);
    profilePreviewAvatar.textContent = initials(displayName);
    profilePreviewName.textContent = displayName;
    profilePreviewTitle.textContent = categoryNames.get(selectedCategory);
    ratingValue.textContent = rating;
    winsValue.textContent = wins;
    winrateValue.textContent = games ? `${Math.round((wins / games) * 100)}%` : '0%';
    streakValue.textContent = player?.streak || '7';
    rankCrest.textContent = rankLabel(rating);
    rankProgress.style.width = `${Math.round((progress / 1000) * 100)}%`;
    rankProgressText.textContent = `${progress} / 1000`;
}

function rankLabel(rating) {
    if (rating >= 2600) return 'V';
    if (rating >= 2300) return 'IV';
    if (rating >= 2000) return 'III';
    if (rating >= 1600) return 'II';
    return 'I';
}

function showView(view) {
    Object.entries(views).forEach(([key, el]) => el.classList.toggle('active-view', key === view));
    Object.entries(tabs).forEach(([key, el]) => el.classList.toggle('active', key === view));
    if (view === 'leaderboard') loadLeaderboard();
    if (view === 'profile') renderProfile();
}

function initials(value) {
    return String(value)
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map(part => part[0])
        .join('')
        .toUpperCase() || 'IQ';
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
tabs.arena.addEventListener('click', () => showView('arena'));
tabs.leaderboard.addEventListener('click', () => showView('leaderboard'));
tabs.profile.addEventListener('click', () => showView('profile'));

renderCategories();
renderProfile();
telegramLogin().catch(() => setStatus('Ошибка запуска WebApp.'));
