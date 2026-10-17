package com.cloudidentity.scheduler;

import com.cloudidentity.config.AppProperties;
import com.cloudidentity.entity.BehaviorFingerprint;
import com.cloudidentity.entity.BehaviorHistory;
import com.cloudidentity.entity.IamIdentity;
import com.cloudidentity.repository.BehaviorFingerprintRepository;
import com.cloudidentity.repository.BehaviorHistoryRepository;
import com.cloudidentity.repository.IamIdentityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Recomputes 10-dimension behavior fingerprints from the last 30 days of behavior history.
 * Runs every hour — configurable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FingerprintRecomputeScheduler {

    private final AppProperties appProperties;
    private final IamIdentityRepository iamIdentityRepository;
    private final BehaviorFingerprintRepository fingerprintRepository;
    private final BehaviorHistoryRepository behaviorHistoryRepository;

    @Scheduled(fixedDelayString = "${app.scheduler.fingerprint-poll-ms:3600000}")
    @Transactional
    public void recomputeFingerprints() {
        if (!appProperties.getScheduler().isFingerprintEnabled()) {
            log.debug("Fingerprint recompute scheduler is disabled. Skipping.");
            return;
        }

        List<IamIdentity> identities = iamIdentityRepository.findAll().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsMonitored()))
                .toList();

        log.info("[SCHEDULER] Recomputing behavior fingerprints for {} monitored identities...", identities.size());
        int updated = 0;

        for (IamIdentity identity : identities) {
            try {
                recomputeForIdentity(identity);
                updated++;
            } catch (Exception ex) {
                log.warn("[SCHEDULER] Failed to recompute fingerprint for identity {}: {}", identity.getArn(), ex.getMessage());
            }
        }

        log.info("[SCHEDULER] Fingerprint recomputation complete. Updated: {}/{}", updated, identities.size());
    }

    private void recomputeForIdentity(IamIdentity identity) {
        LocalDate baselineStart = LocalDate.now().minusDays(30);
        List<BehaviorHistory> history = behaviorHistoryRepository
                .findByIamIdentity_IdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(identity.getId(), baselineStart);

        if (history.isEmpty()) {
            log.debug("No baseline history for identity: {}. Skipping fingerprint update.", identity.getArn());
            return;
        }

        // Aggregate dimension statistics
        OptionalDouble avgApiCalls = history.stream()
                .mapToInt(BehaviorHistory::getApiCallCount).average();
        OptionalDouble avgLogins = history.stream()
                .mapToInt(BehaviorHistory::getLoginCount).average();
        OptionalDouble avgFailedLogins = history.stream()
                .mapToInt(BehaviorHistory::getFailedLoginCount).average();
        OptionalDouble avgDataDownload = history.stream()
                .mapToLong(BehaviorHistory::getDataDownloadBytes).average();
        long maxDataDownload = history.stream()
                .mapToLong(BehaviorHistory::getDataDownloadBytes).max().orElse(0);
        OptionalDouble avgSessionDuration = history.stream()
                .filter(h -> h.getAvgSessionMinutes() != null)
                .mapToDouble(h -> h.getAvgSessionMinutes().doubleValue()).average();
        OptionalDouble avgPrivEscalations = history.stream()
                .mapToInt(BehaviorHistory::getPrivilegeEscalations).average();

        BehaviorFingerprint fp = fingerprintRepository.findByIamIdentity_Id(identity.getId())
                .orElse(BehaviorFingerprint.builder().iamIdentity(identity).build());

        fp.setAvgDailyApiCalls(avgApiCalls.isPresent() ? BigDecimal.valueOf(avgApiCalls.getAsDouble()) : BigDecimal.ZERO);
        fp.setAvgLoginsPerDay(avgLogins.isPresent() ? BigDecimal.valueOf(avgLogins.getAsDouble()) : BigDecimal.ZERO);
        fp.setAvgFailedLoginsPerDay(avgFailedLogins.isPresent() ? BigDecimal.valueOf(avgFailedLogins.getAsDouble()) : BigDecimal.ZERO);
        fp.setAvgDataDownloadBytes(avgDataDownload.isPresent() ? (long) avgDataDownload.getAsDouble() : 0L);
        fp.setMaxDataDownloadBytes(maxDataDownload);
        fp.setAvgSessionDurationMinutes(avgSessionDuration.isPresent() ? BigDecimal.valueOf(avgSessionDuration.getAsDouble()) : BigDecimal.ZERO);
        fp.setAvgPrivilegeEscalationsPerWeek(avgPrivEscalations.isPresent() ? BigDecimal.valueOf(avgPrivEscalations.getAsDouble() * 7) : BigDecimal.ZERO);
        fp.setBaselineComputedAt(LocalDateTime.now());

        fingerprintRepository.save(fp);
    }

    public void triggerManually() {
        log.info("[SCHEDULER] Manual fingerprint recompute triggered by administrator");
        recomputeFingerprints();
    }
}
