package com.cloudidentity.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class DeviceFingerprintUtil {

    private DeviceFingerprintUtil() {}

    /**
     * Extracts operating system from User-Agent string.
     */
    public static String parseOS(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("macintosh") || ua.contains("mac os")) return "macOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        if (ua.contains("linux")) return "Linux";
        
        return "Unknown";
    }

    /**
     * Extracts browser name from User-Agent string.
     */
    public static String parseBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("edg")) return "Edge";
        if (ua.contains("chrome") && !ua.contains("chromium")) return "Chrome";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("opr") || ua.contains("opera")) return "Opera";
        
        return "Unknown";
    }

    /**
     * Identifies device type: Mobile, Tablet, or Desktop.
     */
    public static String parseDeviceType(String userAgent) {
        if (userAgent == null) return "DESKTOP";
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("ipad") || (ua.contains("android") && !ua.contains("mobile"))) return "TABLET";
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")) return "MOBILE";
        
        return "DESKTOP";
    }

    /**
     * Computes a SHA-256 hash of user agent and IP/other components to form a stable fingerprint.
     */
    public static String generateFingerprint(String userAgent, String userEmail) {
        return generateFingerprint(userAgent, userEmail, "1920x1080", "UTC");
    }

    /**
     * Enriched device fingerprint calculation taking browser, OS, screen resolution and timezone.
     */
    public static String generateFingerprint(String userAgent, String userEmail, String screenResolution, String timezone) {
        if (userAgent == null) userAgent = "UnknownUA";
        if (userEmail == null) userEmail = "UnknownUser";
        if (screenResolution == null) screenResolution = "UnknownRes";
        if (timezone == null) timezone = "UnknownTZ";
        
        try {
            String rawString = userAgent + "|" + userEmail + "|" + screenResolution + "|" + timezone;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawString.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback in case SHA-256 is somehow unavailable
            return String.valueOf((userAgent + userEmail + screenResolution + timezone).hashCode());
        }
    }
}
