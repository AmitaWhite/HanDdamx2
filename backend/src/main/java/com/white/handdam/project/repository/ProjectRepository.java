package com.white.handdam.project.repository;

import com.white.handdam.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndDeletedFalse(Long id);

    /** 특정 크리에이터의 프로젝트 목록 (삭제 제외, 최신순) */
    List<Project> findByCreatorIdAndDeletedFalseOrderByCreatedAtDesc(Long creatorId);

    /** 크리에이터 프로필의 projectCount용 */
    long countByCreatorIdAndDeletedFalse(Long creatorId);
}