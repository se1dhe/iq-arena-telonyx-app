import { AnimatePresence, motion } from 'framer-motion';
import { useMemo, useState } from 'react';
import { BrainIcon } from './BrainIcon';
import {
  categories,
  demoQuestion,
  insightCards,
  leaderboardRows,
  navItems,
  playModes,
  type CategoryId,
  type TabId
} from '../data/arena';
import type { PlayerProfile } from '../lib/api';

type ArenaProps = {
  activeTab: TabId;
  player?: PlayerProfile;
  isDemo?: boolean;
  selectedCategoryId: CategoryId;
  phase: string;
  status: string;
  round: number;
  timer: number;
  selectedAnswer: number | null;
  correctIndex: number | null;
  ratingDelta: number;
  calibrationStep: number;
  isPending: boolean;
  onTabChange: (tab: TabId) => void;
  onCategoryChange: (category: CategoryId) => void;
  onFindMatch: () => void;
  onAnswer: (index: number) => void;
  onCalibration: () => void;
};

const screenMotion = {
  initial: { opacity: 0, y: 18 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -12 }
};

export function AppShell(props: ArenaProps) {
  const [challengeState, setChallengeState] = useState<'idle' | 'copied'>('idle');
  const selectedCategory = useMemo(
    () => categories.find((category) => category.id === props.selectedCategoryId) ?? categories[0],
    [props.selectedCategoryId]
  );

  const shareChallenge = async () => {
    const text = 'Я бросаю тебе challenge в BrainArena: 5 вопросов, один skill-score.';
    if (navigator.share) {
      await navigator.share({ title: 'BrainArena Challenge', text });
    } else {
      await navigator.clipboard?.writeText(text);
    }
    setChallengeState('copied');
    window.setTimeout(() => setChallengeState('idle'), 1800);
  };

  return (
    <div className="app-shell">
      <div className="surface-noise" />
      <TopBar player={props.player} isDemo={props.isDemo} />
      <AnimatePresence mode="wait">
        {props.activeTab === 'home' && (
          <HomeScreen
            key="home"
            {...props}
            selectedCategory={selectedCategory}
            challengeState={challengeState}
            onShareChallenge={shareChallenge}
          />
        )}
        {props.activeTab === 'play' && <PlayScreen key="play" {...props} selectedCategory={selectedCategory} onShareChallenge={shareChallenge} />}
        {props.activeTab === 'daily' && <DailyScreen key="daily" onFindMatch={props.onFindMatch} onCalibration={props.onCalibration} />}
        {props.activeTab === 'progress' && <ProgressScreen key="progress" selectedCategoryId={props.selectedCategoryId} onCategoryChange={props.onCategoryChange} />}
        {props.activeTab === 'profile' && <ProfileScreen key="profile" player={props.player} onShareChallenge={shareChallenge} />}
      </AnimatePresence>
      <BottomNav activeTab={props.activeTab} onTabChange={props.onTabChange} />
    </div>
  );
}

function TopBar({ player, isDemo }: { player?: PlayerProfile; isDemo?: boolean }) {
  return (
    <header className="topbar">
      <div className="wordmark">
        <div className="mark">
          <span>B</span>
        </div>
        <div>
          <strong>BrainArena</strong>
          <small>{isDemo ? 'demo profile' : player?.handle ?? 'telegram profile'}</small>
        </div>
      </div>
      <button className="ghost-icon" type="button" aria-label="Профиль">
        <BrainIcon name="profile" />
      </button>
    </header>
  );
}

function HomeScreen(props: ArenaProps & {
  selectedCategory: (typeof categories)[number];
  challengeState: 'idle' | 'copied';
  onShareChallenge: () => void;
}) {
  return (
    <motion.main className="screen-stack" variants={screenMotion} initial="initial" animate="animate" exit="exit">
      <HeroCard {...props} />
      <ModeDock onFindMatch={props.onFindMatch} onShareChallenge={props.onShareChallenge} />
      <DailyStrip onFindMatch={props.onFindMatch} onCalibration={props.onCalibration} />
      <section className="two-column">
        <MasteryCard selectedCategoryId={props.selectedCategoryId} onCategoryChange={props.onCategoryChange} />
        <SocialCard state={props.challengeState} onShareChallenge={props.onShareChallenge} />
      </section>
      <InsightGrid />
    </motion.main>
  );
}

