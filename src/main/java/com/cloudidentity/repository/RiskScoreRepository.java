package com.cloudidentity.repository;

import com.cloudidentity.entity.RiskScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RiskScoreRepository extends JpaRepository<RiskScore, Long> {

    @Query(value = "SELECT DISTINCT ON (iam_identity_id) * FROM risk_scores ORDER BY iam_identity_id, computed_at DESC", nativeQuery = true)
    List<RiskScore> findLatestScoresForAllIdentities();

    @Query(value = "SELECT r.* FROM risk_scores r " +
                   "INNER JOIN (" +
                   "  SELECT iam_identity_id, MAX(computed_at) as max_c " +
                   "  FROM risk_scores GROUP BY iam_identity_id" +
                   ") latest ON r.iam_identity_id = latest.iam_identity_id AND r.computed_at = latest.max_c " +
                   "WHERE (:riskLevel::text IS NULL OR r.risk_level = :riskLevel)", 
           countQuery = "SELECT COUNT(DISTINCT r.iam_identity_id) FROM risk_scores r WHERE (:riskLevel::text IS NULL OR r.risk_level = :riskLevel)",
           nativeQuery = true)
    Page<RiskScore> findLatestScoresByRiskLevel(@Param("riskLevel") String riskLevel, Pageable pageable);

    @Query("SELECT r FROM RiskScore r WHERE r.iamIdentity.id = :identityId ORDER BY r.computedAt DESC")
    List<RiskScore> findHistoryByIdentityId(@Param("identityId") Long identityId);

    @Query("SELECT r FROM RiskScore r WHERE r.iamIdentity.id = :identityId ORDER BY r.computedAt DESC")
    Page<RiskScore> findHistoryByIdentityIdPaged(@Param("identityId") Long identityId, Pageable pageable);

    @Query("SELECT r FROM RiskScore r WHERE r.computedAt >= :since ORDER BY r.computedAt ASC")
    List<RiskScore> findTrendData(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(r) FROM RiskScore r WHERE r.riskLevel = :level AND r.computedAt >= :since")
    long countByRiskLevelSince(@Param("level") String level, @Param("since") LocalDateTime since);
}
