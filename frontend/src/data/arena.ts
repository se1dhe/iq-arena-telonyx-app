import type { BrainIconName } from './icons';

export type CategoryId = 'mixed' | 'logic' | 'science' | 'history' | 'geography' | 'culture';
export type TabId = 'home' | 'play' | 'daily' | 'progress' | 'profile';

export type Category = {
  id: CategoryId;
  title: string;
  progress: number;
  level: number;
  icon: BrainIconName;
};

export type PlayMode = {
  id: 'daily' | 'ranked' | 'async' | 'sprint';
  title: string;
  subtitle: string;
  duration: string;
  accent: 'blue' | 'violet' | 'amber' | 'green';
  icon: BrainIconName;
};

export type LeaderboardRow = {
  rank: number;
  name: string;
  rating: number;
  streak: number;
};

export const categories: Category[] = [
  { id: 'mixed', title: 'Общее', progress: 68, level: 7, icon: 'constellation' },
  { id: 'logic', title: 'Логика', progress: 74, level: 9, icon: 'logic' },
  { id: 'science', title: 'Наука', progress: 52, level: 5, icon: 'atom' },
  { id: 'history', title: 'История', progress: 41, level: 4, icon: 'archive' },
  { id: 'geography', title: 'Гео', progress: 59, level: 6, icon: 'globe' },
  { id: 'culture', title: 'Культура', progress: 36, level: 3, icon: 'spark' }
];

export const playModes: PlayMode[] = [
  { id: 'daily', title: 'Квиз дня', subtitle: '5 вопросов без риска рейтинга', duration: '70 сек', accent: 'blue', icon: 'sun' },
  { id: 'ranked', title: 'Рейтинговая дуэль', subtitle: 'Матчмейкинг по скорости и точности', duration: '<5 сек', accent: 'violet', icon: 'duel' },
  { id: 'async', title: 'Вызов другу', subtitle: 'Асинхронная дуэль по ссылке', duration: '24 ч', accent: 'amber', icon: 'share' },
  { id: 'sprint', title: 'Спринт', subtitle: 'Быстрый solo-режим на серию', duration: '45 сек', accent: 'green', icon: 'bolt' }
];

export const leaderboardRows: LeaderboardRow[] = [
  { rank: 1, name: 'Когнитив', rating: 2784, streak: 11 },
  { rank: 2, name: 'Архимед', rating: 2610, streak: 8 },
  { rank: 3, name: 'Логос', rating: 2486, streak: 7 },
  { rank: 4, name: 'Интеллектор', rating: 2367, streak: 7 }
];

export const demoQuestion = {
  prompt: 'Что сильнее влияет на skill-score в BrainArena?',
  options: ['Случайная тема', 'Скорость + точность', 'Время входа', 'Количество попыток'],
  correctIndex: 1
};

export const insightCards = [
  { label: 'Ритуал', value: 'Квиз дня', detail: 'ежедневный короткий вход' },
  { label: 'Социальная петля', value: 'Вызов', detail: 'результат превращается в дуэль' },
  { label: 'Mastery', value: '6 тем', detail: 'видимый прогресс категорий' }
];

export const navItems = [
  { id: 'home' as const, label: 'Главная', icon: 'arena' as const },
  { id: 'play' as const, label: 'Играть', icon: 'duel' as const },
  { id: 'daily' as const, label: 'Daily', icon: 'sun' as const },
  { id: 'progress' as const, label: 'Прогресс', icon: 'progress' as const },
  { id: 'profile' as const, label: 'Профиль', icon: 'profile' as const }
];