function HeroCard(props: ArenaProps) {
  return (
    <section className="hero-card">
      <div className="hero-copy">
        <span>Ranked trivia service</span>
        <h1>Умная игра на 90 секунд, которая возвращает завтра.</h1>
        <p>Короткие квизы, асинхронные вызовы, ежедневная серия и skill-score без фальшивой психометрики.</p>
      </div>
      <div className="score-board" aria-label="Показатели игрока">
        <Metric label="Skill-score" value={2367 + props.ratingDelta} tone="blue" />
        <Metric label="Daily streak" value="7" tone="amber" />
        <Metric label="Калибровка" value={`${props.calibrationStep}/5`} tone="violet" />
      </div>
    </section>
  );
}

function Metric({ label, value, tone }: { label: string; value: string | number; tone: 'blue' | 'amber' | 'violet' }) {
  return (
    <div className={`metric ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function ModeDock({ onFindMatch, onShareChallenge }: { onFindMatch: () => void; onShareChallenge: () => void }) {
  return (
    <section className="mode-dock" aria-label="Режимы игры">
      {playModes.map((mode) => (
        <button
          key={mode.id}
          className={`mode-card ${mode.accent}`}
          type="button"
          onClick={mode.id === 'async' ? onShareChallenge : onFindMatch}
        >
          <BrainIcon name={mode.icon} />
          <span>{mode.duration}</span>
          <strong>{mode.title}</strong>
          <small>{mode.subtitle}</small>
        </button>
      ))}
    </section>
  );
}

function DailyStrip({ onFindMatch, onCalibration }: { onFindMatch: () => void; onCalibration: () => void }) {
  return (
    <section className="daily-strip">
      <div>
        <span>Сегодня</span>
        <strong>Логическая разминка</strong>
        <p>5 вопросов, объяснение после каждого ответа и бонус к серии.</p>
      </div>
      <div className="daily-actions">
        <button type="button" onClick={onFindMatch}>Старт</button>
        <button type="button" onClick={onCalibration}>Калибровать</button>
      </div>
    </section>
  );
}

function PlayScreen(props: ArenaProps & { selectedCategory: (typeof categories)[number]; onShareChallenge: () => void }) {
  const locked = props.phase === 'reveal' || props.phase === 'result';
  return (
    <motion.main className="screen-stack" variants={screenMotion} initial="initial" animate="animate" exit="exit">
      <section className="question-stage">
        <div className="stage-head">
          <div>
            <span>{props.selectedCategory.title}</span>
            <h2>Раунд {props.round}</h2>
          </div>
          <strong>{props.timer}s</strong>
        </div>
        <div className="duel-meter">
          <i />
          <div>
            <strong>240</strong>
            <span>Вы</span>
          </div>
          <div>
            <strong>210</strong>
            <span>Соперник</span>
          </div>
        </div>
        <div className="question-card">
          <p>{demoQuestion.prompt}</p>
          {demoQuestion.options.map((option, index) => {
            const isCorrect = props.correctIndex === index;
            const isSelected = props.selectedAnswer === index;
            return (
              <button
                key={option}
                className={answerClass(isSelected, isCorrect, locked)}
                type="button"
                disabled={locked}
                onClick={() => props.onAnswer(index)}
              >
                <span>{String.fromCharCode(65 + index)}</span>
                {option}
              </button>
            );
          })}
        </div>
        <div className="stage-actions">
          <button type="button" onClick={props.onFindMatch} disabled={props.isPending}>Новый матч</button>
          <button type="button" onClick={props.onShareChallenge}>Вызвать друга</button>
        </div>
        <small className="status-line">{props.status}</small>
      </section>
    </motion.main>
  );
}

function MasteryCard({ selectedCategoryId, onCategoryChange }: { selectedCategoryId: CategoryId; onCategoryChange: (category: CategoryId) => void }) {
  return (
    <section className="panel mastery-card">
      <PanelTitle icon="progress" title="Освоение тем" action="адаптивно" />
      <div className="category-list">
        {categories.map((category) => (
          <button
            key={category.id}
            type="button"
            className={category.id === selectedCategoryId ? 'category-row active' : 'category-row'}
            onClick={() => onCategoryChange(category.id)}
          >
            <BrainIcon name={category.icon} />
            <span>{category.title}</span>
            <i><b style={{ width: `${category.progress}%` }} /></i>
            <em>Lv {category.level}</em>
          </button>
        ))}
      </div>
    </section>
  );
}

function SocialCard({ state, onShareChallenge }: { state: 'idle' | 'copied'; onShareChallenge: () => void }) {
  return (
    <section className="panel social-card">
      <PanelTitle icon="share" title="Telegram-петля" action="вызов" />
      <p>После результата игрок получает не экран победы, а повод отправить вызов в чат.</p>
      <button className="share-card" type="button" onClick={onShareChallenge}>
        <BrainIcon name="share" />
        <span>{state === 'copied' ? 'Вызов готов' : 'Бросить вызов'}</span>
      </button>
      <div className="mini-leaderboard">
        {leaderboardRows.slice(0, 3).map((row) => (
          <div key={row.rank}>
            <span>{row.rank}</span>
            <strong>{row.name}</strong>
            <em>{row.rating}</em>
          </div>
        ))}
      </div>
    </section>
  );
}

function InsightGrid() {
  return (
    <section className="insight-grid">
      {insightCards.map((card) => (
        <div key={card.label}>
          <span>{card.label}</span>
          <strong>{card.value}</strong>
          <small>{card.detail}</small>
        </div>
      ))}
    </section>
  );
}

function DailyScreen({ onFindMatch, onCalibration }: { onFindMatch: () => void; onCalibration: () => void }) {
  return (
    <motion.main className="screen-stack" variants={screenMotion} initial="initial" animate="animate" exit="exit">
      <section className="daily-focus">
        <BrainIcon name="sun" />
        <span>Daily ritual</span>
        <h1>Вернуться завтра должно быть проще, чем забыть.</h1>
        <p>Квиз дня, загадка дня и серия собраны в один спокойный сценарий без рейтингового давления.</p>
        <div>
          <button type="button" onClick={onFindMatch}>Пройти квиз дня</button>
          <button type="button" onClick={onCalibration}>Настроить уровень</button>
        </div>
      </section>
    </motion.main>
  );
}

function ProgressScreen({ selectedCategoryId, onCategoryChange }: { selectedCategoryId: CategoryId; onCategoryChange: (category: CategoryId) => void }) {
  return (
    <motion.main className="screen-stack" variants={screenMotion} initial="initial" animate="animate" exit="exit">
      <MasteryCard selectedCategoryId={selectedCategoryId} onCategoryChange={onCategoryChange} />
      <InsightGrid />
    </motion.main>
  );
}

function ProfileScreen({ player, onShareChallenge }: { player?: PlayerProfile; onShareChallenge: () => void }) {
  return (
    <motion.main className="screen-stack" variants={screenMotion} initial="initial" animate="animate" exit="exit">
      <section className="profile-panel">
        <div className="profile-orb">{initials(player?.displayName ?? 'Brain Player')}</div>
        <span>@{player?.handle ?? 'brain_player'}</span>
        <h1>{player?.displayName ?? 'Интеллектор'}</h1>
        <p>Skill-score отражает игровые знания, скорость и стабильность, а не медицинский IQ.</p>
        <button type="button" onClick={onShareChallenge}>Поделиться карточкой</button>
      </section>
      <InsightGrid />
    </motion.main>
  );
}

function PanelTitle({ icon, title, action }: { icon: Parameters<typeof BrainIcon>[0]['name']; title: string; action: string }) {
  return (
    <div className="panel-title">
      <BrainIcon name={icon} />
      <strong>{title}</strong>
      <span>{action}</span>
    </div>
  );
}

function BottomNav({ activeTab, onTabChange }: { activeTab: TabId; onTabChange: (tab: TabId) => void }) {
  return (
    <nav className="bottom-nav" aria-label="Навигация">
      {navItems.map((item) => (
        <button
          key={item.id}
          type="button"
          className={activeTab === item.id ? 'active' : undefined}
          onClick={() => onTabChange(item.id)}
        >
          <BrainIcon name={item.icon} />
          <span>{item.label}</span>
        </button>
      ))}
    </nav>
  );
}

function answerClass(isSelected: boolean, isCorrect: boolean, locked: boolean) {
  if (isCorrect) return 'answer-option correct';
  if (isSelected && locked) return 'answer-option selected muted';
  if (isSelected) return 'answer-option selected';
  return 'answer-option';
}

function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || 'B';
}
