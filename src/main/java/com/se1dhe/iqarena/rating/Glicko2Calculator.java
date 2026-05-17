package com.se1dhe.iqarena.rating;

import org.springframework.stereotype.Component;

// Упрощенный Glicko-like калькулятор для MVP.
// В следующем этапе можно заменить на полный Glicko-2 без изменения внешнего API.
@Component
public class Glicko2Calculator {
    public Result update(double rating, double opponentRating, double score) {
        double expected = 1.0 / (1.0 + Math.pow(10, (opponentRating - rating) / 400.0));
        double k = 32.0;
        double newRating = rating + k * (score - expected);
        return new Result(newRating);
    }

    public record Result(double rating) {}
}
