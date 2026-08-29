package team.heather.hardlands.game;

import java.util.Optional;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import team.heather.hardlands.core.config.Option;

public final class GameBossBar {

    private final BossBar bossBar = BossBar.bossBar(
            Component.empty(),
            1.0F,
            BossBar.Color.WHITE,
            BossBar.Overlay.PROGRESS
    );
    private final GameFlow flow;

    private double delta;

    public GameBossBar(GameFlow flow) {
        this.flow = flow;
    }

    public void update() {
        GameState phase = this.flow.getPhase();
        double delta = progressDelta(this.flow, phase).orElse(0.0D);

        this.bossBar.name(Component.text(phase.getDisplayName()));
        this.bossBar.color(phase.getCategory().getBossBarColor());

        switch (phase.getProgressMode()) {
            case EMPTY -> {
                this.bossBar.progress(0.0F);
                this.delta = 0.0D;
            }
            case FULL -> {
                this.bossBar.progress(1.0F);
                this.delta = 0.0D;
            }
            case FILL -> {
                this.bossBar.progress(0.0F);
                this.delta = delta;
            }
            case DRAIN -> {
                this.bossBar.progress(1.0F);
                this.delta = -delta;
            }
        }
    }

    public void progress() {
        double progress = this.bossBar.progress() + this.delta;
        this.bossBar.progress((float) Math.clamp(progress, 0.0D, 1.0D));
    }

    public BossBar bossBar() {
        return this.bossBar;
    }

    private static Optional<Double> progressDelta(GameFlow flow, GameState phase) {
        return phaseDurationInSeconds(flow, phase)
                .filter(duration -> duration > 0)
                .map(duration -> 1.0D / duration);
    }

    private static Optional<Integer> phaseDurationInSeconds(GameFlow flow, GameState phase) {
        Option<Integer> start = phase.getMinuteOption(flow);
        if (start == null) return Optional.empty();

        GameState[] phases = GameState.values();

        for (int i = phase.ordinal() + 1; i < phases.length; i++) {
            Option<Integer> nextStart = phases[i].getMinuteOption(flow);

            if (nextStart != null) {
                return Optional.of((nextStart.getValue() - start.getValue()) * 60);
            }
        }

        return Optional.empty();
    }
}