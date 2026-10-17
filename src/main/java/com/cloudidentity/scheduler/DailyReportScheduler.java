package com.cloudidentity.scheduler;

import com.cloudidentity.config.AppProperties;
import com.cloudidentity.entity.BehaviorHistory;
import com.cloudidentity.entity.IamIdentity;
import com.cloudidentity.entity.RiskScore;
import com.cloudidentity.repository.BehaviorHistoryRepository;
import com.cloudidentity.repository.IamIdentityRepository;
import com.cloudidentity.repository.RiskScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * At midnight, snapshot today's behavior statistics into behavior_history
 * and generate an end-of-day risk score summary.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReportScheduler {

    private final AppProperties appProperties;
    private final IamIdentityRepository iamIdentityRepository;
    private final BehaviorHistoryRepository behaviorHistoryRepository;
    private final RiskScoreRepository riskScoreRepository;

    @Scheduled(cron = "0 0 0 * * *") // Midnight daily
    @Transactional
    public void generateDailyReport() {
        if (!appProperties.getScheduler().isReportEnabled()) {
            log.debug("Daily report scheduler is disabled. Skipping.");
            return;
        }

        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[SCHEDULER] Generating daily behavior snapshots for date: {}", yesterday);

        List<IamIdentity> identities = iamIdentityRepository.findAll().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsMonitored()))
                .toList();

        for (IamIdentity identity : identities) {
            try {
                // Check if snapshot already exists (idempotent)
                boolean snapshotExists = behaviorHistoryRepository
                        .findByIamIdentity_IdAndSnapshotDate(identity.getId(), yesterday)
                        .isPresent();

                if (!snapshotExists) {
                    // Fetch yesterday's end-of-day risk score
                    List<RiskScore> scores = riskScoreRepository.findHistoryByIdentityId(identity.getId());
                    BigDecimal eodScore = scores.isEmpty() ? BigDecimal.ZERO : scores.get(0).getScore();

                    BehaviorHistory snapshot = BehaviorHistory.builder()
                            .iamIdentity(identity)
                            .snapshotDate(yesterday)
                            .apiCallCount(0)
                            .loginCount(0)
                            .failedLoginCount(0)
                            .uniqueRegions(0)
                            .uniqueDevices(0)
                            .privilegeEscalations(0)
                            .dataDownloadBytes(0L)
                            .sessionCount(0)
                            .riskScoreEod(eodScore)
                            .build();

                    behaviorHistoryRepository.save(snapshot);
                    log.debug("[SCHEDULER] Snapshot saved for identity: {}", identity.getArn());
                }
            } catch (Exception ex) {
                log.warn("[SCHEDULER] Failed to create daily snapshot for identity {}: {}", identity.getArn(), ex.getMessage());
            }
        }

        log.info("[SCHEDULER] Daily report generation complete for {} identities.", identities.size());
    }

    public void triggerManually() {
        log.info("[SCHEDULER] Manual daily report triggered by administrator");
        generateDailyReport();
    }
}
