package com.white.handdam.feed.service;

// [LYJ-030] 구독 등급 조회 포트 — SUBSCRIPTION 도메인 연동 전 Stub으로 동작
public interface SubscriptionLevelChecker {
    // TODO 원건님 SUBSCRIPTION(ACTIVE/CANCEL_SCHEDULED) 조회로 교체
    // 반환값: "FREE" | "PAID" | null(비구독)
    String getLevel(Long memberId, Long creatorId);
}
