package com.cloudidentity.repository;

import com.cloudidentity.entity.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionToken(String token);

    List<UserSession> findByUser_IdAndIsActiveTrue(Long userId);

    @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId ORDER BY s.loginAt DESC")
    Page<UserSession> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT EXTRACT(DOW FROM login_at) as dow, EXTRACT(HOUR FROM login_at) as hr, COUNT(*) as cnt " +
                   "FROM user_sessions " +
                   "WHERE login_at >= :since " +
                   "GROUP BY dow, hr", nativeQuery = true)
    List<Object[]> getHeatmapData(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.loginAt >= :start AND s.loginAt <= :end")
    long countSessionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
