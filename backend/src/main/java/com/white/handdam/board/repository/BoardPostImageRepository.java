package com.white.handdam.board.repository;

import com.white.handdam.board.entity.BoardPostImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardPostImageRepository extends JpaRepository<BoardPostImage, Long> {

	List<BoardPostImage> findByBoardPostIdOrderByOrderIndexAsc(Long boardPostId);

	@Query("select max(i.orderIndex) from BoardPostImage i where i.boardPost.id = :boardPostId")
	Optional<Integer> findMaxOrderIndexByBoardPostId(@Param("boardPostId") Long boardPostId);
}
