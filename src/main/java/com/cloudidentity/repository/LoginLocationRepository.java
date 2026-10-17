package com.cloudidentity.repository;

import com.cloudidentity.entity.LoginLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginLocationRepository extends JpaRepository<LoginLocation, Long> {

    List<LoginLocation> findByUser_Id(Long userId);

    @Query("SELECT l.countryCode, l.countryName, l.city, l.latitude, l.longitude, COUNT(l), " +
           "CASE WHEN SUM(CASE WHEN l.isVpn = true OR l.isTor = true THEN 1 ELSE 0 END) > 0 THEN true ELSE false END " +
           "FROM LoginLocation l " +
           "WHERE l.loginAt >= :since " +
           "GROUP BY l.countryCode, l.countryName, l.city, l.latitude, l.longitude")
    List<Object[]> getGeographicMarkers(@Param("since") LocalDateTime since);
    @Query("""
           SELECT l FROM LoginLocation l
           WHERE l.user.id = :userId
           AND l.loginAt < :before
           ORDER BY l.loginAt DESC
           LIMIT 1
           """)
    java.util.Optional<LoginLocation> findLastLoginForUserBefore(
            @Param("userId") Long userId,
            @Param("before") LocalDateTime before);

    @Query("""
           SELECT l FROM LoginLocation l
           WHERE l.user.id = :userId
           ORDER BY l.loginAt DESC
           LIMIT 1
           """)
    java.util.Optional<LoginLocation> findLatestLoginForUser(@Param("userId") Long userId);
}
