package com.picklebook.repository;

import com.picklebook.entity.PricingRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    /**
     * Quy tắc giá áp dụng cho sân: của riêng sân hoặc quy tắc chung (court = null).
     * Sắp xếp theo priority giảm dần để service chọn quy tắc đầu tiên khớp.
     */
    @EntityGraph(attributePaths = {"court", "court.branch", "branch"})
    @Query("""
            select p from PricingRule p
            where p.active = true
              and ((p.court is null and (p.branch is null or p.branch.id = :branchId))
                   or p.court.id = :courtId)
            order by p.priority desc, p.id asc
            """)
    List<PricingRule> findApplicable(@Param("courtId") Long courtId, @Param("branchId") Long branchId);

    @EntityGraph(attributePaths = {"court", "court.branch", "branch"})
    List<PricingRule> findAllByOrderByPriorityDesc();

    List<PricingRule> findByCourtIdOrderByPriorityDesc(Long courtId);

    void deleteByCourtId(Long courtId);
}
