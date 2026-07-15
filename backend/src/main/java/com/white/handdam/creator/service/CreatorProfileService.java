package com.white.handdam.creator.service;

import com.white.handdam.creator.converter.CreatorProfileConverter;
import com.white.handdam.creator.dto.response.CreatorProfileResponse;
import com.white.handdam.creator.dto.request.UpdateCreatorProfileRequest;
import com.white.handdam.creator.entity.CreatorProfile;
import com.white.handdam.creator.exception.CreatorErrorCode;
import com.white.handdam.creator.repository.CreatorProfileRepository;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.Role;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 크리에이터 프로필 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreatorProfileService {

    private final CreatorProfileRepository creatorProfileRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * 크리에이터 공개 프로필 조회
     */
    public CreatorProfileResponse getPublicProfile(Long creatorMemberId, Long requesterId) {
        // 1) 크리에이터 회원 조회 및 role 검증
        Member creator = findCreatorMemberById(creatorMemberId);

        // 2) 크리에이터 프로필 조회
        CreatorProfile profile = creatorProfileRepository.findByMemberId(creatorMemberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND));

        // 3) 요청자 구독 정보 (비로그인 또는 본인 조회 시 null)
        Subscription subscription = null;
        if (requesterId != null && !requesterId.equals(creatorMemberId)) {
            subscription = subscriptionRepository
                    .findBySubscriberIdAndCreatorId(requesterId, creatorMemberId)
                    .orElse(null);
        }

        // 4) 구독자 수 조회
        long subscriberCount = subscriptionRepository.countByCreatorId(creatorMemberId);

        // 5) 본인 여부
        boolean isMine = creatorMemberId.equals(requesterId);

        // Project 엔티티 구현 후 projectCount, feedCount 실값으로 교체
        return CreatorProfileConverter.toResponse(
                profile, creator, subscription,
                subscriberCount, 0L, 0L, isMine
        );
    }

    /**
     * 내 크리에이터 프로필 조회
     */
    public CreatorProfileResponse getMyProfile(Long memberId) {
        // 1) 크리에이터 회원 조회 및 role 검증
        Member creator = findCreatorMemberById(memberId);

        // 2) 크리에이터 프로필 조회
        CreatorProfile profile = creatorProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND));

        // 3) 구독자 수
        long subscriberCount = subscriptionRepository.countByCreatorId(memberId);

        // 4) 본인 조회 → subscription null, isMine=true 고정
        // Project 구현 후 projectCount, feedCount 교체
        return CreatorProfileConverter.toResponse(
                profile, creator, null,
                subscriberCount, 0L, 0L, true
        );
    }

    /**
     * 소개·구독 혜택 수정
     */
    @Transactional
    public CreatorProfileResponse updateProfile(Long memberId, UpdateCreatorProfileRequest request) {
        Member creator = findCreatorMemberById(memberId);
        CreatorProfile profile = creatorProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND));

        profile.updateProfile(request.introduction(), request.benefitsDescription());

        long subscriberCount = subscriptionRepository.countByCreatorId(memberId);
        // Project 구현 후 projectCount, feedCount 교체
        return CreatorProfileConverter.toResponse(
                profile, creator, null, subscriberCount, 0L, 0L, true);
    }

    /**
     * CREATOR role 회원 조회 공통 메서드.
     * 존재하지 않거나 크리에이터가 아니면 CREATOR_NOT_FOUND
     */
    private Member findCreatorMemberById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND));
        if (member.getRole() != Role.CREATOR) {
            throw new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND);
        }
        return member;
    }
}