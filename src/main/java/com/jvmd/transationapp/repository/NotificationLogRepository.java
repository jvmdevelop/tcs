package com.jvmd.transationapp.repository;
import com.jvmd.transationapp.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByTransactionId(UUID transactionId);
    List<NotificationLog> findByCorrelationId(String correlationId);
}
