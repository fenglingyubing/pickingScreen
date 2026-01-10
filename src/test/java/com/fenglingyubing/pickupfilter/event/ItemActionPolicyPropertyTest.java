package com.fenglingyubing.pickupfilter.event;

import com.fenglingyubing.pickupfilter.config.FilterMode;
import org.junit.Test;
import org.quicktheories.WithQuickTheories;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.SourceDSL;

public class ItemActionPolicyPropertyTest implements WithQuickTheories {

    @Test
    public void property3_pickupModeFilteringConsistency() {
        qt().withExamples(250)
                .forAll(modes(), SourceDSL.booleans().all())
                .check((mode, matchesFilter) -> {
                    boolean cancelPickup = ItemActionPolicy.shouldCancelPickup(mode, matchesFilter);
                    if (mode == FilterMode.PICKUP_MATCHING) {
                        return cancelPickup == !matchesFilter;
                    }
                    return !cancelPickup;
                });
    }

    @Test
    public void property2_destroyModeRemovesMatchingDrops() {
        qt().withExamples(250)
                .forAll(modes(), SourceDSL.booleans().all())
                .check((mode, matchesFilter) -> {
                    boolean destroyDrop = ItemActionPolicy.shouldDestroyDrop(mode, matchesFilter);
                    if (mode == FilterMode.DESTROY_MATCHING) {
                        return destroyDrop == matchesFilter;
                    }
                    return !destroyDrop;
                });
    }

    @Test
    public void property4_disabledModeTransparency() {
        qt().withExamples(250)
                .forAll(SourceDSL.booleans().all())
                .check(matchesFilter -> !ItemActionPolicy.shouldCancelPickup(FilterMode.DISABLED, matchesFilter)
                        && !ItemActionPolicy.shouldDestroyDrop(FilterMode.DISABLED, matchesFilter));
    }

    private static Gen<FilterMode> modes() {
        return SourceDSL.arbitrary().pick(FilterMode.values());
    }
}
