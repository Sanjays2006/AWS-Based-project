package com.cloudidentity.repository;

import com.cloudidentity.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @Query("SELECT a FROM ActivityLog a WHERE " +
           "(CAST(:identityId AS long) IS NULL OR a.iamIdentity.id = :identityId) AND " +
           "(CAST(:search AS string) IS NULL OR LOWER(a.eventName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.eventSource) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.sourceIp) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(CAST(:startDate AS timestamp) IS NULL OR a.eventTime >= :startDate) AND " +
           "(CAST(:endDate AS timestamp) IS NULL OR a.eventTime <= :endDate)")
    Page<ActivityLog> filterLogs(@Param("identityId") Long identityId,
                                 @Param("search") String search,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate,
                                 Pageable pageable);

    @Query("SELECT a.awsService, COUNT(a) FROM ActivityLog a GROUP BY a.awsService")
    List<Object[]> getAwsServiceUsageCounts();

    List<ActivityLog> findTop10ByIamIdentity_IdOrderByEventTimeDesc(Long identityId);

    /** De-duplication support: look up a previously-ingested CloudTrail event by its event id. */
    boolean existsByEventId(String eventId);

    Optional<ActivityLog> findByEventId(String eventId);
}
