package com.white.handdam.project.entity;

import com.white.handdam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 크리에이터의 피드 폴더 엔티티.
 * 프로젝트는 공개 등급이 없으며 피드를 묶는 역할만 한다.
 */
@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 프로젝트 소유 크리에이터 (FK → MEMBER.id) */
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    /** 프로젝트 카테고리 (FK → CATEGORY.id) */
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    /** 프로젝트명 */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** 프로젝트 설명 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 커버 이미지 URL */
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    /** 커버 이미지 S3 객체 키 */
    @Column(name = "cover_image_storage_key", length = 500)
    private String coverImageStorageKey;

    /** 소프트 삭제 여부 */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    /** 삭제일 */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder
    private Project(Long creatorId, Long categoryId, String title, String description,
                    String coverImageUrl, String coverImageStorageKey) {
        this.creatorId = creatorId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.coverImageStorageKey = coverImageStorageKey;
    }

    /**
     * 프로젝트 정보 수정
     */
    public void update(String title, String description, Long categoryId) {
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
    }

    /**
     * 커버 이미지 변경
     */
    public void updateCoverImage(String url, String storageKey) {
        this.coverImageUrl = url;
        this.coverImageStorageKey = storageKey;
    }

    /**
     * 커버 이미지 제거
     */
    public void clearCoverImage() {
        this.coverImageUrl = null;
        this.coverImageStorageKey = null;
    }

    /**
     * 소프트 삭제
     */
    public void delete() {
        this.deleted = true;
        this.deletedAt = Instant.now();
    }
}