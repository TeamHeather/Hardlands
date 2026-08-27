package org.heather.hardlands.module.world.pregeneration;

import org.popcraft.chunky.api.ChunkyAPI;

public record PregenerationRequest(
        String worldName,
        double centerX,
        double centerZ,
        double worldSize) {

    public boolean reviewAndStart(ChunkyAPI chunky) {
        double radius = this.worldSize / 2.0D;
        return chunky.startTask(
                this.worldName,
                "square",
                this.centerX,
                this.centerZ,
                radius,
                radius,
                "concentric");
    }
}
