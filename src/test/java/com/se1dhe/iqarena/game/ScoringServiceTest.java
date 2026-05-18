package com.se1dhe.iqarena.game;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringServiceTest {
    private final ScoringService scoringService = new ScoringService();

    @Test
    void correctAnswerGetsBaseAndSpeedBonus() {
        assertThat(scoringService.calculate(true, 0)).isEqualTo(140);
        assertThat(scoringService.calculate(true, 9_999)).isEqualTo(100);
    }

    @Test
    void wrongAnswerGetsNoPoints() {
        assertThat(scoringService.calculate(false, 120)).isZero();
    }

    @Test
    void duelBonusRequiresBothCorrectAndAtLeastThreeHundredMsLead() {
        assertThat(scoringService.duelBonus(true, 1_000, true, 1_300)).isEqualTo(10);
        assertThat(scoringService.duelBonus(true, 1_000, true, 1_299)).isZero();
        assertThat(scoringService.duelBonus(true, 1_000, false, 2_000)).isZero();
    }
}
