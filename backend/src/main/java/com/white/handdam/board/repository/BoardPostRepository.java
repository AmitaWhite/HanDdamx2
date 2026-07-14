package com.white.handdam.board.repository;

import com.white.handdam.board.entity.BoardPost;
import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 유료 게시판 게시글(board_post) 조회용 Repository.
 * 소프트 삭제된 글(is_deleted = true)은 조회 메서드에서 제외한다.
 */
public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {

	/**
	 * 게시글 단건 조회.
	 * id 일치 + 삭제되지 않은 글만. 없으면 Optional.empty().
	 */
	Optional<BoardPost> findByIdAndDeletedFalse(Long id);

	/**
	 * 크리에이터 게시판 글 목록.
	 * creatorId 기준, type/status는 null이면 필터 미적용.
	 */
	@Query("""
		SELECT p FROM BoardPost p
		WHERE p.creatorId = :creatorId
		  AND p.deleted = false
		  AND (:type IS NULL OR p.type = :type)
		  AND (:status IS NULL OR p.status = :status)
		""")
	Page<BoardPost> findByCreator(
		@Param("creatorId") Long creatorId,
		@Param("type") BoardPostType type,
		@Param("status") BoardPostStatus status,
		Pageable pageable
	);

	/**
	 * 내가 작성한 글 목록 (마이페이지).
	 * memberId 기준, type/status는 null이면 필터 미적용.
	 */
	@Query("""
		SELECT p FROM BoardPost p
		WHERE p.memberId = :memberId
		  AND p.deleted = false
		  AND (:type IS NULL OR p.type = :type)
		  AND (:status IS NULL OR p.status = :status)
		""")
	Page<BoardPost> findByMember(
		@Param("memberId") Long memberId,
		@Param("type") BoardPostType type,
		@Param("status") BoardPostStatus status,
		Pageable pageable
	);
}
