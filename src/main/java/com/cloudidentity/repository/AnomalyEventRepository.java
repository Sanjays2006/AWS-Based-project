package com.cloudidentity.repository;

import com.cloudidentity.entity.AnomalyEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnomalyEventRepository extends JpaRepository<AnomalyEvent, Long> {

    @Query("SELECT a FROM AnomalyEvent a WHERE " +
           "(CAST(:identityId AS long) IS NULL OR a.iamIdentity.id = :identityId) AND " +
           "(CAST(:severity AS string) IS NULL OR a.severity = :severity) AND " +
           "(CAST(:isAcknowledged AS boolean) IS NULL OR a.isAcknowledged = :isAcknowledged) AND " +
           "(CAST(:startDate AS timestamp) IS NULL OR a.createdAt >= :startDate) AND " +
           "(CAST(:endDate AS timestamp) IS NULL OR a.createdAt <= :endDate)")
    Page<AnomalyEvent> filterAnomalies(@Param("identityId") Long identityId,
                                       @Param("severity") String severity,
                                       @Param("isAcknowledged") Boolean isAcknowledged,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);

    long countByIsAcknowledgedFalse();

    long countBySeverity(String severity);

    @Query("SELECT COUNT(a) FROM AnomalyEvent a WHERE a.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AnomalyEvent a WHERE a.severity = :severity AND a.createdAt >= :since")
    long countBySeveritySince(@Param("severity") String severity, @Param("since") LocalDateTime since);

    List<AnomalyEvent> findTop5ByIsAcknowledgedFalseOrderByCreatedAtDesc();

    List<AnomalyEvent> findByIamIdentity_IdOrderByCreatedAtDesc(Long identityId);
}
