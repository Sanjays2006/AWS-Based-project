package com.cloudidentity.repository;

import com.cloudidentity.entity.BehaviorHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BehaviorHistoryRepository extends JpaRepository<BehaviorHistory, Long> {

    Optional<BehaviorHistory> findByIamIdentity_IdAndSnapshotDate(Long identityId, LocalDate date);

    List<BehaviorHistory> findByIamIdentity_IdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(Long identityId, LocalDate since);
}
