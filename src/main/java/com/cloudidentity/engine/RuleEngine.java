package com.cloudidentity.engine;

import com.cloudidentity.engine.model.BehaviorVector;
import com.cloudidentity.engine.model.EngineResult;
import com.cloudidentity.entity.BehaviorFingerprint;

public interface RuleEngine {

    /**
     * Evaluates a BehaviorVector against configured rules and user behavior baselines.
     */
    EngineResult evaluateRules(BehaviorVector vector, BehaviorFingerprint fingerprint);
}
