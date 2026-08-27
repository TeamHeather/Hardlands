package org.heather.hardlands.module.world;

import java.time.Duration;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.heather.hardlands.configuration.ConfigBuilder;
import org.heather.hardlands.core.configuration.Option;
import org.heather.hardlands.configuration.OptionDef;
import org.heather.hardlands.core.configuration.Validator;
import org.heather.hardlands.module.world.pregeneration.PregenerationManager;
import org.heather.hardlands.module.world.pregeneration.PregenerationRequest;
import org.popcraft.chunky.api.ChunkyAPI;

@ConfigBuilder(identifier = "world", options = {
        @OptionDef(type = Set.class, elementType = String.class, name = "enabledWorlds"),
        @OptionDef(type = Integer.class, validators = Validator.Keys.POSITIVE, name = "survivalSize"),
        @OptionDef(type = Integer.class, validators = Validator.Keys.POSITIVE, name = "meetupSize"),
        @OptionDef(type = Integer.class, validators = Validator.Keys.NON_NEGATIVE, name = "deathmatchSize"),
        @OptionDef(type = Boolean.class, name = "surfaceTeleport"),
        @OptionDef(type = Boolean.class, name = "borderDamage"),
        @OptionDef(type = Double.class, name = "centerX"),
        @OptionDef(type = Double.class, name = "centerZ")
})
public final class WorldManager extends WorldManagerConfiguration {

    private final PregenerationManager pregenerationManager = new PregenerationManager(requireChunkyService());

    public void configure() {
        this.forEachEnabledWorld((world, centerX, centerZ, survivalSize) -> {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(centerX, centerZ);
            border.setSize(survivalSize);
        });
    }

    public void pregenerate() {
        this.forEachEnabledWorld((world, centerX, centerZ, survivalSize) ->
                this.pregenerationManager.reviewAndAccept(
                        new PregenerationRequest(world.getName(), centerX, centerZ, survivalSize)));
    }

    public void shrinkForMeetup(Duration duration) {
        this.shrinkWorldBorders(super.meetupSize, duration);
    }

    public void shrinkForDeathmatch(Duration duration) {
        this.shrinkWorldBorders(super.deathmatchSize, duration);
    }

    public PregenerationManager getPregenerationManager() {
        return this.pregenerationManager;
    }

    @Override
    public boolean isConfigurationValid() {
        if (!super.isConfigurationValid()) return false;

        int survival = super.survivalSize.getValue();
        int meetup = super.meetupSize.getValue();
        int deathmatch = super.deathmatchSize.getValue();

        return survival >= meetup && meetup >= deathmatch;
    }

    private void shrinkWorldBorders(Option<Integer> targetSize, Duration duration) {
        int size = targetSize.getValue();
        long ticks = duration.toMillis() / 50L;

        this.forEachEnabledWorld(world ->
                world.getWorldBorder().changeSize(scaleForDimension(world, size), ticks));
    }

    private void forEachEnabledWorld(Consumer<World> action) {
        super.enabledWorlds.getValue().forEach(worldName -> {
            World world = Bukkit.getWorld(worldName);

            if (world == null) throw new IllegalStateException("Enabled world is not loaded: " + worldName);

            action.accept(world);
        });
    }

    private void forEachEnabledWorld(WorldConfigurationConsumer action) {
        this.forEachEnabledWorld(world -> action.accept(
                world,
                scaleForDimension(world, super.centerX.getValue()),
                scaleForDimension(world, super.centerZ.getValue()),
                scaleForDimension(world, super.survivalSize.getValue())));
    }

    private static double scaleForDimension(World world, double value) {
        return world.getEnvironment() == World.Environment.NETHER ? value / 8.0D : value;
    }

    private static ChunkyAPI requireChunkyService() {
        ChunkyAPI chunky = Bukkit.getServicesManager().load(ChunkyAPI.class);

        if (chunky == null) throw new IllegalStateException("This plugin requires Chunky");

        return chunky;
    }

    @FunctionalInterface
    private interface WorldConfigurationConsumer {

        void accept(World world, double centerX, double centerZ, double survivalSize);
    }
}