package com.cloudidentity.repository;

import com.cloudidentity.entity.IamIdentityVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IamIdentityVersionRepository extends JpaRepository<IamIdentityVersion, Long> {
    List<IamIdentityVersion> findByIamIdentity_IdOrderByVersionNumberDesc(Long iamIdentityId);
}
