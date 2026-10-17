package com.cloudidentity.repository;

import com.cloudidentity.entity.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByUser_IdAndDeviceFingerprint(Long userId, String fingerprint);

    List<Device> findByUser_Id(Long userId);

    Page<Device> findByUser_Id(Long userId, Pageable pageable);
}
