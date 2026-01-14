package com.fenglingyubing.pickupfilter.network;

import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.FilterRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClientConfigSnapshotStoreTest {
    @Test
    public void applyLocalRulesForMode_updatesPickupRulesAndRevision() {
        int baseRevision = ClientConfigSnapshotStore.getRevision();

        List<FilterRule> pickupRules = Collections.singletonList(new FilterRule("minecraft", "dirt", 0, false));
        List<FilterRule> destroyRules = Collections.singletonList(new FilterRule("minecraft", "gravel", 0, false));
        ClientConfigSnapshotStore.update(FilterMode.PICKUP_MATCHING, pickupRules, destroyRules);
        assertEquals(baseRevision + 1, ClientConfigSnapshotStore.getRevision());
        assertTrue(ClientConfigSnapshotStore.hasReceivedSnapshot());

        List<FilterRule> updatedPickup = Arrays.asList(
                new FilterRule("minecraft", "stone", 0, false),
                new FilterRule("minecraft", "stone", 0, false)
        );
        int beforeApply = ClientConfigSnapshotStore.getRevision();
        ClientConfigSnapshotStore.applyLocalRulesForMode(FilterMode.DISABLED, updatedPickup);
        assertEquals(beforeApply + 1, ClientConfigSnapshotStore.getRevision());

        ClientConfigSnapshotStore.Snapshot snapshot = ClientConfigSnapshotStore.getSnapshot();
        assertEquals(FilterMode.PICKUP_MATCHING, snapshot.getMode());
        assertEquals(Collections.singletonList(new FilterRule("minecraft", "stone", 0, false)), snapshot.getPickupRules());
        assertEquals(destroyRules, snapshot.getDestroyRules());
    }

    @Test
    public void applyLocalRulesForMode_updatesDestroyRulesAndKeepsMode() {
        List<FilterRule> pickupRules = Collections.singletonList(new FilterRule("minecraft", "log", 0, false));
        List<FilterRule> destroyRules = Collections.singletonList(new FilterRule("minecraft", "cobblestone", 0, false));
        ClientConfigSnapshotStore.update(FilterMode.DESTROY_MATCHING, pickupRules, destroyRules);

        List<FilterRule> updatedDestroy = Collections.singletonList(new FilterRule("minecraft", "sand", 0, false));
        ClientConfigSnapshotStore.applyLocalRulesForMode(FilterMode.DESTROY_MATCHING, updatedDestroy);

        ClientConfigSnapshotStore.Snapshot snapshot = ClientConfigSnapshotStore.getSnapshot();
        assertEquals(FilterMode.DESTROY_MATCHING, snapshot.getMode());
        assertEquals(pickupRules, snapshot.getPickupRules());
        assertEquals(updatedDestroy, snapshot.getDestroyRules());
    }
}

