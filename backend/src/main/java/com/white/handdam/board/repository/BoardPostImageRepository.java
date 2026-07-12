package com.white.handdam.board.repository;

import com.white.handdam.board.entity.BoardPostImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardPostImageRepository extends JpaRepository<BoardPostImage, Long> {

	List<BoardPostImage> findByBoardPostIdOrderByOrderIndexAsc(Long boardPostId);
}
