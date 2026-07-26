package com.chefkix.social.post.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PollData {
    String question;
    String optionA;
    String optionB;
    @Builder.Default
    int votesA = 0;
    @Builder.Default
    int votesB = 0;
}
