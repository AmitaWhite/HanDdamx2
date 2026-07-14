package com.white.handdam.board.repository;

import com.white.handdam.board.entity.BoardAnswer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardAnswerRepository extends JpaRepository<BoardAnswer, Long> {

	// 이미 공식 답변이 등록된 게시글인지 확인
	boolean existsByBoardPostId(Long boardPostId);

	Optional<BoardAnswer> findByIdAndDeletedFalse(Long id);
}
