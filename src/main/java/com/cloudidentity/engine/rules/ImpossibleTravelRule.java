package com.cloudidentity.engine.rules;

import com.cloudidentity.engine.model.BehaviorVector;
import com.cloudidentity.engine.model.EngineResult;
import com.cloudidentity.entity.LoginLocation;
import com.cloudidentity.entity.RiskRule;
import com.cloudidentity.repository.LoginLocationRepository;
import com.cloudidentity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Detects Impossible Travel — when the same user appears in two
 * geographically distant locations within a time window too short
 * to travel between them (based on max commercial aircraft speed).
 *
 * Haversine formula is used to calculate great-circle distance.
 * Threshold: distance_km / hours_elapsed > 900 km/h is impossible.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImpossibleTravelRule {

    /** Max realistic travel speed in km/h (faster than any commercial airline). */
    private static final double MAX_TRAVEL_SPEED_KMH = 900.0;

    /** Only flag if prior login was at least 50km away (filters same-city logins). */
    private static final double MIN_DISTANCE_KM = 50.0;

    /** Only check logins within the past 6 hours. */
    private static final long LOOKBACK_HOURS = 6;

    private final LoginLocationRepository loginLocationRepository;
    private final UserRepository userRepository;

    /**
     * Evaluates impossible travel for the given event vector.
     *
     * @param vector      Current event's behavior vector (must have lat/lon from GeoIpUtil)
     * @param currentLat  Latitude of the current login
     * @param currentLon  Longitude of the current login
     * @param currentTime Event timestamp
     * @param userId      User ID to query prior login history
     * @param rule        DB-configured rule for weight and code
     * @param result      EngineResult to append to if triggered
     */
    public void evaluate(BehaviorVector vector,
                         double currentLat, double currentLon,
                         LocalDateTime currentTime,
                         Long userId,
                         RiskRule rule,
                         EngineResult result) {

        if (userId == null || rule == null) return;

        LocalDateTime lookbackFrom = currentTime.minus(LOOKBACK_HOURS, ChronoUnit.HOURS);

        Optional<LoginLocation> priorLoginOpt =
                loginLocationRepository.findLastLoginForUserBefore(userId, currentTime);

        if (priorLoginOpt.isEmpty()) {
            log.debug("[ImpossibleTravel] No prior login found for userId={}", userId);
            return;
        }

        LoginLocation prior = priorLoginOpt.get();

        // Must be within our lookback window
        if (prior.getLoginAt() == null || prior.getLoginAt().isBefore(lookbackFrom)) {
            return;
        }

        // Need coordinates
        if (prior.getLatitude() == null || prior.getLongitude() == null) {
            return;
        }

        double priorLat = prior.getLatitude().doubleValue();
        double priorLon = prior.getLongitude().doubleValue();

        double distanceKm = haversineKm(priorLat, priorLon, currentLat, currentLon);

        if (distanceKm < MIN_DISTANCE_KM) {
            return; // Same general area — not suspicious
        }

        long minutesElapsed = ChronoUnit.MINUTES.between(prior.getLoginAt(), currentTime);
        if (minutesElapsed <= 0) minutesElapsed = 1;

        double hoursElapsed = minutesElapsed / 60.0;
        double impliedSpeedKmh = distanceKm / hoursElapsed;

        if (impliedSpeedKmh > MAX_TRAVEL_SPEED_KMH) {
            String detail = String.format(
                    "Impossible Travel detected: %.0f km between %s (%s) and current location in %.0f minutes " +
                    "(implied speed: %.0f km/h — physically impossible).",
                    distanceKm,
                    prior.getCity() != null ? prior.getCity() : prior.getCountryCode(),
                    prior.getLoginAt(),
                    (double) minutesElapsed,
                    impliedSpeedKmh);

            result.addTriggeredRule(rule.getRuleCode(), rule.getWeight(), detail);
            log.warn("[ImpossibleTravel] TRIGGERED for userId={}: {}", userId, detail);
        }
    }

    /**
     * Haversine formula — returns distance in kilometers between two lat/lon points.
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
