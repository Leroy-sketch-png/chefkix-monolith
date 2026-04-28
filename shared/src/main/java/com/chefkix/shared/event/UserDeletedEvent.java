package com.chefkix.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Spring application event published after an account is deleted.
 * Used for module-local cleanup that does not yet have a dedicated SPI contract.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeletedEvent {

    private String userId;
}