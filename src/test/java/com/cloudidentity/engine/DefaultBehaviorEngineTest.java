package com.cloudidentity.engine;

import com.cloudidentity.engine.impl.DefaultBehaviorEngine;
import com.cloudidentity.engine.model.BehaviorVector;
import com.cloudidentity.entity.ActivityLog;
import com.cloudidentity.entity.Device;
import com.cloudidentity.entity.IamIdentity;
import com.cloudidentity.entity.User;
import com.cloudidentity.entity.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DefaultBehaviorEngineTest {

    private DefaultBehaviorEngine behaviorEngine;

    @BeforeEach
    void setUp() {
        behaviorEngine = new DefaultBehaviorEngine();
    }

    @Test
    @DisplayName("Should extract BehaviorVector from ActivityLog successfully")
    void testExtractVectorFromActivityLog() {
        IamIdentity identity = IamIdentity.builder()
                .arn("arn:aws:iam::123456789012:user/sec-ops")
                .identityName("sec-ops")
                .build();

        ActivityLog log = ActivityLog.builder()
                .iamIdentity(identity)
                .eventTime(LocalDateTime.of(2026, 7, 21, 2, 30)) // Midnight (2 AM)
                .region("us-west-2")
                .sourceIp("8.8.8.8")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                .awsService("s3")
                .eventName("GetObject")
                .dataVolumeBytes(1048576L)
                .build();

        BehaviorVector vector = behaviorEngine.extractVector(log);

        assertNotNull(vector);
        assertEquals("arn:aws:iam::123456789012:user/sec-ops", vector.getIdentityArn());
        assertEquals("us-west-2", vector.getRegion());
        assertEquals("s3", vector.getAwsService());
        assertEquals("GetObject", vector.getApiCallName());
        assertEquals(1048576L, vector.getDataVolumeBytes());
        assertEquals(2, vector.getLoginHour());
        assertTrue(vector.isMidnight());
        assertEquals("Chrome", vector.getBrowser());
        assertEquals("Windows", vector.getOs());
    }

    @Test
    @DisplayName("Should extract BehaviorVector from UserSession successfully")
    void testExtractVectorFromUserSession() {
        User user = User.builder().username("admin").build();
        Device device = Device.builder()
                .browser("Firefox")
                .operatingSystem("Linux")
                .deviceType("DESKTOP")
                .deviceFingerprint("fp_12345")
                .build();

        UserSession session = UserSession.builder()
                .user(user)
                .device(device)
                .ipAddress("1.1.1.1")
                .loginAt(LocalDateTime.of(2026, 7, 19, 14, 0)) // Sunday 2 PM (Weekend)
                .build();

        BehaviorVector vector = behaviorEngine.extractVector(session);

        assertNotNull(vector);
        assertEquals("admin", vector.getIdentityArn());
        assertEquals("Firefox", vector.getBrowser());
        assertEquals("Linux", vector.getOs());
        assertEquals("fp_12345", vector.getDeviceFingerprint());
        assertTrue(vector.isWeekend());
        assertFalse(vector.isMidnight());
    }
}
