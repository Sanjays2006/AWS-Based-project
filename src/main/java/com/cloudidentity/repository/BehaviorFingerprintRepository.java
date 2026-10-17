package com.cloudidentity.repository;

import com.cloudidentity.entity.BehaviorFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BehaviorFingerprintRepository extends JpaRepository<BehaviorFingerprint, Long> {
    Optional<BehaviorFingerprint> findByIamIdentity_Id(Long identityId);
}
