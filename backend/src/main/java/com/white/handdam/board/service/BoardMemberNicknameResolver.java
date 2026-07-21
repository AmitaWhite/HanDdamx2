package com.white.handdam.board.service;

import com.white.handdam.member.entity.Member;
import com.white.handdam.member.repository.MemberRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardMemberNicknameResolver {

	private static final String UNKNOWN = "알 수 없음";

	private final MemberRepository memberRepository;

	public String resolve(Long memberId) {
		if (memberId == null) {
			return UNKNOWN;
		}
		return memberRepository.findById(memberId)
			.map(Member::getNickname)
			.orElse(UNKNOWN);
	}

	public Map<Long, String> resolveAll(Collection<Long> memberIds) {
		if (memberIds == null || memberIds.isEmpty()) {
			return Map.of();
		}
		return memberRepository.findAllById(memberIds).stream()
			.collect(Collectors.toMap(Member::getId, Member::getNickname));
	}

	public String fromMap(Map<Long, String> nicknameMap, Long memberId) {
		return nicknameMap.getOrDefault(memberId, UNKNOWN);
	}
}
