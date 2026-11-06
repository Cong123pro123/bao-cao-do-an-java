package com.picklebook.repository;

import com.picklebook.entity.ExtraService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtraServiceRepository extends JpaRepository<ExtraService, Long> {

    List<ExtraService> findByActiveTrueOrderBySortOrderAsc();

    List<ExtraService> findAllByOrderBySortOrderAsc();
}
