package com.cloudidentity.engine;

import com.cloudidentity.engine.model.BehaviorVector;
import com.cloudidentity.entity.ActivityLog;
import com.cloudidentity.entity.UserSession;

public interface BehaviorEngine {
    
    /**
     * Converts a raw ActivityLog (CloudTrail event) into a normalized BehaviorVector.
     */
    BehaviorVector extractVector(ActivityLog activityLog);

    /**
     * Converts a UserSession (web console login) into a normalized BehaviorVector.
     */
    BehaviorVector extractVector(UserSession session);
}
