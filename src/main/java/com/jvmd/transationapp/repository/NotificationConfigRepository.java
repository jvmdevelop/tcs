package com.jvmd.transationapp.repository;
import com.jvmd.transationapp.model.NotificationChannel;
import com.jvmd.transationapp.model.NotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, Long> {
    List<NotificationConfig> findByEnabledTrue();
    List<NotificationConfig> findByChannelAndEnabledTrue(NotificationChannel channel);
}
