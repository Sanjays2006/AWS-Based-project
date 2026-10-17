package com.cloudidentity.repository;

import com.cloudidentity.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(CAST(:userId AS long) IS NULL OR a.user.id = :userId) AND " +
           "(CAST(:search AS string) IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(CAST(:startDate AS timestamp) IS NULL OR a.createdAt >= :startDate) AND " +
           "(CAST(:endDate AS timestamp) IS NULL OR a.createdAt <= :endDate)")
    Page<AuditLog> filterAuditLogs(@Param("userId") Long userId,
                                   @Param("search") String search,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   Pageable pageable);
}
