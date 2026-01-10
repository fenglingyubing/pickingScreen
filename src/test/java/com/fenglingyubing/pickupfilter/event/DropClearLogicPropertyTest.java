package com.fenglingyubing.pickupfilter.event;

import org.junit.Test;
import org.quicktheories.WithQuickTheories;
import org.quicktheories.generators.SourceDSL;

import java.util.ArrayList;
import java.util.List;

public class DropClearLogicPropertyTest implements WithQuickTheories {

    @Test
    public void property5_clearDropsCompleteness() {
        qt().withExamples(250)
                .forAll(SourceDSL.lists().of(SourceDSL.booleans().all()).ofSizeBetween(0, 200))
                .check(deadFlags -> {
                    List<FakeDrop> drops = new ArrayList<>(deadFlags.size());
                    int expectedToClear = 0;
                    for (boolean isDead : deadFlags) {
                        drops.add(new FakeDrop(isDead));
                        if (!isDead) {
                            expectedToClear++;
                        }
                    }

                    int cleared = DropClearLogic.clearAll(drops, FakeDrop::isDead, FakeDrop::setDead);
                    boolean allDeadAfter = drops.stream().allMatch(FakeDrop::isDead);
                    return cleared == expectedToClear && allDeadAfter;
                });
    }

    private static final class FakeDrop {
        private boolean dead;

        private FakeDrop(boolean dead) {
            this.dead = dead;
        }

        private boolean isDead() {
            return dead;
        }

        private void setDead() {
            dead = true;
        }
    }
}

