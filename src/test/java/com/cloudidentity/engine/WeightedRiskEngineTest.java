package com.cloudidentity.engine;

import com.cloudidentity.config.AppProperties;
import com.cloudidentity.engine.impl.WeightedRiskEngine;
import com.cloudidentity.engine.model.EngineResult;
import com.cloudidentity.entity.IamIdentity;
import com.cloudidentity.entity.RiskScore;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.IamIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeightedRiskEngineTest {

    @Mock
    private IamIdentityRepository iamIdentityRepository;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private WeightedRiskEngine weightedRiskEngine;

    private IamIdentity testIdentity;
    private AppProperties.Risk riskProps;

    @BeforeEach
    void setUp() {
        testIdentity = IamIdentity.builder()
                .id(1L)
                .identityName("admin-user")
                .arn("arn:aws:iam::123456789012:user/admin-user")
                .build();

        riskProps = new AppProperties.Risk();
        riskProps.setCriticalThreshold(76);
        riskProps.setHighThreshold(51);
        riskProps.setMediumThreshold(26);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when identity is missing")
    void testCalculateRisk_IdentityNotFound() {
        when(iamIdentityRepository.findById(99L)).thenReturn(Optional.empty());

        EngineResult result = EngineResult.builder().build();

        assertThrows(ResourceNotFoundException.class, () -> 
            weightedRiskEngine.calculateRisk(99L, result)
        );
    }

    @Test
    @DisplayName("Should evaluate LOW risk level for score below 26")
    void testCalculateRisk_LowRisk() {
        when(iamIdentityRepository.findById(1L)).thenReturn(Optional.of(testIdentity));
        when(appProperties.getRisk()).thenReturn(riskProps);

        EngineResult result = EngineResult.builder()
                .anomalyConfidence(0.2)
                .decisionReason("Minor anomaly")
                .build();
        result.addTriggeredRule("MIDNIGHT_LOGIN", 15, "Midnight login alert");

        RiskScore score = weightedRiskEngine.calculateRisk(1L, result);

        assertNotNull(score);
        assertEquals(BigDecimal.valueOf(15.00).setScale(2), score.getScore());
        assertEquals("LOW", score.getRiskLevel());
        assertEquals(1, score.getTriggeredRules().size());
    }

    @Test
    @DisplayName("Should evaluate CRITICAL risk level and cap score at 100")
    void testCalculateRisk_CriticalRiskCappedAt100() {
        when(iamIdentityRepository.findById(1L)).thenReturn(Optional.of(testIdentity));
        when(appProperties.getRisk()).thenReturn(riskProps);

        EngineResult result = EngineResult.builder()
                .anomalyConfidence(0.95)
                .decisionReason("Multiple high severity rules triggered")
                .build();
        result.addTriggeredRule("PRIVILEGE_ESCALATION", 50, "Admin policy attached");
        result.addTriggeredRule("VPN_OR_TOR", 40, "Tor exit node detected");
        result.addTriggeredRule("MASS_DOWNLOAD", 30, "Exceeded 50GB data download");

        RiskScore score = weightedRiskEngine.calculateRisk(1L, result);

        assertNotNull(score);
        assertEquals(BigDecimal.valueOf(100.00).setScale(2), score.getScore());
        assertEquals("CRITICAL", score.getRiskLevel());
    }
}
