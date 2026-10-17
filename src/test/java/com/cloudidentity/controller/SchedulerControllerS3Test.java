package com.cloudidentity.controller;

import com.cloudidentity.dto.response.IngestionResultResponse;
import com.cloudidentity.scheduler.BaselineRetrainingScheduler;
import com.cloudidentity.scheduler.CloudTrailIngestionScheduler;
import com.cloudidentity.scheduler.DailyReportScheduler;
import com.cloudidentity.scheduler.FingerprintRecomputeScheduler;
import com.cloudidentity.scheduler.S3CloudTrailIngestionScheduler;
import com.cloudidentity.service.AwsGuardDutyService;
import com.cloudidentity.service.AwsIamSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SchedulerControllerS3Test {

    private MockMvc mockMvc;

    @Mock private CloudTrailIngestionScheduler cloudTrailIngestionScheduler;
    @Mock private FingerprintRecomputeScheduler fingerprintRecomputeScheduler;
    @Mock private DailyReportScheduler dailyReportScheduler;
    @Mock private BaselineRetrainingScheduler baselineRetrainingScheduler;
    @Mock private AwsIamSyncService awsIamSyncService;
    @Mock private AwsGuardDutyService awsGuardDutyService;
    @Mock private S3CloudTrailIngestionScheduler s3CloudTrailIngestionScheduler;

    @InjectMocks
    private SchedulerController schedulerController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(schedulerController).build();
    }

    @Test
    @DisplayName("POST /aws/s3-cloudtrail/trigger returns the ingestion status report")
    void testTriggerS3Ingestion_ReturnsStatus() throws Exception {
        IngestionResultResponse report = IngestionResultResponse.builder()
                .source("S3")
                .scanned(10)
                .ingested(7)
                .duplicates(2)
                .evaluated(7)
                .failed(1)
                .objectsRead(3)
                .sourceAvailable(true)
                .message("S3 ingestion complete")
                .build();

        when(s3CloudTrailIngestionScheduler.triggerManually()).thenReturn(report);

        mockMvc.perform(post("/api/v1/scheduler/aws/s3-cloudtrail/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("S3"))
                .andExpect(jsonPath("$.scanned").value(10))
                .andExpect(jsonPath("$.ingested").value(7))
                .andExpect(jsonPath("$.duplicates").value(2))
                .andExpect(jsonPath("$.evaluated").value(7))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.objectsRead").value(3))
                .andExpect(jsonPath("$.sourceAvailable").value(true));

        verify(s3CloudTrailIngestionScheduler).triggerManually();
    }

    @Test
    @DisplayName("POST /aws/s3-cloudtrail/trigger reports sourceAvailable=false when bucket unconfigured")
    void testTriggerS3Ingestion_BucketUnconfigured() throws Exception {
        IngestionResultResponse report = IngestionResultResponse.builder()
                .source("S3")
                .sourceAvailable(false)
                .message("CloudTrail S3 bucket is not configured (aws.cloudtrail.s3-bucket).")
                .build();

        when(s3CloudTrailIngestionScheduler.triggerManually()).thenReturn(report);

        mockMvc.perform(post("/api/v1/scheduler/aws/s3-cloudtrail/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceAvailable").value(false))
                .andExpect(jsonPath("$.ingested").value(0));
    }
}
