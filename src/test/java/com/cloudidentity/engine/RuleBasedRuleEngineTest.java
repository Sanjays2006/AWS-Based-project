package com.cloudidentity.engine;

import com.cloudidentity.engine.impl.RuleBasedRuleEngine;
import com.cloudidentity.engine.model.BehaviorVector;
import com.cloudidentity.engine.model.EngineResult;
import com.cloudidentity.entity.BehaviorFingerprint;
import com.cloudidentity.entity.RiskRule;
import com.cloudidentity.repository.RiskRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleBasedRuleEngineTest {

    @Mock
    private RiskRuleRepository riskRuleRepository;

    @InjectMocks
    private RuleBasedRuleEngine ruleBasedRuleEngine;

    private List<RiskRule> mockRules;

    @BeforeEach
    void setUp() {
        mockRules = List.of(
                RiskRule.builder().ruleCode("MIDNIGHT_LOGIN").weight(15).isActive(true).build(),
                RiskRule.builder().ruleCode("WEEKEND_LOGIN").weight(10).isActive(true).build(),
                RiskRule.builder().ruleCode("VPN_OR_TOR").weight(35).isActive(true).build(),
                RiskRule.builder().ruleCode("PRIVILEGE_ESCALATION").weight(45).isActive(true).build(),
                RiskRule.builder().ruleCode("NEW_COUNTRY").weight(25).isActive(true).build()
        );
    }

    @Test
    @DisplayName("Should trigger midnight login and privilege escalation rules")
    void testEvaluateRules_StaticRulesTriggered() {
        when(riskRuleRepository.findAll()).thenReturn(mockRules);

        BehaviorVector vector = BehaviorVector.builder()
                .isMidnight(true)
                .loginHour(2)
                .isWeekend(false)
                .isVpn(false)
                .isTor(false)
                .apiCallName("AttachUserPolicy")
                .build();

        EngineResult result = ruleBasedRuleEngine.evaluateRules(vector, null);

        assertNotNull(result);
        assertEquals(2, result.getTriggeredRules().size());
        assertTrue(result.getTriggeredRules().stream().anyMatch(r -> r.get("ruleCode").equals("MIDNIGHT_LOGIN")));
        assertTrue(result.getTriggeredRules().stream().anyMatch(r -> r.get("ruleCode").equals("PRIVILEGE_ESCALATION")));
    }

    @Test
    @DisplayName("Should trigger NEW_COUNTRY rule when vector country is outside baseline")
    void testEvaluateRules_DynamicCountryRuleTriggered() {
        when(riskRuleRepository.findAll()).thenReturn(mockRules);

        BehaviorVector vector = BehaviorVector.builder()
                .countryCode("RU")
                .isMidnight(false)
                .isWeekend(false)
                .build();

        BehaviorFingerprint fingerprint = BehaviorFingerprint.builder()
                .typicalCountries(java.util.List.of("US", "CA", "DE"))
                .build();

        EngineResult result = ruleBasedRuleEngine.evaluateRules(vector, fingerprint);

        assertNotNull(result);
        assertEquals(1, result.getTriggeredRules().size());
        assertEquals("NEW_COUNTRY", result.getTriggeredRules().get(0).get("ruleCode"));
    }
}
