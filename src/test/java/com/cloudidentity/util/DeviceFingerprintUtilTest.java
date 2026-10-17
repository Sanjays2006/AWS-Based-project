package com.cloudidentity.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceFingerprintUtilTest {

    @Test
    @DisplayName("Should correctly parse Chrome browser from User-Agent string")
    void testParseBrowser_Chrome() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        String browser = DeviceFingerprintUtil.parseBrowser(ua);
        assertEquals("Chrome", browser);
    }

    @Test
    @DisplayName("Should correctly parse Firefox browser from User-Agent string")
    void testParseBrowser_Firefox() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0";
        String browser = DeviceFingerprintUtil.parseBrowser(ua);
        assertEquals("Firefox", browser);
    }

    @Test
    @DisplayName("Should correctly parse Windows OS from User-Agent string")
    void testParseOS_Windows() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        String os = DeviceFingerprintUtil.parseOS(ua);
        assertEquals("Windows", os);
    }

    @Test
    @DisplayName("Should generate deterministic SHA-256 device fingerprint")
    void testGenerateFingerprint() {
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)";
        String identityName = "alice";

        String fp1 = DeviceFingerprintUtil.generateFingerprint(ua, identityName);
        String fp2 = DeviceFingerprintUtil.generateFingerprint(ua, identityName);

        assertNotNull(fp1);
        assertEquals(64, fp1.length(), "SHA-256 hex string should be 64 characters long");
        assertEquals(fp1, fp2, "Device fingerprint generation should be deterministic");
    }
}
