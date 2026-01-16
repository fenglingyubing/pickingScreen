package com.fenglingyubing.pickupfilter.config;

import com.fenglingyubing.pickupfilter.PickupFilterCommon;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class ConfigManager {
    private static final String KEY_MODE = "mode";
    private static final String KEY_RULE_PREFIX = "rule.";
    private static final String KEY_RULE_COUNT = "rules.count";

    private File configFile;
    private FilterMode currentMode = FilterMode.DISABLED;
    private final List<FilterRule> filterRules = new ArrayList<>();

    public ConfigManager() {
        this(null);
    }

    public ConfigManager(File configFile) {
        this.configFile = configFile;
    }

    public synchronized void load() {
        if (configFile == null) {
            return;
        }
        loadConfig(configFile);
    }

    public synchronized void loadConfig(File configFile) {
        this.configFile = configFile;
        Properties properties = new Properties();
        boolean loadedFromDisk = false;
        if (configFile != null && configFile.exists() && configFile.isFile()) {
            try (FileInputStream inputStream = new FileInputStream(configFile)) {
                properties.load(inputStream);
                loadedFromDisk = true;
            } catch (Exception e) {
                PickupFilterCommon.LOGGER.warn("Failed to load config file: {}", configFile, e);
                return;
            }
        }
        syncFromProperties(properties);
        if (configFile != null && (!configFile.exists() || loadedFromDisk)) {
            saveConfig();
        }
    }

    public synchronized void saveConfig() {
        if (configFile == null) {
            return;
        }

        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            PickupFilterCommon.LOGGER.warn("Failed to create config directory: {}", parent);
            return;
        }

        Properties out = new Properties();
        out.setProperty(KEY_MODE, currentMode.getId());
        out.setProperty(KEY_RULE_COUNT, Integer.toString(filterRules.size()));
        for (int index = 0; index < filterRules.size(); index++) {
            out.setProperty(KEY_RULE_PREFIX + index, filterRules.get(index).serialize());
        }

        try (FileOutputStream outputStream = new FileOutputStream(configFile)) {
            out.store(outputStream, "PickupFilter config");
        } catch (Exception e) {
            PickupFilterCommon.LOGGER.warn("Failed to save config file: {}", configFile, e);
        }
    }

    public synchronized FilterMode getCurrentMode() {
        return currentMode;
    }

    public synchronized void setCurrentMode(FilterMode mode) {
        currentMode = mode == null ? FilterMode.DISABLED : mode;
    }

    public synchronized List<FilterRule> getFilterRules() {
        return Collections.unmodifiableList(new ArrayList<>(filterRules));
    }

    public synchronized void setFilterRules(List<FilterRule> rules) {
        filterRules.clear();
        if (rules != null) {
            for (FilterRule rule : rules) {
                if (rule != null) {
                    filterRules.add(rule);
                }
            }
        }
    }

    public synchronized void addFilterRule(FilterRule rule) {
        if (rule == null) {
            return;
        }
        filterRules.add(rule);
    }

    public synchronized boolean removeFilterRule(FilterRule rule) {
        return filterRules.remove(rule);
    }

    public synchronized boolean matchesAnyRule(String registryName, int metadata) {
        for (FilterRule rule : filterRules) {
            if (rule.matches(registryName, metadata)) {
                return true;
            }
        }
        return false;
    }

    private void syncFromProperties(Properties properties) {
        currentMode = FilterMode.fromId(properties.getProperty(KEY_MODE, FilterMode.DISABLED.getId()));

        filterRules.clear();
        List<String> ruleKeys = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(KEY_RULE_PREFIX)) {
                ruleKeys.add(key);
            }
        }
        ruleKeys.sort(Comparator.comparingInt(ConfigManager::parseRuleIndex));

        for (String key : ruleKeys) {
            try {
                FilterRule rule = FilterRule.deserialize(properties.getProperty(key));
                if (rule != null) {
                    filterRules.add(rule);
                }
            } catch (Exception e) {
                PickupFilterCommon.LOGGER.warn("Failed to parse rule '{}' in config file: {}", key, configFile, e);
            }
        }
    }

    private static int parseRuleIndex(String key) {
        try {
            return Integer.parseInt(key.substring(KEY_RULE_PREFIX.length()));
        } catch (Exception ignored) {
            return Integer.MAX_VALUE;
        }
    }
}
