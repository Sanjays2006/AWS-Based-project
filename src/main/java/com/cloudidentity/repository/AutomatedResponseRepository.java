package com.cloudidentity.repository;

import com.cloudidentity.entity.AutomatedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AutomatedResponseRepository extends JpaRepository<AutomatedResponse, Long> {

    Page<AutomatedResponse> findByIamIdentity_IdOrderByCreatedAtDesc(Long identityId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM AutomatedResponse r WHERE r.status = :status AND r.executedAt >= :since")
    long countByStatusSince(@Param("status") String status, @Param("since") LocalDateTime since);

    List<AutomatedResponse> findTop10ByOrderByCreatedAtDesc();
}
