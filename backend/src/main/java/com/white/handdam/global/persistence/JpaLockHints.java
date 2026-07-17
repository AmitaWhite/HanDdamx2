package com.white.handdam.global.persistence;

public final class JpaLockHints {

    public static final String LOCK_TIMEOUT_HINT = "jakarta.persistence.lock.timeout";
    public static final String LOCK_TIMEOUT_MILLIS = "3000";

    private JpaLockHints() {
    }
}
