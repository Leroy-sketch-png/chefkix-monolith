package com.chefkix.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeletedEvent {

    private String userId;
}