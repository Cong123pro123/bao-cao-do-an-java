package com.picklebook.service.impl;

import com.picklebook.entity.SiteSetting;
import com.picklebook.repository.SiteSettingRepository;
import com.picklebook.service.SettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cấu hình được cache trong bộ nhớ để tránh truy vấn DB trên mỗi request.
 * Cache bị xóa khi Admin cập nhật cấu hình.
 */
@Service
public class SettingServiceImpl implements SettingService {

    private final SiteSettingRepository settingRepository;
    private volatile Map<String, String> cache;

    public SettingServiceImpl(SiteSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Override
    public String get(String key, String defaultValue) {
        String value = cache().get(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    @Override
    public String get(String key) {
        return get(key, "");
    }

    @Override
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = cache().get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    @Override
    public List<SiteSetting> all() {
        return settingRepository.findAllByOrderBySettingGroupAscSettingKeyAsc();
    }

    @Override
    public List<SiteSetting> byGroup(String group) {
        return settingRepository.findBySettingGroupOrderBySettingKeyAsc(group);
    }

    @Override
    public Map<String, String> asMap() {
        return cache();
    }

    @Override
    @Transactional
    public void update(String key, String value) {
        SiteSetting setting = settingRepository.findBySettingKey(key)
                .orElseGet(() -> {
                    SiteSetting created = new SiteSetting();
                    created.setSettingKey(key);
                    created.setSettingGroup("GENERAL");
                    return created;
                });
        setting.setSettingValue(value);
        settingRepository.save(setting);
        cache = null;
    }

    @Override
    @Transactional
    public void updateMany(Map<String, String> values) {
        values.forEach((key, value) -> {
            SiteSetting setting = settingRepository.findBySettingKey(key).orElse(null);
            if (setting != null) {
                setting.setSettingValue(value);
            }
        });
        cache = null;
    }

    private Map<String, String> cache() {
        Map<String, String> current = cache;
        if (current == null) {
            synchronized (this) {
                current = cache;
                if (current == null) {
                    current = settingRepository.findAll().stream()
                            .filter(item -> item.getSettingValue() != null)
                            .collect(Collectors.toMap(
                                    SiteSetting::getSettingKey,
                                    SiteSetting::getSettingValue,
                                    (first, second) -> second,
                                    LinkedHashMap::new));
                    cache = current;
                }
            }
        }
        return current;
    }
}
