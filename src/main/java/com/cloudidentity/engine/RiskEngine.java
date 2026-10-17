package com.cloudidentity.engine;

import com.cloudidentity.engine.model.EngineResult;
import com.cloudidentity.entity.RiskScore;

public interface RiskEngine {

    /**
     * Translates rule outputs into a final weighted RiskScore entity.
     */
    RiskScore calculateRisk(Long identityId, EngineResult ruleResult);
}
