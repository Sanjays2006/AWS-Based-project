package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "report_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"generatedBy"})
@ToString(exclude = {"generatedBy", "parameters"})
public class ReportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** DAILY_RISK | ANOMALY_TRENDS | IDENTITY_ACTIVITY */
    @Column(name = "report_type", nullable = false, length = 100)
    private String reportType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private Map<String, Object> parameters;

    @Builder.Default
    @Column(name = "record_count")
    private Integer recordCount = 0;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}
