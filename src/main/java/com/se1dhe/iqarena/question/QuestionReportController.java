package com.se1dhe.iqarena.question;

import com.se1dhe.iqarena.repo.PlayerRepository;
import com.se1dhe.iqarena.repo.QuestionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/questions/report")
public class QuestionReportController {
    private final QuestionReportRepository reportRepository;
    private final QuestionRepository questionRepository;
    private final PlayerRepository playerRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ReportResponse report(Authentication authentication, @Valid @RequestBody ReportRequest request) {
        UUID playerId = (UUID) authentication.getPrincipal();
        QuestionReportEntity report = new QuestionReportEntity();
        report.setQuestion(questionRepository.findById(request.questionId()).orElseThrow());
        report.setPlayer(playerRepository.findById(playerId).orElseThrow());
        report.setReason(request.reason());
        report.setMatchId(request.matchId());
        report.setRoundIndex(request.roundIndex());
        report.setNote(request.note());
        report = reportRepository.save(report);
        return new ReportResponse(report.getId(), report.getStatus());
    }

    public record ReportRequest(
            @NotNull UUID questionId,
            UUID matchId,
            Integer roundIndex,
            @NotBlank @Pattern(regexp = "WRONG_ANSWER|SPELLING_GRAMMAR|TOO_SPECIFIC|OUTDATED|WRONG_CATEGORY|OFFENSIVE|DUPLICATE|WRONG_LANGUAGE|SPAM_LOW_QUALITY") String reason,
            @Size(max = 500) String note
    ) {}

    public record ReportResponse(UUID reportId, String status) {}
}

