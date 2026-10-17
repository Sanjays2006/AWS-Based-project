package com.cloudidentity.scheduler;

import com.cloudidentity.config.AppProperties;
import com.cloudidentity.dto.response.IngestionResultResponse;
import com.cloudidentity.service.AwsS3CloudTrailReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3CloudTrailIngestionSchedulerTest {

    @Mock
    private AppProperties appProperties;

    @Mock
    private AwsS3CloudTrailReader awsS3CloudTrailReader;

    @InjectMocks
    private S3CloudTrailIngestionScheduler scheduler;

    private AppProperties.Scheduler schedulerProps;

    @BeforeEach
    void setUp() {
        schedulerProps = new AppProperties.Scheduler();
    }

    @Test
    @DisplayName("Scheduled poll does nothing when S3 scheduler is disabled")
    void testPoll_DisabledSkips() {
        schedulerProps.setS3Enabled(false);
        when(appProperties.getScheduler()).thenReturn(schedulerProps);

        scheduler.pollAndIngestFromS3();

        verify(awsS3CloudTrailReader, never()).ingestRecentEvents();
    }

    @Test
    @DisplayName("Scheduled poll delegates to the shared reader when enabled")
    void testPoll_EnabledDelegates() {
        schedulerProps.setS3Enabled(true);
        when(appProperties.getScheduler()).thenReturn(schedulerProps);
        when(awsS3CloudTrailReader.ingestRecentEvents())
                .thenReturn(IngestionResultResponse.builder()
                        .source("S3").sourceAvailable(true).message("ok").build());

        scheduler.pollAndIngestFromS3();

        verify(awsS3CloudTrailReader, times(1)).ingestRecentEvents();
    }

    @Test
    @DisplayName("Manual trigger always delegates to the shared reader regardless of enable flag")
    void testManualTrigger_AlwaysRuns() {
        when(awsS3CloudTrailReader.ingestRecentEvents())
                .thenReturn(IngestionResultResponse.builder()
                        .source("S3").sourceAvailable(true).ingested(5).message("ok").build());

        IngestionResultResponse result = scheduler.triggerManually();

        assertEquals(5, result.getIngested());
        verify(awsS3CloudTrailReader).ingestRecentEvents();
    }

    @Test
    @DisplayName("Manual trigger returns a failure report (not an exception) when the reader throws")
    void testManualTrigger_HandlesException() {
        when(awsS3CloudTrailReader.ingestRecentEvents())
                .thenThrow(new RuntimeException("S3 unreachable"));

        IngestionResultResponse result = scheduler.triggerManually();

        assertFalse(result.isSourceAvailable());
        assertEquals("S3", result.getSource());
    }
}
