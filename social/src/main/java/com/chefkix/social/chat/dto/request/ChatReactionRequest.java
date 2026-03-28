package com.chefkix.social.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatReactionRequest {
    @NotBlank
    @Size(max = 20, message = "Emoji must be at most 20 characters")
    String emoji;
}
