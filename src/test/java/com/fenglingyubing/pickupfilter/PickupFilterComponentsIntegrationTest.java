package com.fenglingyubing.pickupfilter;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class PickupFilterComponentsIntegrationTest {

    @Test
    public void bootstrapsComponents() {
        PickupFilterComponents components = PickupFilterComponents.bootstrap();
        assertNotNull(components.getPlayerConfigStore());
        assertNotNull(components.getCommonEventHandler());
    }
}
