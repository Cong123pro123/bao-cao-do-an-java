package com.picklebook.service;

import com.picklebook.entity.SiteSetting;

import java.util.List;
import java.util.Map;

/**
 * Đọc/ghi cấu hình website dạng key - value.
 */
public interface SettingService {

    String get(String key, String defaultValue);

    String get(String key);

    int getInt(String key, int defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    List<SiteSetting> all();

    List<SiteSetting> byGroup(String group);

    Map<String, String> asMap();

    void update(String key, String value);

    void updateMany(Map<String, String> values);
}
