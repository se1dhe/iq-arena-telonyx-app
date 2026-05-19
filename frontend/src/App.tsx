import { AppShell } from './components/AppShell';
import { useArena } from './hooks/useArena';

export default function App() {
  const arena = useArena();

  return (
    <AppShell
      activeTab={arena.activeTab}
      player={arena.auth?.player}
      isDemo={arena.auth?.isDemo}
      selectedCategoryId={arena.selectedCategory.id}
      phase={arena.phase}
      status={arena.status}
      round={arena.round}
      timer={arena.timer}
      selectedAnswer={arena.selectedAnswer}
      correctIndex={arena.correctIndex}
      ratingDelta={arena.ratingDelta}
      calibrationStep={arena.calibrationStep}
      isPending={arena.isPending}
      onTabChange={arena.setActiveTab}
      onCategoryChange={arena.chooseCategory}
      onFindMatch={arena.connectRealtime}
      onAnswer={arena.answer}
      onCalibration={arena.advanceCalibration}
    />
  );
}
