package com.cloudidentity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class BehavioralDriftResponse {

    private LocalDate snapshotDate;
    private Integer apiCallCount;
    private Integer loginCount;
    private Integer uniqueRegions;
    private Integer privilegeEscalations;
    private Long dataDownloadBytes;
    private BigDecimal riskScoreEod;
}
