package com.white.handdam.creator.entity;

/**
 * 크리에이터 전환 신청 상태.
 */
public enum CreatorApplicationStatus {
    PENDING,   // 심사 대기
    APPROVED,  // 승인 완료
    REJECTED   // 거절
}