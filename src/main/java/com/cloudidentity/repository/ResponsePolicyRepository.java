package com.cloudidentity.repository;

import com.cloudidentity.entity.ResponsePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ResponsePolicyRepository extends JpaRepository<ResponsePolicy, Long> {

    List<ResponsePolicy> findByIsActiveTrueOrderByPriorityAsc();

    @Query("SELECT p FROM ResponsePolicy p WHERE p.isActive = true AND :score >= p.minRiskScore ORDER BY p.priority ASC, p.minRiskScore DESC")
    List<ResponsePolicy> findMatchingPoliciesForScore(@Param("score") BigDecimal score);
}
