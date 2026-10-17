package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "activity_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"iamIdentity", "user"})
@ToString(exclude = {"iamIdentity", "user", "requestParameters", "responseElements", "rawEvent"})
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CloudTrail event UUID — unique per event, used for de-duplication. */
    @Column(name = "event_id", length = 100, unique = true)
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_identity_id")
    private IamIdentity iamIdentity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "event_name", length = 200)
    private String eventName;

    @Column(name = "event_source", length = 200)
    private String eventSource;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(name = "source_ip", length = 50)
    private String sourceIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "aws_service", length = 100)
    private String awsService;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_parameters", columnDefinition = "jsonb")
    private Map<String, Object> requestParameters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_elements", columnDefinition = "jsonb")
    private Map<String, Object> responseElements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_event", columnDefinition = "jsonb")
    private Map<String, Object> rawEvent;

    @Builder.Default
    @Column(name = "data_volume_bytes", nullable = false)
    private Long dataVolumeBytes = 0L;

    @CreationTimestamp
    @Column(name = "ingested_at", nullable = false, updatable = false)
    private LocalDateTime ingestedAt;
}
