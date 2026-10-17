package com.cloudidentity.engine.model;

/**
 * Constants for known risk rule codes. These match the ruleCode values
 * seeded into the risk_rules table via DataInitializer.
 */
public final class RiskFactors {

    private RiskFactors() {}

    // ── Time-Based ────────────────────────────────────────────────
    public static final String MIDNIGHT_LOGIN       = "MIDNIGHT_LOGIN";
    public static final String WEEKEND_LOGIN        = "WEEKEND_LOGIN";

    // ── Network / Location ─────────────────────────────────────────
    public static final String VPN_OR_TOR           = "VPN_OR_TOR";
    public static final String NEW_COUNTRY          = "NEW_COUNTRY";
    public static final String NEW_REGION           = "NEW_REGION";

    // ── Device / Browser ───────────────────────────────────────────
    public static final String NEW_DEVICE           = "NEW_DEVICE";

    // ── Service / API ──────────────────────────────────────────────
    public static final String UNUSUAL_SERVICE      = "UNUSUAL_SERVICE";
    public static final String PRIVILEGE_ESCALATION = "PRIVILEGE_ESCALATION";

    // ── Volume / Data ──────────────────────────────────────────────
    public static final String MASS_DOWNLOAD        = "MASS_DOWNLOAD";

    // ── Authentication ─────────────────────────────────────────────
    public static final String MULTIPLE_FAILED_LOGINS = "MULTIPLE_FAILED_LOGINS";

    // ── Travel / Location Anomaly ──────────────────────────────────
    public static final String IMPOSSIBLE_TRAVEL      = "IMPOSSIBLE_TRAVEL";

    // ── IAM Policy ─────────────────────────────────────────────────
    public static final String IAM_POLICY_CHANGE      = "IAM_POLICY_CHANGE";
}
