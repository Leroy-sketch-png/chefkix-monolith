package com.chefkix.social.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO để client gửi yêu cầu tạo một Reply mới.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyRequest {

    @NotBlank(message = "Nội dung không được để trống")
    @Size(max = 2000, message = "Nội dung trả lời quá dài")
    private String content;

    @NotBlank(message = "Cần có ID của comment cha")
    @Size(max = 100)
    private String parentCommentId;

    @Size(max = 10, message = "Maximum 10 tagged users")
    private List<String> taggedUserIds;
}