package com.jvmd.transationapp.repository;
import com.jvmd.transationapp.model.RuleChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface RuleChangeHistoryRepository extends JpaRepository<RuleChangeHistory, Long> {
    List<RuleChangeHistory> findByRuleIdOrderByChangedAtDesc(Long ruleId);
}
