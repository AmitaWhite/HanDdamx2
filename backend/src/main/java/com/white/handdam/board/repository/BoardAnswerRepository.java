package com.white.handdam.board.repository;

import com.white.handdam.board.entity.BoardAnswer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardAnswerRepository extends JpaRepository<BoardAnswer, Long> {

	boolean existsByBoardPostId(Long boardPostId);

	Optional<BoardAnswer> findByIdAndDeletedFalse(Long id);
}
