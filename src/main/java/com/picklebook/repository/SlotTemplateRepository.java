package com.picklebook.repository;

import com.picklebook.entity.SlotTemplate;
import com.picklebook.entity.enums.DayType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlotTemplateRepository extends JpaRepository<SlotTemplate, Long> {

    List<SlotTemplate> findByBranchIdAndDayTypeAndActiveTrueOrderByStartTimeAsc(
            Long branchId, DayType dayType);
}
