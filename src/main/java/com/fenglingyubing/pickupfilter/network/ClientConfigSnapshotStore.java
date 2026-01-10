package com.fenglingyubing.pickupfilter.network;

import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.FilterRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ClientConfigSnapshotStore {
    private ClientConfigSnapshotStore() {
    }

    private static final AtomicInteger REVISION = new AtomicInteger();
    private static final AtomicBoolean HAS_SNAPSHOT = new AtomicBoolean(false);
    private static volatile Snapshot snapshot = new Snapshot(FilterMode.DISABLED, Collections.emptyList(), Collections.emptyList());

    public static int getRevision() {
        return REVISION.get();
    }

    public static boolean hasReceivedSnapshot() {
        return HAS_SNAPSHOT.get();
    }

    public static Snapshot getSnapshot() {
        return snapshot;
    }

    static void update(FilterMode mode, List<FilterRule> pickupRules, List<FilterRule> destroyRules) {
        FilterMode safeMode = mode == null ? FilterMode.DISABLED : mode;
        snapshot = new Snapshot(
                safeMode,
                Collections.unmodifiableList(copyRules(pickupRules)),
                Collections.unmodifiableList(copyRules(destroyRules))
        );
        HAS_SNAPSHOT.set(true);
        REVISION.incrementAndGet();
    }

    private static List<FilterRule> copyRules(List<FilterRule> rules) {
        List<FilterRule> copied = new ArrayList<>();
        if (rules != null) {
            for (FilterRule rule : rules) {
                if (rule != null && !copied.contains(rule)) {
                    copied.add(rule);
                }
            }
        }
        if (copied.size() > 200) {
            copied = copied.subList(0, 200);
        }
        return copied;
    }

    public static final class Snapshot {
        private final FilterMode mode;
        private final List<FilterRule> pickupRules;
        private final List<FilterRule> destroyRules;

        Snapshot(FilterMode mode, List<FilterRule> pickupRules, List<FilterRule> destroyRules) {
            this.mode = mode;
            this.pickupRules = pickupRules == null ? Collections.emptyList() : pickupRules;
            this.destroyRules = destroyRules == null ? Collections.emptyList() : destroyRules;
        }

        public FilterMode getMode() {
            return mode;
        }

        public List<FilterRule> getPickupRules() {
            return pickupRules;
        }

        public List<FilterRule> getDestroyRules() {
            return destroyRules;
        }

        public List<FilterRule> getRulesForMode(FilterMode mode) {
            FilterMode safeMode = mode == null ? FilterMode.DISABLED : mode;
            if (safeMode == FilterMode.DESTROY_MATCHING) {
                return destroyRules;
            }
            return pickupRules;
        }
    }
}
