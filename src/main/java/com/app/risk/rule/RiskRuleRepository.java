package com.app.risk.rule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskRuleRepository extends JpaRepository<RiskRule, Long> {
    List<RiskRule> findByActiveTrue();
}

