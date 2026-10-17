package com.cloudidentity.repository;

import com.cloudidentity.entity.RiskRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskRuleRepository extends JpaRepository<RiskRule, Long> {

    Optional<RiskRule> findByRuleCode(String ruleCode);

    List<RiskRule> findByIsActiveTrue();

    List<RiskRule> findByCategory(String category);
}
