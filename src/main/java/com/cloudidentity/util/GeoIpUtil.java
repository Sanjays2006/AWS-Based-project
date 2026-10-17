package com.cloudidentity.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;

@Component
public class GeoIpUtil {

    private static final Random RANDOM = new Random();

    @Getter
    @AllArgsConstructor
    public static class GeoLocation {
        private final String countryCode;
        private final String countryName;
        private final String city;
        private final BigDecimal latitude;
        private final BigDecimal longitude;
        private final boolean isVpn;
        private final boolean isTor;
    }

    private static final Map<String, GeoLocation> KNOWN_MOCKS = Map.of(
        "127.0.0.1", new GeoLocation("US", "United States", "Localhost", new BigDecimal("37.7749"), new BigDecimal("-122.4194"), false, false),
        "0:0:0:0:0:0:0:1", new GeoLocation("US", "United States", "Localhost", new BigDecimal("37.7749"), new BigDecimal("-122.4194"), false, false),
        "10.0.0.1", new GeoLocation("IN", "India", "Chennai", new BigDecimal("13.0827"), new BigDecimal("80.2707"), false, false),
        "192.168.1.1", new GeoLocation("US", "United States", "Seattle", new BigDecimal("47.6062"), new BigDecimal("-122.3321"), false, false)
    );

    /**
     * Resolves location information from an IP address.
     * Uses static mocks for development/local IPs, and deterministic randomization for other public IPs.
     */
    public static GeoLocation resolveIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return KNOWN_MOCKS.get("127.0.0.1");
        }

        if (KNOWN_MOCKS.containsKey(ipAddress)) {
            return KNOWN_MOCKS.get(ipAddress);
        }

        // Determine country/city deterministically from hashcode of IP
        int hash = Math.abs(ipAddress.hashCode());
        boolean isVpn = hash % 23 == 0;
        boolean isTor = hash % 51 == 0;

        int countrySelector = hash % 5;
        return switch (countrySelector) {
            case 0 -> new GeoLocation("IN", "India", "Mumbai", new BigDecimal("19.0760"), new BigDecimal("72.8777"), isVpn, isTor);
            case 1 -> new GeoLocation("US", "United States", "San Francisco", new BigDecimal("37.7749"), new BigDecimal("-122.4194"), isVpn, isTor);
            case 2 -> new GeoLocation("GB", "United Kingdom", "London", new BigDecimal("51.5074"), new BigDecimal("-0.1278"), isVpn, isTor);
            case 3 -> new GeoLocation("SG", "Singapore", "Singapore", new BigDecimal("1.3521"), new BigDecimal("103.8198"), isVpn, isTor);
            default -> new GeoLocation("DE", "Germany", "Frankfurt", new BigDecimal("50.1109"), new BigDecimal("8.6821"), isVpn, isTor);
        };
    }
}
