package com.chefkix.identity.service;

import com.chefkix.identity.entity.CreatorTipSettings;
import com.chefkix.identity.entity.Tip;
import com.chefkix.identity.repository.CreatorTipSettingsRepository;
import com.chefkix.identity.repository.TipRepository;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipService {

    private final CreatorTipSettingsRepository tipSettingsRepository;
    private final TipRepository tipRepository;

    public CreatorTipSettings getOrCreateSettings(String userId) {
        return tipSettingsRepository.findByUserId(userId)
                .orElseGet(() -> tipSettingsRepository.save(
                        CreatorTipSettings.builder().userId(userId).build()));
    }

    public CreatorTipSettings updateSettings(String userId, boolean tipsEnabled,
                                              int[] suggestedAmounts, String thankYouMessage) {
        CreatorTipSettings settings = getOrCreateSettings(userId);
        settings.setTipsEnabled(tipsEnabled);
        if (suggestedAmounts != null && suggestedAmounts.length > 0) {
            settings.setSuggestedAmounts(suggestedAmounts);
        }
        if (thankYouMessage != null) {
            settings.setThankYouMessage(thankYouMessage);
        }
        return tipSettingsRepository.save(settings);
    }

    public CreatorTipSettings getCreatorTipSettings(String creatorId) {
        return tipSettingsRepository.findByUserId(creatorId)
                .orElse(null);
    }

    @Transactional
    public Tip sendTip(String tipperId, String creatorId, String recipeId,
                       int amountCents, String message) {
        if (tipperId.equals(creatorId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        CreatorTipSettings settings = tipSettingsRepository.findByUserId(creatorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!settings.isTipsEnabled()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (settings.getPayoutAccountId() == null) {
            // Phase 0: Store the tip intent but mark as pending (no payment processing)
            log.info("Tip stored as pending — creator {} has no payout account connected", creatorId);
        }

        Tip tip = Tip.builder()
                .tipperId(tipperId)
                .creatorId(creatorId)
                .recipeId(recipeId)
                .amountCents(amountCents)
                .currency(settings.getCurrency())
                .message(message)
                .status(settings.getPayoutAccountId() != null ? "processing" : "pending")
                .build();

        return tipRepository.save(tip);
    }

    public List<Tip> getReceivedTips(String creatorId) {
        return tipRepository.findByCreatorIdOrderByCreatedAtDesc(creatorId);
    }

    public List<Tip> getSentTips(String tipperId) {
        return tipRepository.findByTipperIdOrderByCreatedAtDesc(tipperId);
    }

    public long getTipCount(String creatorId) {
        return tipRepository.countByCreatorId(creatorId);
    }
}
