package com.white.handdam.category.repository;

import com.white.handdam.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** 활성 카테고리 목록 */
    List<Category> findAllByActiveTrueOrderByNameAsc();

    /** 전체 카테고리 목록 */
    List<Category> findAllByOrderByNameAsc();

    /** 이름 중복 검사 */
    boolean existsByName(String name);
}