package com.cloudidentity.repository;

import com.cloudidentity.entity.ReportHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {
    
    Page<ReportHistory> findByReportTypeOrderByGeneratedAtDesc(String reportType, Pageable pageable);
}
