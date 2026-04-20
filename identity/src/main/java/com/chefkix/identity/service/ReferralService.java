package com.chefkix.identity.service;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.identity.dto.response.ReferralCodeResponse;
import com.chefkix.identity.dto.response.ReferralStatsResponse;
import com.chefkix.identity.entity.ReferralCode;
import com.chefkix.identity.entity.ReferralRedemption;
import com.chefkix.identity.repository.ReferralCodeRepository;
import com.chefkix.identity.repository.ReferralRedemptionRepository;
import com.chefkix.shared.event.XpRewardEvent;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import java.security.SecureRandom;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReferralService {

    static final String XP_DELIVERY_TOPIC = "xp-delivery";
    static final int REFERRAL_XP = 100;
    static final String SHARE_URL_BASE = "https://chefkix.app/join?ref=";
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    ReferralCodeRepository referralCodeRepository;
    ReferralRedemptionRepository referralRedemptionRepository;
    KafkaTemplate<String, Object> kafkaTemplate;
    ProfileProvider profileProvider;

    /**
     * Get the current user's referral code, creating one if it doesn't exist.
     */
    public ReferralCodeResponse getOrCreateMyCode(String userId) {
        ReferralCode code = referralCodeRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ReferralCode newCode = ReferralCode.builder()
                            .userId(userId)
                            .code(generateUniqueCode())
                            .build();
                    return referralCodeRepository.save(newCode);
                });
        return toCodeResponse(code);
    }

    /**
     * Redeem a referral code. Awards XP to both referrer and referred user.
     */
    @Transactional
    public ReferralCodeResponse redeemCode(String userId, String code) {
        ReferralCode referralCode = referralCodeRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new AppException(ErrorCode.REFERRAL_CODE_NOT_FOUND));

        // Cannot redeem own code
        if (referralCode.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.REFERRAL_SELF_REDEEM);
        }

        // Check max uses
        if (!referralCode.getActive() || referralCode.getUsageCount() >= referralCode.getMaxUses()) {
            throw new AppException(ErrorCode.REFERRAL_CODE_EXHAUSTED);
        }

        // Save redemption first — unique index on referredUserId prevents double redemption atomically
        ReferralRedemption redemption = ReferralRedemption.builder()
                .referrerUserId(referralCode.getUserId())
                .referredUserId(userId)
                .referralCodeId(referralCode.getId())
                .xpAwarded(REFERRAL_XP)
                .build();
        try {
            referralRedemptionRepository.save(redemption);
        } catch (DuplicateKeyException e) {
            throw new AppException(ErrorCode.REFERRAL_ALREADY_REDEEMED);
        }

        // Increment usage count
        referralCode.setUsageCount(referralCode.getUsageCount() + 1);
        referralCodeRepository.save(referralCode);

        // Get referred user's name for the referrer's notification
        String referredUsername;
        try {
            BasicProfileInfo profile = profileProvider.getBasicProfile(userId);
            referredUsername = profile.getUsername() != null ? profile.getUsername() : "someone";
        } catch (Exception e) {
            log.warn("Could not fetch profile for referred user {}: {}", userId, e.getMessage());
            referredUsername = "someone";
        }

        // Award XP to referrer
        sendXpReward(referralCode.getUserId(), "REFERRAL_BONUS",
                "Referral bonus: " + referredUsername + " joined via your invite");

        // Award XP to referred user
        sendXpReward(userId, "REFERRAL_WELCOME",
                "Welcome bonus from referral");

        log.info("Referral redeemed: referrer={}, referred={}, code={}",
                referralCode.getUserId(), userId, referralCode.getCode());

        return toCodeResponse(referralCode);
    }

    /**
     * Get referral statistics for the current user.
     */
    public ReferralStatsResponse getMyStats(String userId) {
        ReferralCode code = referralCodeRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ReferralCode newCode = ReferralCode.builder()
                            .userId(userId)
                            .code(generateUniqueCode())
                            .build();
                    return referralCodeRepository.save(newCode);
                });

        List<ReferralRedemption> redemptions = referralRedemptionRepository
                .findByReferrerUserId(userId);

        List<ReferralStatsResponse.ReferralDetail> details = redemptions.stream()
                .map(r -> {
                    String username = "Unknown";
                    String avatar = null;
                    try {
                        BasicProfileInfo profile = profileProvider.getBasicProfile(r.getReferredUserId());
                        username = profile.getUsername() != null ? profile.getUsername() : "Unknown";
                        avatar = profile.getAvatarUrl();
                    } catch (Exception e) {
                        log.warn("Could not fetch profile for user {}: {}", r.getReferredUserId(), e.getMessage());
                    }
                    return ReferralStatsResponse.ReferralDetail.builder()
                            .referredUsername(username)
                            .referredAvatar(avatar)
                            .xpAwarded(r.getXpAwarded())
                            .redeemedAt(r.getRedeemedAt())
                            .build();
                })
                .toList();

        long totalXp = redemptions.stream()
                .mapToLong(ReferralRedemption::getXpAwarded)
                .sum();

        return ReferralStatsResponse.builder()
                .code(code.getCode())
                .totalReferrals((long) redemptions.size())
                .totalXpEarned(totalXp)
                .referrals(details)
                .build();
    }

    private void sendXpReward(String userId, String source, String description) {
        try {
            XpRewardEvent event = XpRewardEvent.builder()
                    .userId(userId)
                    .amount(REFERRAL_XP)
                    .source(source)
                    .description(description)
                    .build();
            kafkaTemplate.send(XP_DELIVERY_TOPIC, userId, event);
        } catch (Exception e) {
            log.error("Failed to send referral XP for user={}, source={}: {}",
                    userId, source, e.getMessage());
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(SECURE_RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!referralCodeRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ReferralCodeResponse toCodeResponse(ReferralCode code) {
        return ReferralCodeResponse.builder()
                .code(code.getCode())
                .usageCount(code.getUsageCount())
                .maxUses(code.getMaxUses())
                .active(code.getActive())
                .createdAt(code.getCreatedAt())
                .shareUrl(SHARE_URL_BASE + code.getCode())
                .build();
    }
}
