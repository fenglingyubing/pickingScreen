package com.fenglingyubing.pickupfilter.config;

import java.util.Locale;

public enum FilterMode {
    DESTROY_MATCHING("destroy_matching"),
    PICKUP_MATCHING("pickup_matching"),
    DISABLED("disabled");

    private final String id;

    FilterMode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getTranslationKey() {
        return "pickupfilter.mode." + id;
    }

    public static FilterMode fromId(String id) {
        if (id == null) {
            return DISABLED;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (FilterMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }
        return DISABLED;
    }
}
