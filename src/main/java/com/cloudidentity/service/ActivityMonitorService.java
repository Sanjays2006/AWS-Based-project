package com.cloudidentity.service;

import com.cloudidentity.dto.response.ActivityLogResponse;
import com.cloudidentity.dto.response.PagedResponse;
import com.cloudidentity.entity.ActivityLog;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.ActivityLogRepository;
import com.cloudidentity.util.MapperUtil;
import com.cloudidentity.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityMonitorService {

    private final ActivityLogRepository activityLogRepository;
    private final MapperUtil mapperUtil;

    @Transactional(readOnly = true)
    public PagedResponse<ActivityLogResponse> listLogs(Long identityId, String search, 
                                                       LocalDateTime startDate, LocalDateTime endDate, 
                                                       int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortDir);
        Page<ActivityLog> logPage = activityLogRepository.filterLogs(identityId, search, startDate, endDate, pageable);
        
        return PaginationUtil.toPagedResponse(logPage, 
                logPage.getContent().stream().map(mapperUtil::toActivityLogResponse).collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public ActivityLogResponse getLogById(Long id) {
        ActivityLog log = activityLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity log not found with id: " + id));
        return mapperUtil.toActivityLogResponse(log);
    }
}
