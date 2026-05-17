package com.se1dhe.iqarena.game;

import org.springframework.stereotype.Service;

// Подсчет очков в раунде.
@Service
public class ScoringService {
    public int calculate(boolean correct, int responseMs) {
        if (!correct) {
            return 0;
        }
        int base = 100;
        int speedBonus = Math.max(0, 10_000 - responseMs) / 250;
        return base + speedBonus;
    }
}
