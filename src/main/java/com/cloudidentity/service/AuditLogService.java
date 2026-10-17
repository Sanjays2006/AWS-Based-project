package com.cloudidentity.service;

import com.cloudidentity.dto.response.AuditLogResponse;
import com.cloudidentity.dto.response.PagedResponse;
import com.cloudidentity.entity.AuditLog;
import com.cloudidentity.entity.User;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.AuditLogRepository;
import com.cloudidentity.util.MapperUtil;
import com.cloudidentity.util.PaginationUtil;
import com.cloudidentity.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final MapperUtil mapperUtil;

    /**
     * Creates and persists an audit log record. Runs in a new transaction
     * to ensure audit logs are written even if the calling transaction rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(User user, String action, String entityType, Long entityId, 
                          String description, Map<String, Object> oldVal, Map<String, Object> newVal) {
        
        String ip = RequestContextUtil.getClientIp();
        String ua = RequestContextUtil.getUserAgent();

        AuditLog logEntry = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .ipAddress(ip)
                .userAgent(ua)
                .oldValues(oldVal)
                .newValues(newVal)
                .createdAt(LocalDateTime.now())
                .build();
                
        auditLogRepository.save(logEntry);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> listAuditLogs(Long userId, String search, 
                                                         LocalDateTime startDate, LocalDateTime endDate, 
                                                         int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortDir);
        Page<AuditLog> auditPage = auditLogRepository.filterAuditLogs(userId, search, startDate, endDate, pageable);
        
        return PaginationUtil.toPagedResponse(auditPage, 
                auditPage.getContent().stream().map(mapperUtil::toAuditLogResponse).collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(Long id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found with id: " + id));
        return mapperUtil.toAuditLogResponse(log);
    }
}
