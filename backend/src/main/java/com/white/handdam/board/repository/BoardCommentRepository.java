package com.white.handdam.board.repository;

import com.white.handdam.board.entity.BoardComment;
import java.util.List;
import java.util.Optional;

import com.white.handdam.member.dto.response.MyBoardCommentResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {

	List<BoardComment> findByBoardPostIdOrderByCreatedAtAsc(Long boardPostId);

	Optional<BoardComment> findByIdAndDeletedFalse(Long id);

    // KSY-021 - 내가 작성한 게시판 댓글 - 생성일 최신순
    @Query("""
            select new com.white.handdam.member.dto.response.MyBoardCommentResponse(
                        bc.id, bc.content, bp.id, bp.title, bc.createdAt
                        )
            from BoardComment bc
            join bc.boardPost bp
            where bc.memberId = :memberId
            and bc.deleted = false
            and bp.deleted = false
            order by bc.createdAt desc
            """)
    Slice<MyBoardCommentResponse> findMyComments(@Param("memberId") Long memberId, Pageable pageable);

    // KSY-015
    long countByMemberIdAndDeletedFalse(Long memberId);
}
