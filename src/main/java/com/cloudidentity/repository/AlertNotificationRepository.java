package com.cloudidentity.repository;

import com.cloudidentity.entity.AlertNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertNotificationRepository extends JpaRepository<AlertNotification, Long> {

    List<AlertNotification> findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    Page<AlertNotification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUser_IdAndIsReadFalse(Long userId);
}
