package com.se1dhe.iqarena.game;

import com.se1dhe.iqarena.domain.Player;
import com.se1dhe.iqarena.domain.Question;
import com.se1dhe.iqarena.repo.QuestionRepository;
import com.se1dhe.iqarena.realtime.WsSender;
import com.se1dhe.iqarena.repo.PlayerRepository;
import com.se1dhe.iqarena.rating.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

// Server-authoritative game engine для MVP 1v1.
@Service
@RequiredArgsConstructor
public class GameService {
    private static final String BOT_HANDLE = "arena_bot";
    private static final String MIXED_CATEGORY = "mixed";

    private final MatchRepository matchRepository;
    private final MatchRoundRepository roundRepository;
    private final MatchAnswerRepository answerRepository;
    private final PlayerRepository playerRepository;
    private final QuestionRepository questionRepository;
    private final ScoringService scoringService;
    private final RatingService ratingService;
    private final WsSender wsSender;
    private final TaskScheduler taskScheduler;

    // Защита от двойного reveal, когда оба игрока ответили почти одновременно или сработал timer.
    private final Set<String> revealedRounds = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> matchCategories = new ConcurrentHashMap<>();

    @Value("${app.game.rounds:5}")
    private int rounds;

    @Value("${app.game.round-seconds:10}")
    private int roundSeconds;

    @Value("${app.game.reveal-pause-seconds:2}")
    private int revealPauseSeconds;

    @Transactional
    public UUID createMatch(UUID playerOneId, UUID playerTwoId) {
        return createMatch(playerOneId, playerTwoId, MIXED_CATEGORY);
    }

    @Transactional
    public UUID createMatch(UUID playerOneId, UUID playerTwoId, String category) {
        Player one = playerRepository.findById(playerOneId).orElseThrow();
        Player two = playerRepository.findById(playerTwoId).orElseThrow();

        MatchEntity match = new MatchEntity();
        match.setState(MatchState.match_found);
        match.setPlayerOne(one);
        match.setPlayerTwo(two);
        match.setStartedAt(Instant.now());
        match = matchRepository.save(match);
        String normalizedCategory = normalizeCategory(category);
        matchCategories.put(match.getId(), normalizedCategory);

        Map<String, Object> payload = Map.of(
                "matchId", match.getId().toString(),
                "players", List.of(playerPayload(one), playerPayload(two)),
                "rounds", rounds,
                "roundSeconds", roundSeconds,
                "category", normalizedCategory
        );

        wsSender.sendToPlayer(playerOneId, "match.found", payload);
        wsSender.sendToPlayer(playerTwoId, "match.found", payload);

        openRound(match.getId(), 1);
        return match.getId();
    }

    @Transactional
    public void answer(UUID playerId, UUID matchId, int roundIndex, int selectedIndex) {
        MatchEntity match = matchRepository.findById(matchId).orElseThrow();
        MatchRoundEntity round = roundRepository.findByMatchIdAndRoundIndex(matchId, roundIndex).orElseThrow();

        if (!isParticipant(match, playerId)) {
            throw new IllegalArgumentException("Игрок не является участником матча");
        }
        if (revealedRounds.contains(roundKey(matchId, roundIndex))) {
            wsSender.sendToPlayer(playerId, "answer.rejected", Map.of("reason", "round_already_revealed"));
            return;
        }
        if (answerRepository.existsByMatchIdAndRoundIndexAndPlayerId(matchId, roundIndex, playerId)) {
            wsSender.sendToPlayer(playerId, "answer.rejected", Map.of("reason", "already_answered"));
            return;
        }

        Instant now = Instant.now();
        boolean late = now.isAfter(round.getDeadlineAt());
        boolean correct = !late && selectedIndex == round.getCorrectIndex();
        int responseMs = Math.max(0, (int) (now.toEpochMilli() - round.getOpenedAt().toEpochMilli()));
        int points = scoringService.calculate(correct, responseMs);

        MatchAnswerEntity answer = new MatchAnswerEntity();
        answer.setMatch(match);
        answer.setRoundIndex(roundIndex);
        answer.setPlayer(playerRepository.findById(playerId).orElseThrow());
        answer.setSelectedIndex(selectedIndex);
        answer.setCorrect(correct);
        answer.setResponseMs(responseMs);
        answer.setPoints(points);
        answerRepository.save(answer);

        wsSender.sendToPlayer(playerId, "answer.accepted", Map.of(
                "matchId", matchId.toString(),
                "round", roundIndex,
                "points", points
        ));

        if (answerRepository.findByMatchIdAndRoundIndex(matchId, roundIndex).size() >= 2) {
            revealRound(matchId, roundIndex, "both_answered");
        }
    }

