import { useCallback, useEffect, useMemo, useRef, useState, useTransition } from 'react';
import { categories, demoQuestion, type CategoryId, type TabId } from '../data/arena';
import { authenticate, issueWsTicket, type AuthState } from '../lib/api';
import { haptic, prepareTelegramApp } from '../lib/telegram';

type ArenaPhase = 'idle' | 'syncing' | 'searching' | 'matched' | 'round' | 'reveal' | 'result';

type ScoreRow = {
  playerId: string;
  score: number;
};

type ArenaState = {
  activeTab: TabId;
  auth: AuthState | null;
  authError: string | null;
  selectedCategory: CategoryId;
  phase: ArenaPhase;
  status: string;
  matchId: string | null;
  round: number;
  timer: number;
  selectedAnswer: number | null;
  correctIndex: number | null;
  ratingDelta: number;
  calibrationStep: number;
  scoreboard: ScoreRow[];
};

const initialState: ArenaState = {
  activeTab: 'home',
  auth: null,
  authError: null,
  selectedCategory: 'mixed',
  phase: 'idle',
  status: 'Система готовит персональную калибровку',
  matchId: null,
  round: 3,
  timer: 10,
  selectedAnswer: null,
  correctIndex: null,
  ratingDelta: 0,
  calibrationStep: 3,
  scoreboard: [
    { playerId: 'demo-player', score: 240 },
    { playerId: 'opponent', score: 210 }
  ]
};

export function useArena() {
  const [state, setState] = useState<ArenaState>(initialState);
  const [isPending, startTransition] = useTransition();
  const socketRef = useRef<WebSocket | null>(null);
  const timerRef = useRef<number | null>(null);

  useEffect(() => {
    prepareTelegramApp();
    authenticate()
      .then((auth) => {
        setState((current) => ({
          ...current,
          auth,
          status: auth.isDemo ? 'Демо-режим. Можно посмотреть полный сценарий.' : 'Telegram профиль подключен.'
        }));
      })
      .catch(() => {
        setState((current) => ({
          ...current,
          authError: 'Telegram авторизация не прошла',
          status: 'Откройте Mini App заново из Telegram.'
        }));
      });

    return () => {
      socketRef.current?.close();
      if (timerRef.current) {
        window.clearInterval(timerRef.current);
      }
    };
  }, []);

  const selectedCategory = useMemo(
    () => categories.find((category) => category.id === state.selectedCategory) ?? categories[0],
    [state.selectedCategory]
  );

  const setActiveTab = useCallback((tab: TabId) => {
    haptic('light');
    setState((current) => ({ ...current, activeTab: tab }));
  }, []);

  const chooseCategory = useCallback((categoryId: CategoryId) => {
    haptic('light');
    setState((current) => ({ ...current, selectedCategory: categoryId }));
  }, []);

  const startRoundTimer = useCallback(() => {
    if (timerRef.current) {
      window.clearInterval(timerRef.current);
    }
    timerRef.current = window.setInterval(() => {
      setState((current) => {
        if (current.timer <= 1) {
          window.clearInterval(timerRef.current ?? undefined);
          return { ...current, timer: 0, phase: 'reveal', correctIndex: demoQuestion.correctIndex };
        }
        return { ...current, timer: current.timer - 1 };
      });
    }, 1000);
  }, []);

  const beginDemoMatch = useCallback(() => {
    haptic('medium');
    setState((current) => ({
      ...current,
      activeTab: 'play',
      phase: 'round',
      status: 'Раунд открыт. Один ответ, один шанс.',
      matchId: 'demo-match',
      round: 3,
      timer: 10,
      selectedAnswer: null,
      correctIndex: null,
      ratingDelta: 0
    }));
    startRoundTimer();
  }, [startRoundTimer]);

  const connectRealtime = useCallback(async () => {
    if (!state.auth || state.auth.isDemo || !state.auth.accessToken) {
      beginDemoMatch();
      return;
    }

    setState((current) => ({ ...current, phase: 'syncing', status: 'Открываем realtime-сессию' }));
    const ticket = await issueWsTicket(state.auth.accessToken);
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    const socket = new WebSocket(`${protocol}://${location.host}/ws?ticket=${ticket.wsTicket}`);
    socketRef.current = socket;

    socket.onopen = () => {
      socket.send(JSON.stringify({ type: 'queue.join', payload: { category: state.selectedCategory } }));
      setState((current) => ({ ...current, phase: 'searching', status: 'Балансируем соперника по скорости и точности' }));
    };

    socket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      startTransition(() => handleRealtimeMessage(message));
    };

    socket.onclose = () => {
      setState((current) => ({ ...current, phase: 'idle', status: 'Realtime-сессия завершена' }));
    };
  }, [beginDemoMatch, state.auth, state.selectedCategory]);

  const answer = useCallback((index: number) => {
    haptic(index === demoQuestion.correctIndex ? 'medium' : 'light');
    const socket = socketRef.current;
    if (socket && socket.readyState === WebSocket.OPEN && state.matchId) {
      socket.send(JSON.stringify({
        type: 'round.answer',
        payload: { matchId: state.matchId, round: state.round, selectedIndex: index }
      }));
    }

    setState((current) => ({
      ...current,
      selectedAnswer: index,
      correctIndex: demoQuestion.correctIndex,
      phase: 'reveal',
      status: index === demoQuestion.correctIndex ? 'Точный ответ. Скоростной бонус засчитан.' : 'Ответ принят. Reveal покажет результат.',
      ratingDelta: index === demoQuestion.correctIndex ? 18 : -6
    }));
  }, [state.matchId, state.round]);

  const advanceCalibration = useCallback(() => {
    haptic('medium');
    setState((current) => ({
      ...current,
      activeTab: 'daily',
      calibrationStep: Math.min(5, current.calibrationStep + 1),
      status: 'Калибровка обновлена по последнему раунду'
    }));
  }, []);

  const handleRealtimeMessage = useCallback((message: any) => {
    if (message.type === 'queue.status') {
      setState((current) => ({ ...current, phase: 'searching', status: 'Очередь активна. Ищем честный матч.' }));
    }
    if (message.type === 'match.found') {
      setState((current) => ({ ...current, phase: 'matched', matchId: message.payload.matchId, status: 'Соперник найден.' }));
    }
    if (message.type === 'round.open') {
      setState((current) => ({
        ...current,
        phase: 'round',
        round: message.payload.round,
        timer: 10,
        selectedAnswer: null,
        correctIndex: null,
        status: 'Раунд открыт.'
      }));
      startRoundTimer();
    }
    if (message.type === 'round.reveal') {
      setState((current) => ({
        ...current,
        phase: 'reveal',
        correctIndex: message.payload.correctIndex,
        scoreboard: message.payload.scoreboard ?? current.scoreboard
      }));
    }
    if (message.type === 'rating.updated') {
      setState((current) => ({ ...current, ratingDelta: 16, phase: 'result' }));
    }
  }, [startRoundTimer]);

  return {
    ...state,
    isPending,
    selectedCategory,
    setActiveTab,
    chooseCategory,
    connectRealtime,
    answer,
    advanceCalibration
  };
}
