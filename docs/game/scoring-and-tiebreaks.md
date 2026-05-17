# Scoring And Tiebreaks

## Round Points

```text
if answer is wrong or timed out:
    roundPoints = 0

if answer is correct:
    base = 100
    speedBonus = floor(max(0, 10000 - responseMs) / 250)
    roundPoints = base + speedBonus
```

Knowledge is the main factor. Speed only separates players who both know the answer.

## Match Winner

Winner is decided by total score. If scores are equal, the match is a draw for MVP. Future tiebreakers can use correct answer count and total response time for correct answers.

