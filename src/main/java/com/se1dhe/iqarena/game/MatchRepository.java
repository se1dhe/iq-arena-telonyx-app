package com.se1dhe.iqarena.game;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchRepository extends JpaRepository<MatchEntity, UUID> {
}
