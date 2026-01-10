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
    private static volatile Snapshot snapshot = new Snapshot(FilterMode.DISABLED, Collections.emptyList());

    public static int getRevision() {
        return REVISION.get();
    }

    public static boolean hasReceivedSnapshot() {
        return HAS_SNAPSHOT.get();
    }

    public static Snapshot getSnapshot() {
        return snapshot;
    }

    static void update(FilterMode mode, List<FilterRule> rules) {
        FilterMode safeMode = mode == null ? FilterMode.DISABLED : mode;
        List<FilterRule> copied = new ArrayList<>();
        if (rules != null) {
            for (FilterRule rule : rules) {
                if (rule != null) {
                    copied.add(rule);
                }
            }
        }
        snapshot = new Snapshot(safeMode, Collections.unmodifiableList(copied));
        HAS_SNAPSHOT.set(true);
        REVISION.incrementAndGet();
    }

    public static final class Snapshot {
        private final FilterMode mode;
        private final List<FilterRule> rules;

        Snapshot(FilterMode mode, List<FilterRule> rules) {
            this.mode = mode;
            this.rules = rules;
        }

        public FilterMode getMode() {
            return mode;
        }

        public List<FilterRule> getRules() {
            return rules;
        }
    }
}
