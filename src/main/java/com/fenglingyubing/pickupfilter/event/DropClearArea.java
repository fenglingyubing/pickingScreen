package com.fenglingyubing.pickupfilter.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

public final class DropClearArea {
    private DropClearArea() {
    }

    public static final int DEFAULT_CHUNK_RADIUS = 2;

    public static AxisAlignedBB chunkRadiusArea(EntityPlayer player, int chunkRadius) {
        int chunkX = player.chunkCoordX;
        int chunkZ = player.chunkCoordZ;

        int minX = (chunkX - chunkRadius) * 16;
        int minZ = (chunkZ - chunkRadius) * 16;
        int maxX = (chunkX + chunkRadius) * 16 + 15;
        int maxZ = (chunkZ + chunkRadius) * 16 + 15;

        int maxY = player.world == null ? 256 : player.world.getHeight();
        return new AxisAlignedBB(minX, 0, minZ, maxX + 1D, maxY, maxZ + 1D);
    }
}

