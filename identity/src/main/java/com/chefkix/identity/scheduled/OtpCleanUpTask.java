package com.chefkix.identity.scheduled;

import com.chefkix.identity.repository.SignupRequestRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class OtpCleanUpTask {
  private static final Logger log = LoggerFactory.getLogger(OtpCleanUpTask.class);
  private final SignupRequestRepository signupRequestRepository;

  @Scheduled(fixedDelayString = "${app.signup.cleanup-millis:3600000}")
  @Transactional
  public void cleanupExpired() {
    int deleted = signupRequestRepository.deleteExpired(Instant.now());
    if (deleted > 0) {
      log.info("Deleted {} expired signup requests", deleted);
    }
  }
}
