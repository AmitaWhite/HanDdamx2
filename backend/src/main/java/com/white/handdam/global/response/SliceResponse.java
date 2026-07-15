package com.white.handdam.global.response;

import org.springframework.data.domain.Slice;
import java.util.List;

// [LYJ-006-009] 목록 조회 시 무한 스크롤을 위한 SliceResponse
public record SliceResponse<T>(
        List<T> content,
        boolean hasNext,
        int page,
        int size
) {
    public static <T> SliceResponse<T> from(Slice<T> slice){
        return new SliceResponse<>(
                slice.getContent(),
                slice.hasNext(),
                slice.getNumber(),
                slice.getSize()
        );
    }
}
