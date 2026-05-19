package com.se1dhe.iqarena.matchmaking;

import java.util.Optional;

public record LockResult<T>(boolean acquired, Optional<T> value) {
    public static <T> LockResult<T> notAcquired() {
        return new LockResult<>(false, Optional.empty());
    }

    public static <T> LockResult<T> acquired(Optional<T> value) {
        return new LockResult<>(true, value);
    }
}
