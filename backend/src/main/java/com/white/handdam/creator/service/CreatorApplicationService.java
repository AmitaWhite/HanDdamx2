package com.white.handdam.creator.service;

import com.white.handdam.creator.converter.CreatorApplicationConverter;
import com.white.handdam.creator.dto.request.ApplyCreatorRequest;
import com.white.handdam.creator.dto.request.RejectCreatorApplicationRequest;
import com.white.handdam.creator.dto.response.CreatorApplicationResponse;
import com.white.handdam.creator.entity.CreatorApplication;
import com.white.handdam.creator.entity.CreatorApplicationStatus;
import com.white.handdam.creator.entity.CreatorProfile;
import com.white.handdam.creator.exception.CreatorErrorCode;
import com.white.handdam.creator.repository.CreatorApplicationRepository;
import com.white.handdam.creator.repository.CreatorProfileRepository;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.Role;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.storage.ObjectStorage;
import com.white.handdam.storage.StoredObject;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 크리에이터 전환 신청 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreatorApplicationService {

    private final CreatorApplicationRepository creatorApplicationRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final MemberRepository memberRepository;
    private final ObjectStorage objectStorage;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 크리에이터 전환 신청
     */
    @Transactional
    public CreatorApplicationResponse apply(Long memberId, ApplyCreatorRequest request,
                                            MultipartFile representativeImage) {
        Member member = findMemberById(memberId);

        if (member.getRole() == Role.CREATOR) {
            throw new CustomException(CreatorErrorCode.APPLICATION_ALREADY_CREATOR);
        }
        if (creatorApplicationRepository.existsByMemberIdAndStatus(
                memberId, CreatorApplicationStatus.PENDING)) {
            throw new CustomException(CreatorErrorCode.APPLICATION_PENDING_EXISTS);
        }

        // 대표 이미지 S3 업로드
        final String finalImageUrl;
        final String finalImageKey;
        if (representativeImage != null && !representativeImage.isEmpty()) {
            StoredObject stored = objectStorage.upload("creator-profiles/" + memberId, representativeImage);
            finalImageUrl = stored.url();
            finalImageKey = stored.storageKey();
        } else {
            finalImageUrl = null;
            finalImageKey = null;
        }

        // 1) 신청 이력 생성
        CreatorApplication application = CreatorApplication.builder()
                .memberId(memberId)
                .build();
        creatorApplicationRepository.save(application);

        // 2) 크리에이터 프로필 초안 생성 또는 갱신
        creatorProfileRepository.findByMemberId(memberId)
                .ifPresentOrElse(
                        existingProfile -> existingProfile.updateFromApplication(
                                request.introduction(), finalImageUrl, finalImageKey),
                        () -> creatorProfileRepository.save(
                                CreatorProfile.builder()
                                        .memberId(memberId)
                                        .introduction(request.introduction())
                                        .representativeImageUrl(finalImageUrl)
                                        .representativeImageStorageKey(finalImageKey)
                                        .subscriptionPrice(0)
                                        .build())
                );

        return CreatorApplicationConverter.toResponse(application);
    }

    /**
     * 내 최근 신청 상태 조회
     */
    public CreatorApplicationResponse getMyLatestApplication(Long memberId) {
        CreatorApplication application = creatorApplicationRepository
                .findFirstByMemberIdOrderByAppliedAtDesc(memberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.APPLICATION_NOT_FOUND));
        return CreatorApplicationConverter.toResponse(application);
    }

    /**
     * 내 신청 이력 전체 조회
     */
    public List<CreatorApplicationResponse> getMyApplicationHistory(Long memberId) {
        return creatorApplicationRepository
                .findByMemberIdOrderByAppliedAtDesc(memberId)
                .stream()
                .map(CreatorApplicationConverter::toResponse)
                .toList();
    }

    /**
     * 관리자: 신청 목록 조회
     */
    public Page<CreatorApplicationResponse> getApplicationList(
            Long adminId, CreatorApplicationStatus status, Pageable pageable) {
        assertAdmin(adminId);
        return creatorApplicationRepository
                .findAllByStatus(status, pageable)
                .map(this::toResponseWithProfile);
    }

    /**
     * 관리자: 신청 상세 조회
     */
    public CreatorApplicationResponse getApplicationDetail(Long adminId, Long applicationId) {
        assertAdmin(adminId);
        return toResponseWithProfile(findApplicationById(applicationId));
    }

    /**
     * 관리자: 신청 승인
     */
    @Transactional
    public CreatorApplicationResponse approve(Long adminId, Long applicationId) {
        assertAdmin(adminId);

        CreatorApplication application = findApplicationById(applicationId);
        assertPending(application);

        // 1) role CREATOR로 변경
        entityManager.createQuery("UPDATE Member m SET m.role = :role WHERE m.id = :memberId")
                .setParameter("role", Role.CREATOR)
                .setParameter("memberId", application.getMemberId())
                .executeUpdate();

        // 2) 신청 상태 APPROVED
        application.approve(adminId);

        return CreatorApplicationConverter.toResponse(application);
    }

    /**
     * 관리자: 신청 거절
     */
    @Transactional
    public CreatorApplicationResponse reject(Long adminId, Long applicationId,
                                             RejectCreatorApplicationRequest request) {
        assertAdmin(adminId);

        CreatorApplication application = findApplicationById(applicationId);
        assertPending(application);

        application.reject(adminId, request.rejectReason());
        return CreatorApplicationConverter.toResponse(application);
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.MEMBER_NOT_FOUND));
    }

    private CreatorApplication findApplicationById(Long applicationId) {
        return creatorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.APPLICATION_NOT_FOUND));
    }

    private void assertAdmin(Long memberId) {
        Member member = findMemberById(memberId);
        if (member.getRole() != Role.ADMIN) {
            throw new CustomException(CreatorErrorCode.ADMIN_ONLY);
        }
    }

    private void assertPending(CreatorApplication application) {
        if (application.getStatus() != CreatorApplicationStatus.PENDING) {
            throw new CustomException(CreatorErrorCode.APPLICATION_NOT_PENDING);
        }
    }
    /** 신청 응답에 creator_profile의 소개글·이미지를 포함해 반환 */
    private CreatorApplicationResponse toResponseWithProfile(CreatorApplication application) {
        return creatorProfileRepository.findByMemberId(application.getMemberId())
                .map(profile -> CreatorApplicationConverter.toResponse(
                        application,
                        profile.getIntroduction(),
                        profile.getRepresentativeImageUrl()))
                .orElse(CreatorApplicationConverter.toResponse(application));
    }
}