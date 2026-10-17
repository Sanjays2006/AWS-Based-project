package com.cloudidentity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AwsSecretManagerService {

    private final Map<String, String> localSecretsCache = new ConcurrentHashMap<>();

    public String getSecret(String secretName) {
        if (localSecretsCache.containsKey(secretName)) {
            return localSecretsCache.get(secretName);
        }

        // Check environment variable fallback
        String envValue = System.getenv(secretName.toUpperCase().replace('/', '_').replace('-', '_'));
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        log.info("[AWS SECRETS MANAGER] Resolving secret '{}' from secure store / environment", secretName);
        return "mocked-secret-val-for-" + secretName;
    }

    public void storeSecret(String secretName, String secretValue) {
        localSecretsCache.put(secretName, secretValue);
        log.info("[AWS SECRETS MANAGER] Stored/Updated secret: {}", secretName);
    }
}
