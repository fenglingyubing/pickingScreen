package com.fenglingyubing.pickupfilter;

import com.fenglingyubing.pickupfilter.config.PlayerFilterConfigStore;
import com.fenglingyubing.pickupfilter.event.CommonEventHandler;
import java.util.Objects;

public final class PickupFilterComponents {
    private final PlayerFilterConfigStore playerConfigStore;
    private final CommonEventHandler commonEventHandler;

    private PickupFilterComponents(
            PlayerFilterConfigStore playerConfigStore,
            CommonEventHandler commonEventHandler
    ) {
        this.playerConfigStore = Objects.requireNonNull(playerConfigStore, "playerConfigStore");
        this.commonEventHandler = Objects.requireNonNull(commonEventHandler, "commonEventHandler");
    }

    public static PickupFilterComponents bootstrap() {
        PlayerFilterConfigStore playerConfigStore = new PlayerFilterConfigStore();
        CommonEventHandler commonEventHandler = new CommonEventHandler(playerConfigStore);
        return new PickupFilterComponents(playerConfigStore, commonEventHandler);
    }

    public PlayerFilterConfigStore getPlayerConfigStore() {
        return playerConfigStore;
    }

    public CommonEventHandler getCommonEventHandler() {
        return commonEventHandler;
    }
}
