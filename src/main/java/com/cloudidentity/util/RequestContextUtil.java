package com.cloudidentity.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestContextUtil {

    private RequestContextUtil() {}

    /**
     * Extracts IP Address from current HTTP context. Handles reverse proxies (X-Forwarded-For).
     */
    public static String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "127.0.0.1";
        }
        
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // Handle comma-separated lists of proxy ips
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Extracts User-Agent header from HTTP context.
     */
    public static String getUserAgent() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "Unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "Unknown";
    }
}
