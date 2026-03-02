package com.chefkix.shared.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

/**
 * Pagination metadata returned alongside paginated responses.
 * <p>
 * Extracted from {@link ApiResponse} inner class to a standalone DTO
 * for direct reuse and clearer serialization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaginationMeta {

    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean first;
    boolean last;

    /** Build from a Spring Data {@link Page}. */
    public static PaginationMeta from(Page<?> page) {
        return PaginationMeta.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
