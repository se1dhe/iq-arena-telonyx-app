package com.se1dhe.iqarena.rating;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RatingSmokeTest {
    @Test
    void ratingIncreasesForWinner() {
        Glicko2Calculator calculator = new Glicko2Calculator();
        assertThat(calculator.update(1500, 1500, 1.0).rating()).isGreaterThan(1500);
    }
}
