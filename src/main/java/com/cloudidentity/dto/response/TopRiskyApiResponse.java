package com.cloudidentity.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopRiskyApiResponse {

    private String apiCallName;
    private String awsService;
    private Long triggerCount;
    private String riskCategory;
    private String description;
}
