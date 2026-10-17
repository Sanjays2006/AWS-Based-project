package com.cloudidentity.repository;

import com.cloudidentity.entity.IamIdentity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IamIdentityRepository extends JpaRepository<IamIdentity, Long> {

    Optional<IamIdentity> findByArn(String arn);

    @Query("SELECT i FROM IamIdentity i WHERE " +
           "(CAST(:search AS string) IS NULL OR LOWER(i.arn) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.identityName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.accountId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<IamIdentity> searchIdentities(@Param("search") String search, Pageable pageable);

    long countByIsMonitoredTrue();
}
