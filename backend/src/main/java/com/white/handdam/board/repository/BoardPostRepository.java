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

public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {

	Optional<BoardPost> findByIdAndDeletedFalse(Long id);

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
}
