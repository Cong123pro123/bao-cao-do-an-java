package com.picklebook.repository;

import com.picklebook.entity.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteSettingRepository extends JpaRepository<SiteSetting, Long> {

    Optional<SiteSetting> findBySettingKey(String settingKey);

    List<SiteSetting> findAllByOrderBySettingGroupAscSettingKeyAsc();

    List<SiteSetting> findBySettingGroupOrderBySettingKeyAsc(String settingGroup);
}
