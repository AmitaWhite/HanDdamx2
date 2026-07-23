package com.white.handdam.board.repository;

import com.white.handdam.board.entity.BoardAnswer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardAnswerRepository extends JpaRepository<BoardAnswer, Long> {

	/** 활성(미삭제) 공식 답변 존재 여부 — 중복 등록 차단용 */
	boolean existsByBoardPostIdAndDeletedFalse(Long boardPostId);

	/** 소프트 삭제 포함, 게시글당 최대 1행 (UNIQUE board_post_id) */
	Optional<BoardAnswer> findByBoardPostId(Long boardPostId);

	/** 활성(미삭제) 공식 답변 조회 */
	Optional<BoardAnswer> findByBoardPostIdAndDeletedFalse(Long boardPostId);

	Optional<BoardAnswer> findByIdAndDeletedFalse(Long id);
}
