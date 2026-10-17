package com.cloudidentity.engine.impl;

import com.cloudidentity.engine.RuleEngine;
import com.cloudidentity.engine.model.BehaviorVector;
import com.cloudidentity.engine.model.EngineResult;
import com.cloudidentity.engine.model.RiskFactors;
import com.cloudidentity.entity.BehaviorFingerprint;
import com.cloudidentity.entity.RiskRule;
import com.cloudidentity.repository.RiskRuleRepository;
import com.cloudidentity.engine.rules.ImpossibleTravelRule;
import com.cloudidentity.util.GeoIpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleBasedRuleEngine implements RuleEngine {

    private final RiskRuleRepository riskRuleRepository;
    private final ImpossibleTravelRule impossibleTravelRule;

    @Override
    public EngineResult evaluateRules(BehaviorVector vector, BehaviorFingerprint fingerprint) {
        EngineResult result = EngineResult.builder().build();

        // 1. Fetch rules configuration from DB
        Map<String, RiskRule> rulesMap = riskRuleRepository.findAll().stream()
                .filter(RiskRule::getIsActive)
                .collect(Collectors.toMap(RiskRule::getRuleCode, r -> r));

        // 2. Perform static evaluations
        if (vector.isMidnight() && rulesMap.containsKey(RiskFactors.MIDNIGHT_LOGIN)) {
            RiskRule rule = rulesMap.get(RiskFactors.MIDNIGHT_LOGIN);
            result.addTriggeredRule(rule.getRuleCode(), rule.getWeight(), "Activity at " + vector.getLoginHour() + ":00 local time falls in midnight window.");
        }

        if (vector.isWeekend() && rulesMap.containsKey(RiskFactors.WEEKEND_LOGIN)) {
            RiskRule rule = rulesMap.get(RiskFactors.WEEKEND_LOGIN);
            result.addTriggeredRule(rule.getRuleCode(), rule.getWeight(), "Activity occurred on weekend.");
        }

        if (vector.isVpn() || vector.isTor()) {
            if (rulesMap.containsKey(RiskFactors.VPN_OR_TOR)) {
                RiskRule rule = rulesMap.get(RiskFactors.VPN_OR_TOR);
                String detail = vector.isTor() ? "Source IP is a TOR exit node." : "Source IP is a VPN node.";
                result.addTriggeredRule(rule.getRuleCode(), rule.getWeight(), detail);
            }
        }

        // Detect privilege escalation API patterns
        if (isPrivilegeEscalationAction(vector.getApiCallName()) && rulesMap.containsKey(RiskFactors.PRIVILEGE_ESCALATION)) {
            RiskRule rule = rulesMap.get(RiskFactors.PRIVILEGE_ESCALATION);
            result.addTriggeredRule(rule.getRuleCode(), rule.getWeight(), "Privilege escalation API call: " + vector.getApiCallName());
        }

        // Detect general IAM policy changes
        if (isIamPolicyChangeAction(vector.getApiCallName()) && rulesMap.containsKey(RiskFactors.IAM_POLICY_CHANGE)) {
            RiskRule rule = rulesMap.get(RiskFactors.IAM_POLICY_CHANGE);
            result.addTriggeredRule(rule.getRuleCode(), rule.getWeight(), "IAM Policy change API call: " + vector.getApiCallName());
        }

        // Check Impossible Travel
        if (rulesMap.containsKey(RiskFactors.IMPOSSIBLE_TRAVEL) && vector.getSourceIp() != null) {
            GeoIpUtil.GeoLocation geo = GeoIpUtil.resolveIp(vector.getSourceIp());
            if (geo.getLatitude() != null && geo.getLongitude() != null && 
                BigDecimal.ZERO.compareTo(geo.getLatitude()) != 0) {
                Long userId = fingerprint != null && fingerprint.getIamIdentity() != null 
                        ? fingerprint.getIamIdentity().getId() : null;
                impossibleTravelRule.evaluate(
                        vector, 
                        geo.getLatitude().doubleValue(), 
                        geo.getLongitude().doubleValue(), 
                        vector.getEventTime(), 
                        userId, 
                        rulesMap.get(RiskFactors.IMPOSSIBLE_TRAVEL), 
                        result
                );
            }
        }

        // 3. Perform dynamic comparisons if baseline exists
        if (fingerprint != null) {
            
            // Check country
            if (fingerprint.getTypicalCountries() != null && !fingerprint.getTypicalCountries().isEmpty()) {
                if (!fingerprint.getTypicalCountries().contains(vector.getCountryCode())) {
                    if (rulesMap.containsKey(RiskFactors.NEW_COUNTRY)) {
                        RiskRule rule = rulesMap.get(RiskFactors.NEW_COUNTRY);
                        result.addTriggeredRule(rule.getRuleCode(), rule.getWeight(), "Activity from country '" + vector.getCountryCode() + "' is outside known baseline list " + fingerprint.getTypicalCountries() + ".");
                    }
                }
            }

            // Check region
            if (fingerprint.getTypicalRegions() != null && !fingerprint.getTypicalRegions().isEmpty()) {
                if (!fingerprint.getTypicalRegions().contains(vector.getRegion())) {
                    if (rulesMap.containsKey(RiskFactors.NEW_REGION)) {
                        RiskRule rule = rulesMap.get(RiskFactors.NEW_REGION);
                        result.addTriggeredRule(rule.getRuleCode(), rule.getWeight(), "AWS region '" + vector.getRegion() + "' has never been used by this identity.");
                    }
                }
            }

            // Check service
            if (fingerprint.getTypicalServices() != null && !fingerprint.getTypicalServices().isEmpty()) {
                if (!fingerprint.getTypicalServices().contains(vector.getAwsService())) {
                    if (rulesMap.containsKey(RiskFactors.UNUSUAL_SERVICE)) {
                        RiskRule rule = rulesMap.get(RiskFactors.UNUSUAL_SERVICE);
                        result.addTriggeredRule(rule.getRuleCode(), rule.getWeight(), "AWS Service '" + vector.getAwsService() + "' has not been used typically.");
                    }
                }
            }

            // Check device fingerprint
            if (fingerprint.getTypicalDeviceFingerprints() != null && !fingerprint.getTypicalDeviceFingerprints().isEmpty()) {
                if (!fingerprint.getTypicalDeviceFingerprints().contains(vector.getDeviceFingerprint())) {
                    if (rulesMap.containsKey(RiskFactors.NEW_DEVICE)) {
                        RiskRule rule = rulesMap.get(RiskFactors.NEW_DEVICE);
                        result.addTriggeredRule(rule.getRuleCode(), rule.getWeight(), "Unrecognized browser/OS device configuration.");
                    }
                }
            }

            // Check mass download volume
            if (fingerprint.getMaxDataDownloadBytes() != null && fingerprint.getMaxDataDownloadBytes() > 0) {
                if (vector.getDataVolumeBytes() > fingerprint.getMaxDataDownloadBytes()) {
                    if (rulesMap.containsKey(RiskFactors.MASS_DOWNLOAD)) {
                        RiskRule rule = rulesMap.get(RiskFactors.MASS_DOWNLOAD);
                        result.addTriggeredRule(rule.getRuleCode(), rule.getWeight(), 
                                "Activity volume " + vector.getDataVolumeBytes() + " bytes exceeded the learned maximum download limit " + fingerprint.getMaxDataDownloadBytes() + " bytes.");
                    }
                }
            }
        }

        // Calculate confidence
        double triggeredCount = result.getTriggeredRules().size();
        double possibleRules = Math.max(1, rulesMap.size());
        result.setAnomalyConfidence(Math.min(1.0, triggeredCount / 5.0)); // simple confidence ceiling at 5 alerts
        result.setDecisionReason(triggeredCount > 0 
                ? triggeredCount + " security rule triggers fired" 
                : "Activity matches standard baseline configuration");

        return result;
    }

    private boolean isPrivilegeEscalationAction(String action) {
        if (action == null) return false;
        String name = action.toLowerCase();
        return name.contains("attachuserpolicy") || 
               name.contains("attachrolepolicy") || 
               name.contains("attachgrouppolicy") || 
               name.contains("createpolicyversion") || 
               name.contains("setdefaultpolicyversion") || 
               name.contains("putuserpolicy") || 
               name.contains("putrolepolicy") || 
               name.contains("putgrouppolicy") || 
               name.contains("updateassumerolepolicy") || 
               name.contains("createloginprofile") || 
               name.contains("updateloginprofile") || 
               name.contains("createaccesskey");
    }

    private boolean isIamPolicyChangeAction(String action) {
        if (action == null) return false;
        String name = action.toLowerCase();
        return name.contains("createpolicy") || 
               name.contains("deletepolicy") || 
               name.contains("detachuserpolicy") || 
               name.contains("detachrolepolicy") || 
               name.contains("detachgrouppolicy") || 
               name.contains("deleteuserpolicy") || 
               name.contains("deleterolepolicy") || 
               name.contains("deletegrouppolicy");
    }
}
