package com.white.handdam.project.dto.request;

/**
 * 프로젝트 생성 요청 DTO
 */
public record CreateProjectRequest(String title, String description, Long categoryId) {}