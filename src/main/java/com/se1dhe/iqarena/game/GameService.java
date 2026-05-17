package com.se1dhe.iqarena.game;

import com.se1dhe.iqarena.domain.Player;
import com.se1dhe.iqarena.domain.Question;
import com.se1dhe.iqarena.question.QuestionRepository;
import com.se1dhe.iqarena.realtime.WsSender;
import com.se1dhe.iqarena.repo.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

// Server-authoritative game engine для MVP 1v1.
@Service
@RequiredArgsConstructor
public class GameService {
    private final MatchRepository matchRepository;
    private final MatchRoundRepository roundRepository;
    private final MatchAnswerRepository answerRepository;
    private final PlayerRepository playerRepository;
    private final QuestionRepository questionRepository;
    private final ScoringService scoringService;
    private final WsSender wsSender;

    @Value("${app.game.rounds:5}")
    private int rounds;

    @Value("${app.game.round-seconds:10}")
    private int roundSeconds;

    @Transactional
    public UUID createMatch(UUID playerOneId, UUID playerTwoId) {
        Player one = playerRepository.findById(playerOneId).orElseThrow();
        Player two = playerRepository.findById(playerTwoId).orElseThrow();

        MatchEntity match = new MatchEntity();
        match.setState(MatchState.match_found);
        match.setPlayerOne(one);
        match.setPlayerTwo(two);
        match.setStartedAt(Instant.now());
        match = matchRepository.save(match);

        Map<String, Object> payload = Map.of(
                "matchId", match.getId().toString(),
                "players", List.of(playerPayload(one), playerPayload(two)),
                "rounds", rounds,
                "roundSeconds", roundSeconds
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
            revealRound(matchId, roundIndex);
        }
    }

    @Transactional
    public void openRound(UUID matchId, int roundIndex) {
        MatchEntity match = matchRepository.findById(matchId).orElseThrow();
        List<Question> questions = questionRepository.pickRandomApproved("ru-RU", 1);
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
    }

    @Transactional
    public void revealRound(UUID matchId, int roundIndex) {
        MatchEntity match = matchRepository.findById(matchId).orElseThrow();
        MatchRoundEntity round = roundRepository.findByMatchIdAndRoundIndex(matchId, roundIndex).orElseThrow();
        List<MatchAnswerEntity> answers = answerRepository.findByMatchIdAndRoundIndex(matchId, roundIndex);

        int onePoints = answers.stream().filter(a -> a.getPlayer().getId().equals(match.getPlayerOne().getId())).mapToInt(MatchAnswerEntity::getPoints).sum();
        int twoPoints = answers.stream().filter(a -> a.getPlayer().getId().equals(match.getPlayerTwo().getId())).mapToInt(MatchAnswerEntity::getPoints).sum();

        match.setPlayerOneScore(match.getPlayerOneScore() + onePoints);
        match.setPlayerTwoScore(match.getPlayerTwoScore() + twoPoints);
        matchRepository.save(match);

        sendBoth(match, "round.reveal", Map.of(
                "matchId", matchId.toString(),
                "round", roundIndex,
                "correctIndex", round.getCorrectIndex(),
                "explanation", round.getQuestion().getExplanation(),
                "scoreboard", scoreboard(match)
        ));

        if (roundIndex >= rounds) {
            completeMatch(matchId);
        } else {
            openRound(matchId, roundIndex + 1);
        }
    }

    @Transactional
    public void completeMatch(UUID matchId) {
        MatchEntity match = matchRepository.findById(matchId).orElseThrow();
        UUID winnerId = null;
        if (match.getPlayerOneScore() > match.getPlayerTwoScore()) {
            match.setWinnerPlayer(match.getPlayerOne());
            winnerId = match.getPlayerOne().getId();
        } else if (match.getPlayerTwoScore() > match.getPlayerOneScore()) {
            match.setWinnerPlayer(match.getPlayerTwo());
            winnerId = match.getPlayerTwo().getId();
        }
        match.setState(MatchState.match_complete);
        match.setCompletedAt(Instant.now());
        matchRepository.save(match);

        sendBoth(match, "match.result", Map.of(
                "matchId", matchId.toString(),
                "winnerPlayerId", winnerId == null ? "" : winnerId.toString(),
                "scoreboard", scoreboard(match)
        ));
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

    private List<Map<String, Object>> scoreboard(MatchEntity match) {
        return List.of(
                Map.of("playerId", match.getPlayerOne().getId().toString(), "score", match.getPlayerOneScore()),
                Map.of("playerId", match.getPlayerTwo().getId().toString(), "score", match.getPlayerTwoScore())
        );
    }
}
