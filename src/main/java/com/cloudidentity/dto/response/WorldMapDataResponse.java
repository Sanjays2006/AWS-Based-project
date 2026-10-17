package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldMapDataResponse {
    private List<LocationMarker> markers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationMarker {
        private String countryCode;
        private String countryName;
        private String city;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private long count;
        private boolean hasAnomaly;
    }
}