    @Transactional
    public void openRound(UUID matchId, int roundIndex) {
        MatchEntity match = matchRepository.findById(matchId).orElseThrow();
        if (match.getState() == MatchState.match_complete) {
            return;
        }

        String matchCategory = matchCategories.getOrDefault(matchId, MIXED_CATEGORY);
        List<Question> questions = pickQuestion(matchCategory);
        if (questions.isEmpty()) {
            throw new IllegalStateException("Нет вопросов для матча");
        }

        Question question = questions.get(0);
        Instant openedAt = Instant.now();
        Instant deadlineAt = openedAt.plusSeconds(roundSeconds);

        MatchRoundEntity round = new MatchRoundEntity();
        round.setMatch(match);
        round.setRoundIndex(roundIndex);
        round.setQuestion(question);
        round.setCorrectIndex(question.getCorrectIndex());
        round.setOpenedAt(openedAt);
        round.setDeadlineAt(deadlineAt);
        roundRepository.save(round);

        match.setState(MatchState.round_open);
        matchRepository.save(match);

        Map<String, Object> payload = Map.of(
                "matchId", matchId.toString(),
                "round", roundIndex,
                "category", question.getCategory(),
                "questionId", question.getId().toString(),
                "prompt", question.getPrompt(),
                "options", question.getOptions(),
                "openedAt", openedAt.toString(),
                "deadlineAt", deadlineAt.toString()
        );
        sendBoth(match, "round.open", payload);
        scheduleBotAnswerIfNeeded(match, roundIndex, question.getCorrectIndex(), question.getOptions().size());

        taskScheduler.schedule(() -> revealRoundSafe(matchId, roundIndex), deadlineAt.plusMillis(250));
    }

    @Transactional
    public void revealRound(UUID matchId, int roundIndex) {
        revealRound(matchId, roundIndex, "manual");
    }

    @Transactional
    public void revealRound(UUID matchId, int roundIndex, String reason) {
        String key = roundKey(matchId, roundIndex);
        if (!revealedRounds.add(key)) {
            return;
        }

        MatchEntity match = matchRepository.findById(matchId).orElseThrow();
        if (match.getState() == MatchState.match_complete) {
            return;
        }

        MatchRoundEntity round = roundRepository.findByMatchIdAndRoundIndex(matchId, roundIndex).orElseThrow();
        List<MatchAnswerEntity> answers = answerRepository.findByMatchIdAndRoundIndex(matchId, roundIndex);
        applyDuelBonus(answers);

        int onePoints = answers.stream()
                .filter(a -> a.getPlayer().getId().equals(match.getPlayerOne().getId()))
                .mapToInt(MatchAnswerEntity::getPoints)
                .sum();
        int twoPoints = answers.stream()
                .filter(a -> a.getPlayer().getId().equals(match.getPlayerTwo().getId()))
                .mapToInt(MatchAnswerEntity::getPoints)
                .sum();

        match.setPlayerOneScore(match.getPlayerOneScore() + onePoints);
        match.setPlayerTwoScore(match.getPlayerTwoScore() + twoPoints);
        match.setState(MatchState.round_reveal);
        matchRepository.save(match);

        sendBoth(match, "round.reveal", Map.of(
                "matchId", matchId.toString(),
                "round", roundIndex,
                "reason", reason,
                "correctIndex", round.getCorrectIndex(),
                "explanation", round.getQuestion().getExplanation(),
                "answers", answers.stream().map(this::answerPayload).toList(),
                "scoreboard", scoreboard(match)
        ));

        if (roundIndex >= rounds) {
            taskScheduler.schedule(() -> completeMatch(matchId), Instant.now().plusSeconds(revealPauseSeconds));
        } else {
            taskScheduler.schedule(() -> openRound(matchId, roundIndex + 1), Instant.now().plusSeconds(revealPauseSeconds));
        }
    }

