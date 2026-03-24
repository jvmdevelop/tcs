package com.jvmd.transationapp.repository;
import com.jvmd.transationapp.model.EStatus;
import com.jvmd.transationapp.model.Transactions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface TransactionRepository extends JpaRepository<Transactions, UUID> {
    Optional<Transactions> findByCorrelationId(String correlationId);
    boolean existsByCorrelationId(String correlationId);
    Page<Transactions> findByStatus(EStatus status, Pageable pageable);
    List<Transactions> findByFromAndTimestampBetween(String from, LocalDateTime start, LocalDateTime end);
    List<Transactions> findByToAndTimestampBetween(String to, LocalDateTime start, LocalDateTime end);
    @Query("SELECT COUNT(t) FROM Transactions t WHERE t.status = :status")
    Long countByStatus(@Param("status") EStatus status);
    @Query("SELECT COUNT(t) FROM Transactions t WHERE t.timestamp BETWEEN :start AND :end")
    Long countByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    @Query("SELECT t FROM Transactions t WHERE t.timestamp BETWEEN :start AND :end ORDER BY t.timestamp DESC")
    List<Transactions> findTransactionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
