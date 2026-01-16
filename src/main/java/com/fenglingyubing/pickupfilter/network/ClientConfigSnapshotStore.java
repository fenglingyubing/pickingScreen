package com.fenglingyubing.pickupfilter.network;

import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.FilterRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    public static void applyLocalRulesForMode(FilterMode mode, List<FilterRule> rulesForMode) {
        FilterMode safeTargetMode = mode == null ? FilterMode.DISABLED : mode;
        Snapshot current = snapshot;
        FilterMode currentMode = current == null ? FilterMode.DISABLED : current.getMode();
        List<FilterRule> currentPickup = current == null ? Collections.emptyList() : current.getPickupRules();
        List<FilterRule> currentDestroy = current == null ? Collections.emptyList() : current.getDestroyRules();

        List<FilterRule> safePickup = Collections.unmodifiableList(copyRules(currentPickup));
        List<FilterRule> safeDestroy = Collections.unmodifiableList(copyRules(currentDestroy));
        List<FilterRule> safeTargetRules = Collections.unmodifiableList(copyRules(rulesForMode));

        snapshot = safeTargetMode == FilterMode.DESTROY_MATCHING
                ? new Snapshot(currentMode, safePickup, safeTargetRules)
                : new Snapshot(currentMode, safeTargetRules, safeDestroy);
        HAS_SNAPSHOT.set(true);
        REVISION.incrementAndGet();
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
        Set<FilterRule> seen = new LinkedHashSet<>();
        if (rules != null) {
            for (FilterRule rule : rules) {
                if (rule != null && seen.add(rule)) {
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
