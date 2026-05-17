package com.se1dhe.iqarena.repo;

import com.se1dhe.iqarena.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

// Репозиторий игроков.
public interface PlayerRepository extends JpaRepository<Player, UUID> {
    Optional<Player> findByHandle(String handle);
    boolean existsByHandle(String handle);
}