    @Transactional
    public void completeMatch(UUID matchId) {
        MatchEntity match = matchRepository.findById(matchId).orElseThrow();
        if (match.getState() == MatchState.match_complete) {
            return;
        }
        MatchTiebreak tiebreak = resolveWinner(match, answerRepository.findByMatchId(matchId));
        UUID winnerId = tiebreak.winnerId();
        if (winnerId != null && winnerId.equals(match.getPlayerOne().getId())) {
            match.setWinnerPlayer(match.getPlayerOne());
        } else if (winnerId != null && winnerId.equals(match.getPlayerTwo().getId())) {
            match.setWinnerPlayer(match.getPlayerTwo());
        }
        match.setState(MatchState.match_complete);
        match.setCompletedAt(Instant.now());
        matchRepository.save(match);
        matchCategories.remove(matchId);

        List<Map<String, Object>> ratingUpdates = ratingService.updateAfterMatch(match);

        sendBoth(match, "match.result", Map.of(
                "matchId", matchId.toString(),
                "winnerPlayerId", winnerId == null ? "" : winnerId.toString(),
                "tiebreak", tiebreak.reason(),
                "scoreboard", scoreboard(match)
        ));
        sendBoth(match, "rating.updated", Map.of(
                "matchId", matchId.toString(),
                "ratings", ratingUpdates
        ));
    }

    private void revealRoundSafe(UUID matchId, int roundIndex) {
        try {
            revealRound(matchId, roundIndex, "timeout");
        } catch (Exception ignored) {
            // MVP: не валим scheduler из-за отдельного матча.
        }
    }

