package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "anomalyEvent"})
@ToString(exclude = {"user", "anomalyEvent"})
public class AlertNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anomaly_event_id")
    private AnomalyEvent anomalyEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** IN_APP | SNS | EMAIL */
    @Builder.Default
    @Column(name = "channel", nullable = false, length = 50)
    private String channel = "IN_APP";

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
