package com.chefkix.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Unified API response wrapper used by ALL endpoints.
 * <p>
 * Consolidated from 6 service-specific copies into a single source of truth.
 * Uses {@link PaginationMeta} for paginated responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    boolean success;
    int statusCode;
    String message;
    T data;
    PaginationMeta pagination;

    // ─── Factory Methods ────────────────────────────────────────────

    /** 200 OK with data. */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(200)
                .data(data)
                .build();
    }

    /** 200 OK with data and message. */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(200)
                .data(data)
                .message(message)
                .build();
    }

    /** 201 Created with data. */
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(201)
                .message("Created successfully")
                .data(data)
                .build();
    }

    /** Error response (no data). */
    public static <T> ApiResponse<T> error(int statusCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .build();
    }

    /** Paginated response with DTO mapping. */
    public static <T, R> ApiResponse<List<R>> successPage(Page<T> page, Function<T, R> mapper) {
        List<R> items = page.getContent().stream().map(mapper).toList();
        return ApiResponse.<List<R>>builder()
                .success(true)
                .statusCode(200)
                .data(items)
                .pagination(PaginationMeta.from(page))
                .build();
    }

    /** Paginated response without mapping (entity == DTO). */
    public static <T> ApiResponse<List<T>> successPage(Page<T> page) {
        return ApiResponse.<List<T>>builder()
                .success(true)
                .statusCode(200)
                .data(page.getContent())
                .pagination(PaginationMeta.from(page))
                .build();
    }

    /** Non-paginated list response. */
    public static <T> ApiResponse<List<T>> successList(List<T> list) {
        return ApiResponse.<List<T>>builder()
                .success(true)
                .statusCode(200)
                .data(list)
                .build();
    }
}
