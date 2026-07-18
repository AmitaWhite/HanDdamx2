package com.white.handdam.creator.service;

import com.white.handdam.creator.converter.CreatorProfileConverter;
import com.white.handdam.creator.dto.response.CreatorProfileResponse;
import com.white.handdam.creator.dto.request.UpdateCreatorProfileRequest;
import com.white.handdam.creator.dto.request.UpdateSubscriptionPriceRequest;
import com.white.handdam.creator.entity.CreatorProfile;
import com.white.handdam.creator.exception.CreatorErrorCode;
import com.white.handdam.creator.repository.CreatorProfileRepository;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.CommonErrorCode;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.Role;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.project.repository.ProjectRepository;
import com.white.handdam.storage.ObjectStorage;
import com.white.handdam.storage.StoredObject;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final ObjectStorage objectStorage;
    private final ProjectRepository projectRepository;

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

        // 5) 프로젝트 수, 피드 수 조회
        long projectCount = projectRepository.countByCreatorIdAndDeletedFalse(creatorMemberId);
        long feedCount = 0L;
        // 6) 본인 여부
        boolean isMine = creatorMemberId.equals(requesterId);

        return CreatorProfileConverter.toResponse(
                profile, creator, subscription,
                subscriberCount, projectCount, feedCount, isMine
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

        // 3) 구독자 수, 프로젝트 수, 피드 수 조회
        long subscriberCount = subscriptionRepository.countByCreatorId(memberId);
        long projectCount = projectRepository.countByCreatorIdAndDeletedFalse(memberId);
        long feedCount = 0L;
        return CreatorProfileConverter.toResponse(
                profile, creator, null,
                subscriberCount, projectCount, feedCount, true
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
        long projectCount = projectRepository.countByCreatorIdAndDeletedFalse(memberId);
        long feedCount = 0L;
        return CreatorProfileConverter.toResponse(
                profile, creator, null, subscriberCount, projectCount, feedCount, true);
    }

    /**
     * 대표 이미지 변경
     */
    @Transactional
    public CreatorProfileResponse updateRepresentativeImage(Long memberId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }

        Member creator = findCreatorMemberById(memberId);
        CreatorProfile profile = creatorProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND));

        StoredObject stored = objectStorage.upload("creator-profiles/" + memberId, image);
        profile.updateRepresentativeImage(stored.url(), stored.storageKey());

        long subscriberCount = subscriptionRepository.countByCreatorId(memberId);
        long projectCount = projectRepository.countByCreatorIdAndDeletedFalse(memberId);
        long feedCount = 0L;
        return CreatorProfileConverter.toResponse(
                profile, creator, null, subscriberCount, projectCount, feedCount, true);
    }

    /**
     * 대표 이미지 제거
     */
    @Transactional
    public CreatorProfileResponse clearRepresentativeImage(Long memberId) {
        Member creator = findCreatorMemberById(memberId);
        CreatorProfile profile = creatorProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND));

        profile.clearRepresentativeImage();

        long subscriberCount = subscriptionRepository.countByCreatorId(memberId);
        long projectCount = projectRepository.countByCreatorIdAndDeletedFalse(memberId);
        long feedCount = 0L;
        return CreatorProfileConverter.toResponse(
                profile, creator, null, subscriberCount, projectCount, feedCount, true);
    }

    /**
     * 커버 이미지 변경
     */
    @Transactional
    public CreatorProfileResponse updateCoverImage(Long memberId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }

        Member creator = findCreatorMemberById(memberId);
        CreatorProfile profile = creatorProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND));

        StoredObject stored = objectStorage.upload("creator-profiles/" + memberId, image);
        profile.updateCoverImage(stored.url(), stored.storageKey());

        long subscriberCount = subscriptionRepository.countByCreatorId(memberId);
        long projectCount = projectRepository.countByCreatorIdAndDeletedFalse(memberId);
        long feedCount = 0L;
        return CreatorProfileConverter.toResponse(
                profile, creator, null, subscriberCount, projectCount, feedCount, true);
    }

    /**
     * 커버 이미지 제거
     */
    @Transactional
    public CreatorProfileResponse clearCoverImage(Long memberId) {
        Member creator = findCreatorMemberById(memberId);
        CreatorProfile profile = creatorProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND));

        profile.clearCoverImage();

        long subscriberCount = subscriptionRepository.countByCreatorId(memberId);
        long projectCount = projectRepository.countByCreatorIdAndDeletedFalse(memberId);
        long feedCount = 0L;
        return CreatorProfileConverter.toResponse(
                profile, creator, null, subscriberCount, projectCount, feedCount, true);
    }

    /**
     * 월 구독 가격 변경
     */
    @Transactional
    public CreatorProfileResponse updateSubscriptionPrice(Long memberId, UpdateSubscriptionPriceRequest request) {
        if (request.subscriptionPrice() < 0) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }

        Member creator = findCreatorMemberById(memberId);
        CreatorProfile profile = creatorProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND));

        profile.updateSubscriptionPrice(request.subscriptionPrice());

        long subscriberCount = subscriptionRepository.countByCreatorId(memberId);
        long projectCount = projectRepository.countByCreatorIdAndDeletedFalse(memberId);
        long feedCount = 0L;
        return CreatorProfileConverter.toResponse(
                profile, creator, null, subscriberCount, projectCount, feedCount, true);
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