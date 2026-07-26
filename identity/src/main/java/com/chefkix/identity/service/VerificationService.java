package com.chefkix.identity.service;

import com.chefkix.identity.dto.request.VerificationApplyRequest;
import com.chefkix.identity.dto.response.VerificationResponse;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.entity.VerificationRequest;
import com.chefkix.identity.repository.UserProfileRepository;
import com.chefkix.identity.repository.VerificationRequestRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VerificationService {

  VerificationRequestRepository verificationRepo;
  UserProfileRepository profileRepo;

  public VerificationResponse applyForVerification(String userId, VerificationApplyRequest request) {
    UserProfile profile =
        profileRepo.findByUserId(userId).orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

    if (profile.isVerified()) {
      throw new AppException(ErrorCode.VERIFICATION_ALREADY_VERIFIED);
    }

    if (verificationRepo.existsByUserIdAndStatus(userId, "PENDING")) {
      throw new AppException(ErrorCode.VERIFICATION_ALREADY_PENDING);
    }

    VerificationRequest entity =
        VerificationRequest.builder()
            .userId(userId)
            .reason(request != null ? request.getReason() : null)
            .paymentId(request != null ? request.getPaymentId() : null)
            .build();

    entity = verificationRepo.save(entity);
    log.info("Verification request created for userId={}, requestId={}", userId, entity.getId());
    return toResponse(entity);
  }

  public VerificationResponse getVerificationStatus(String userId) {
    VerificationRequest req =
        verificationRepo
            .findTopByUserIdOrderByRequestedAtDesc(userId)
            .orElseThrow(() -> new AppException(ErrorCode.VERIFICATION_REQUEST_NOT_FOUND));
    return toResponse(req);
  }

  @Transactional
  public VerificationResponse approveVerification(String requestId, String adminUserId, String notes) {
    VerificationRequest req =
        verificationRepo.findById(requestId).orElseThrow(() -> new AppException(ErrorCode.VERIFICATION_REQUEST_NOT_FOUND));

    if (!"PENDING".equals(req.getStatus())) {
      throw new AppException(ErrorCode.INVALID_ACTION);
    }

    req.setStatus("APPROVED");
    req.setReviewedBy(adminUserId);
    req.setAdminNotes(notes);
    req.setReviewedAt(Instant.now());
    verificationRepo.save(req);

    UserProfile profile = profileRepo
        .findByUserId(req.getUserId())
        .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));
    profile.setVerified(true);
    profileRepo.save(profile);

    log.info("Verification APPROVED for userId={}, by admin={}", req.getUserId(), adminUserId);
    return toResponse(req);
  }

  public VerificationResponse rejectVerification(String requestId, String adminUserId, String notes) {
    VerificationRequest req =
        verificationRepo.findById(requestId).orElseThrow(() -> new AppException(ErrorCode.VERIFICATION_REQUEST_NOT_FOUND));

    if (!"PENDING".equals(req.getStatus())) {
      throw new AppException(ErrorCode.INVALID_ACTION);
    }

    req.setStatus("REJECTED");
    req.setReviewedBy(adminUserId);
    req.setAdminNotes(notes);
    req.setReviewedAt(Instant.now());
    verificationRepo.save(req);

    log.info("Verification REJECTED for userId={}, by admin={}", req.getUserId(), adminUserId);
    return toResponse(req);
  }

  private VerificationResponse toResponse(VerificationRequest entity) {
    return VerificationResponse.builder()
        .id(entity.getId())
        .userId(entity.getUserId())
        .status(entity.getStatus())
        .reason(entity.getReason())
        .adminNotes(entity.getAdminNotes())
        .requestedAt(entity.getRequestedAt())
        .reviewedAt(entity.getReviewedAt())
        .build();
  }
}
