package com.jvmd.transationapp.repository;
import com.jvmd.transationapp.model.Rule;
import com.jvmd.transationapp.model.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {
    List<Rule> findByEnabledTrueOrderByPriorityAsc();
    List<Rule> findByTypeAndEnabledTrue(RuleType type);
    boolean existsByName(String name);
}
