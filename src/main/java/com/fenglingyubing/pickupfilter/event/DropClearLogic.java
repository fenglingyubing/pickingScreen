package com.fenglingyubing.pickupfilter.event;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class DropClearLogic {
    private DropClearLogic() {
    }

    public static <T> int clearAll(Iterable<T> drops, Predicate<T> isDead, Consumer<T> setDead) {
        int cleared = 0;
        for (T drop : drops) {
            if (drop == null || isDead.test(drop)) {
                continue;
            }
            setDead.accept(drop);
            cleared++;
        }
        return cleared;
    }
}

