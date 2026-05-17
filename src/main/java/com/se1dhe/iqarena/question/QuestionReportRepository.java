package com.se1dhe.iqarena.question;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuestionReportRepository extends JpaRepository<QuestionReportEntity, UUID> {
}

