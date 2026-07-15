package com.white.handdam.board.repository;

import com.white.handdam.board.entity.BoardComment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {

	List<BoardComment> findByBoardPostIdOrderByCreatedAtAsc(Long boardPostId);

	Optional<BoardComment> findByIdAndDeletedFalse(Long id);
}
