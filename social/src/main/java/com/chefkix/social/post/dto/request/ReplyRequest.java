package com.chefkix.social.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for creating a new Reply.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyRequest {

    @NotBlank(message = "Content must not be blank")
    @Size(max = 2000, message = "Reply content is too long")
    private String content;

    @NotBlank(message = "Parent comment ID is required")
    @Size(max = 100)
    private String parentCommentId;

    @Size(max = 10, message = "Maximum 10 tagged users")
    private List<String> taggedUserIds;
}