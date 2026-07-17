package com.white.handdam.category.entity;

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

/**
 * 크리에이터 프로젝트 분류 카테고리 엔티티.
 */
@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 카테고리명 */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /** 활성 여부 */
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Builder
    private Category(String name) {
        this.name = name;
        this.active = true;
    }

    /**
     * 카테고리명 수정
     */
    public void updateName(String name) {
        this.name = name;
    }

    /**
     * 활성·비활성 토글
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}