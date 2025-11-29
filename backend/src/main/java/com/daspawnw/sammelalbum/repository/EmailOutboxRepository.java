package com.daspawnw.sammelalbum.repository;

import com.daspawnw.sammelalbum.model.EmailOutbox;
import com.daspawnw.sammelalbum.model.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {
    List<EmailOutbox> findByStatus(EmailStatus status);

    @Query("SELECT e FROM EmailOutbox e WHERE e.status = :status AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)")
    List<EmailOutbox> findBatchToProcess(@Param("status") EmailStatus status, @Param("now") LocalDateTime now);
}
