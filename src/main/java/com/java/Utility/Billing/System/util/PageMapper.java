package com.java.Utility.Billing.System.util;

import com.java.Utility.Billing.System.dto.response.PageResponse;
import org.springframework.data.domain.Page;

public final class PageMapper {

    private PageMapper() {}

    public static <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
