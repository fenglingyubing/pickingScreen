package com.fenglingyubing.pickupfilter.config;

public final class FilterModeCycle {
    private FilterModeCycle() {
    }

    public static FilterMode next(FilterMode current) {
        if (current == null) {
            return FilterMode.PICKUP_MATCHING;
        }
        switch (current) {
            case DISABLED:
                return FilterMode.PICKUP_MATCHING;
            case PICKUP_MATCHING:
                return FilterMode.DESTROY_MATCHING;
            case DESTROY_MATCHING:
            default:
                return FilterMode.DISABLED;
        }
    }
}
