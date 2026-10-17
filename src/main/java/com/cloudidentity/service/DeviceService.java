package com.cloudidentity.service;

import com.cloudidentity.dto.response.DeviceResponse;
import com.cloudidentity.dto.response.PagedResponse;
import com.cloudidentity.entity.Device;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.DeviceRepository;
import com.cloudidentity.util.MapperUtil;
import com.cloudidentity.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final MapperUtil mapperUtil;

    @Transactional(readOnly = true)
    public PagedResponse<DeviceResponse> listUserDevices(Long userId, int page, int size) {
        Pageable pageable = PaginationUtil.createPageable(page, size, "lastSeenAt", "DESC");
        Page<Device> devicePage = deviceRepository.findByUser_Id(userId, pageable);
        
        return PaginationUtil.toPagedResponse(devicePage, 
                devicePage.getContent().stream().map(mapperUtil::toDeviceResponse).collect(Collectors.toList()));
    }

    @Transactional
    public void trustDevice(Long deviceId, Long userId, boolean isTrusted) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        if (!device.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to modify this device");
        }

        device.setIsTrusted(isTrusted);
        deviceRepository.save(device);
    }
}
