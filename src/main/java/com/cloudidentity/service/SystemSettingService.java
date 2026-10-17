package com.cloudidentity.service;

import com.cloudidentity.dto.response.SystemSettingResponse;
import com.cloudidentity.entity.SystemSetting;
import com.cloudidentity.entity.User;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.SystemSettingRepository;
import com.cloudidentity.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final MapperUtil mapperUtil;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getAllSettings() {
        return systemSettingRepository.findAll().stream()
                .map(mapperUtil::toSystemSettingResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SystemSettingResponse updateSetting(String key, String value, User updater) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("System setting key not found: " + key));

        String oldValue = setting.getSettingValue();
        setting.setSettingValue(value);
        setting.setUpdatedBy(updater);
        systemSettingRepository.save(setting);

        // Audit the change
        Map<String, Object> oldVal = new HashMap<>();
        oldVal.put("settingValue", oldValue);

        Map<String, Object> newVal = new HashMap<>();
        newVal.put("settingValue", value);

        auditLogService.logAction(updater, "UPDATE_SYSTEM_SETTING", "SystemSetting", setting.getId(), 
                "Updated system setting: " + key + " from '" + oldValue + "' to '" + value + "'", oldVal, newVal);

        return mapperUtil.toSystemSettingResponse(setting);
    }
}
