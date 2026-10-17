package com.cloudidentity.service;

import com.cloudidentity.dto.response.IngestionResultResponse;
import com.cloudidentity.entity.ActivityLog;
import com.cloudidentity.entity.IamIdentity;
import com.cloudidentity.repository.ActivityLogRepository;
import com.cloudidentity.repository.IamIdentityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudTrailEventIngestorTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private IamIdentityRepository iamIdentityRepository;

    @Mock
    private ThreatDetectionService threatDetectionService;

    @InjectMocks
    private CloudTrailEventIngestor ingestor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode sampleEvent;

    @BeforeEach
    void setUp() throws Exception {
        String json = """
                {
                  "eventID": "11111111-2222-3333-4444-555555555555",
                  "eventName": "ConsoleLogin",
                  "eventSource": "signin.amazonaws.com",
                  "eventTime": "2026-10-17T12:34:56Z",
                  "sourceIPAddress": "203.0.113.10",
                  "userAgent": "Mozilla/5.0",
                  "awsRegion": "us-east-1",
                  "userIdentity": {
                    "type": "IAMUser",
                    "arn": "arn:aws:iam::123456789012:user/alice",
                    "userName": "alice",
                    "accountId": "123456789012"
                  }
                }
                """;
        sampleEvent = objectMapper.readTree(json);
    }

    @Test
    @DisplayName("Should persist and evaluate a new CloudTrail event")
    void testIngestRecord_NewEvent() {
        when(activityLogRepository.existsByEventId("11111111-2222-3333-4444-555555555555"))
                .thenReturn(false);
        when(iamIdentityRepository.findByArn("arn:aws:iam::123456789012:user/alice"))
                .thenReturn(Optional.of(IamIdentity.builder().id(1L)
                        .arn("arn:aws:iam::123456789012:user/alice").build()));
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        IngestionResultResponse result = IngestionResultResponse.builder().source("TEST").build();
        ingestor.ingestRecord(sampleEvent, result);

        assertEquals(1, result.getScanned());
        assertEquals(1, result.getIngested());
        assertEquals(0, result.getDuplicates());
        assertEquals(1, result.getEvaluated());
        assertEquals(0, result.getFailed());

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        ActivityLog saved = captor.getValue();
        assertEquals("11111111-2222-3333-4444-555555555555", saved.getEventId());
        assertEquals("ConsoleLogin", saved.getEventName());
        assertEquals("203.0.113.10", saved.getSourceIp());
        assertEquals("us-east-1", saved.getRegion());
        verify(threatDetectionService).evaluateActivityLog(saved);
    }

    @Test
    @DisplayName("Should skip a duplicate event and not persist or evaluate")
    void testIngestRecord_Duplicate() {
        when(activityLogRepository.existsByEventId("11111111-2222-3333-4444-555555555555"))
                .thenReturn(true);

        IngestionResultResponse result = IngestionResultResponse.builder().source("TEST").build();
        ingestor.ingestRecord(sampleEvent, result);

        assertEquals(1, result.getScanned());
        assertEquals(0, result.getIngested());
        assertEquals(1, result.getDuplicates());
        verify(activityLogRepository, never()).save(any());
        verify(threatDetectionService, never()).evaluateActivityLog(any());
    }

    @Test
    @DisplayName("Should create a new IAM identity when the event ARN is unseen")
    void testIngestRecord_CreatesIdentity() {
        when(activityLogRepository.existsByEventId(any())).thenReturn(false);
        when(iamIdentityRepository.findByArn(any())).thenReturn(Optional.empty());
        when(iamIdentityRepository.save(any(IamIdentity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        IngestionResultResponse result = IngestionResultResponse.builder().source("TEST").build();
        ingestor.ingestRecord(sampleEvent, result);

        verify(iamIdentityRepository).save(any(IamIdentity.class));
        assertEquals(1, result.getIngested());
    }

    @Test
    @DisplayName("Should record a failure when persistence throws")
    void testIngestRecord_PersistFailure() {
        when(activityLogRepository.existsByEventId(any())).thenReturn(false);
        when(iamIdentityRepository.findByArn(any()))
                .thenReturn(Optional.of(IamIdentity.builder().id(1L).build()));
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenThrow(new RuntimeException("DB down"));

        IngestionResultResponse result = IngestionResultResponse.builder().source("TEST").build();
        ingestor.ingestRecord(sampleEvent, result);

        assertEquals(1, result.getScanned());
        assertEquals(0, result.getIngested());
        assertEquals(1, result.getFailed());
        assertFalse(result.getErrors().isEmpty());
    }
}