    private List<Question> pickQuestion(String category) {
        if (category != null && !MIXED_CATEGORY.equals(category)) {
            List<Question> categoryQuestions = questionRepository.pickRandomApprovedByCategory("ru-RU", category, 1);
            if (!categoryQuestions.isEmpty()) {
                return categoryQuestions;
            }
        }
        return questionRepository.pickRandomApproved("ru-RU", 1);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return MIXED_CATEGORY;
        }
        return switch (category) {
            case "history", "geography", "science", "it", "sports" -> category;
            default -> MIXED_CATEGORY;
        };
    }

    private void applyDuelBonus(List<MatchAnswerEntity> answers) {
        if (answers.size() < 2) {
            return;
        }

        MatchAnswerEntity first = answers.get(0);
        MatchAnswerEntity second = answers.get(1);
        int firstBonus = scoringService.duelBonus(
                first.isCorrect(),
                responseMs(first),
                second.isCorrect(),
                responseMs(second)
        );
        int secondBonus = scoringService.duelBonus(
                second.isCorrect(),
                responseMs(second),
                first.isCorrect(),
                responseMs(first)
        );

        if (firstBonus > 0) {
            first.setPoints(first.getPoints() + firstBonus);
            answerRepository.save(first);
        }
        if (secondBonus > 0) {
            second.setPoints(second.getPoints() + secondBonus);
            answerRepository.save(second);
        }
    }

    private MatchTiebreak resolveWinner(MatchEntity match, List<MatchAnswerEntity> answers) {
        UUID oneId = match.getPlayerOne().getId();
        UUID twoId = match.getPlayerTwo().getId();

        if (match.getPlayerOneScore() != match.getPlayerTwoScore()) {
            return match.getPlayerOneScore() > match.getPlayerTwoScore()
                    ? new MatchTiebreak(oneId, "score")
                    : new MatchTiebreak(twoId, "score");
        }

        int oneCorrect = correctCount(answers, oneId);
        int twoCorrect = correctCount(answers, twoId);
        if (oneCorrect != twoCorrect) {
            return oneCorrect > twoCorrect
                    ? new MatchTiebreak(oneId, "correct_count")
                    : new MatchTiebreak(twoId, "correct_count");
        }

        int oneResponseMs = totalCorrectResponseMs(answers, oneId);
        int twoResponseMs = totalCorrectResponseMs(answers, twoId);
        if (oneResponseMs != twoResponseMs) {
            return oneResponseMs < twoResponseMs
                    ? new MatchTiebreak(oneId, "correct_response_ms")
                    : new MatchTiebreak(twoId, "correct_response_ms");
        }

        return new MatchTiebreak(null, "draw");
    }

    private int correctCount(List<MatchAnswerEntity> answers, UUID playerId) {
        return (int) answers.stream()
                .filter(answer -> answer.getPlayer().getId().equals(playerId))
                .filter(MatchAnswerEntity::isCorrect)
                .count();
    }

    private int totalCorrectResponseMs(List<MatchAnswerEntity> answers, UUID playerId) {
        return answers.stream()
                .filter(answer -> answer.getPlayer().getId().equals(playerId))
                .filter(MatchAnswerEntity::isCorrect)
                .mapToInt(this::responseMs)
                .sum();
    }

    private int responseMs(MatchAnswerEntity answer) {
        return answer.getResponseMs() == null ? Integer.MAX_VALUE : answer.getResponseMs();
    }

    private void scheduleBotAnswerIfNeeded(MatchEntity match, int roundIndex, int correctIndex, int optionsCount) {
        Optional<UUID> botPlayerId = botParticipantId(match);
        if (botPlayerId.isEmpty()) {
            return;
        }

        int answerIndex = botAnswerIndex(correctIndex, optionsCount);
        long upperDelayMs = Math.max(700, Math.min(6_500, roundSeconds * 1_000L - 1_000));
        long lowerDelayMs = Math.min(1_200, upperDelayMs);
        long delayMs = ThreadLocalRandom.current().nextLong(lowerDelayMs, upperDelayMs + 1);
        UUID matchId = match.getId();
        UUID playerId = botPlayerId.get();

        taskScheduler.schedule(() -> answerBotSafe(playerId, matchId, roundIndex, answerIndex), Instant.now().plusMillis(delayMs));
    }

    private Optional<UUID> botParticipantId(MatchEntity match) {
        if (BOT_HANDLE.equals(match.getPlayerOne().getHandle())) {
            return Optional.of(match.getPlayerOne().getId());
        }
        if (BOT_HANDLE.equals(match.getPlayerTwo().getHandle())) {
            return Optional.of(match.getPlayerTwo().getId());
        }
        return Optional.empty();
    }

    private int botAnswerIndex(int correctIndex, int optionsCount) {
        if (optionsCount <= 1 || ThreadLocalRandom.current().nextDouble() < 0.62) {
            return correctIndex;
        }

        int selected;
        do {
            selected = ThreadLocalRandom.current().nextInt(optionsCount);
        } while (selected == correctIndex);
        return selected;
    }

    private void answerBotSafe(UUID playerId, UUID matchId, int roundIndex, int selectedIndex) {
        try {
            answer(playerId, matchId, roundIndex, selectedIndex);
        } catch (Exception ignored) {
            // Bot opponent is best-effort; timeout reveal will still finish the round.
        }
    }

    private String roundKey(UUID matchId, int roundIndex) {
        return matchId + ":" + roundIndex;
    }

    private void sendBoth(MatchEntity match, String type, Object payload) {
        wsSender.sendToPlayer(match.getPlayerOne().getId(), type, payload);
        wsSender.sendToPlayer(match.getPlayerTwo().getId(), type, payload);
    }

    private boolean isParticipant(MatchEntity match, UUID playerId) {
        return match.getPlayerOne().getId().equals(playerId) || match.getPlayerTwo().getId().equals(playerId);
    }

    private Map<String, Object> playerPayload(Player player) {
        return Map.of("playerId", player.getId().toString(), "displayName", player.getDisplayName(), "avatarId", player.getAvatarId());
    }

    private Map<String, Object> answerPayload(MatchAnswerEntity answer) {
        return Map.of(
                "playerId", answer.getPlayer().getId().toString(),
                "selectedIndex", answer.getSelectedIndex(),
                "correct", answer.isCorrect(),
                "responseMs", answer.getResponseMs(),
                "points", answer.getPoints()
        );
    }

    private List<Map<String, Object>> scoreboard(MatchEntity match) {
        return List.of(
                Map.of("playerId", match.getPlayerOne().getId().toString(), "score", match.getPlayerOneScore()),
                Map.of("playerId", match.getPlayerTwo().getId().toString(), "score", match.getPlayerTwoScore())
        );
    }

    private record MatchTiebreak(UUID winnerId, String reason) {}
}
