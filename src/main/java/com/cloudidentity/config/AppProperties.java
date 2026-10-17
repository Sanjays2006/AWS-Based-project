package com.cloudidentity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Strongly-typed binding for all custom 'app.*' properties in application.yml.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private Risk risk = new Risk();
    private Anomaly anomaly = new Anomaly();
    private Scheduler scheduler = new Scheduler();

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:8080";
    }

    @Data
    public static class Risk {
        private int criticalThreshold = 76;
        private int highThreshold = 51;
        private int mediumThreshold = 26;
    }

    @Data
    public static class Anomaly {
        private int baselineDays = 30;
        private boolean detectionEnabled = true;
    }

    @Data
    public static class Scheduler {
        private boolean cloudtrailEnabled = false;
        private boolean s3Enabled = false;
        private boolean fingerprintEnabled = false;
        private boolean reportEnabled = false;
        private boolean baselineEnabled = false;
    }
}
