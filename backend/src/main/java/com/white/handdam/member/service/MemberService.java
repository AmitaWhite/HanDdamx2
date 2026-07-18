package com.white.handdam.member.service;

import com.white.handdam.global.exception.CommonErrorCode;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.dto.request.MemberUpdateRequest;
import com.white.handdam.member.dto.response.MemberProfileResponse;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.exception.MemberErrorCode;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.storage.ObjectStorage;
import com.white.handdam.storage.StoredObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final ObjectStorage objectStorage;

    // 단건 조회
    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    // 존재 여부 확인
    public boolean existsById(Long memberId) {
        return memberRepository.existsById(memberId);
    }

    // KSY-014
    public MemberProfileResponse getMyProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
        return MemberProfileResponse.from(member);
    }

    // KSY-016
    @Transactional
    public MemberProfileResponse updateMyProfile(Long memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (!member.getNickname().equals(request.nickname())
            && memberRepository.existsByNickname(request.nickname())) {
            throw new CustomException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        member.updateNickname(request.nickname());
        return MemberProfileResponse.from(member);
    }

    // KSY-017
    @Transactional
    public MemberProfileResponse updateProfileImage(Long memberId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        String oldStorageKey = member.getProfileImageStorageKey(); // 기존 이미지 키

        StoredObject storedObject = objectStorage.upload("member-profiles/" + memberId, image);
        member.updateProfileImage(storedObject.url(), storedObject.storageKey());

        if (oldStorageKey != null) {
            try {
                objectStorage.delete(oldStorageKey); // 기존 이미지 정리
            } catch (Exception e) {
                log.warn("기존 프로필 이미지 삭제 실패, storageKey={}", oldStorageKey, e);
            }

        }

        return MemberProfileResponse.from(member);
    }

}
