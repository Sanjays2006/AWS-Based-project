package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapDataResponse {
    
    /** Each point has: dayOfWeek (1-7), hourOfDay (0-23), count */
    private List<HeatmapPoint> points;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HeatmapPoint {
        private int dayOfWeek; // 1 = Monday, 7 = Sunday
        private int hourOfDay; // 0-23
        private long value;
    }
}
