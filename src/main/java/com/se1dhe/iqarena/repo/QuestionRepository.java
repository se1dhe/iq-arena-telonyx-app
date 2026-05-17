package com.se1dhe.iqarena.repo;

import com.se1dhe.iqarena.domain.Question;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

// Репозиторий вопросов.
public interface QuestionRepository extends JpaRepository<Question, UUID> {
    @Query(value = "SELECT * FROM questions WHERE status = 'approved' AND locale = :locale ORDER BY random() LIMIT :limit", nativeQuery = true)
    List<Question> pickRandomApproved(@Param("locale") String locale, @Param("limit") int limit);
}
