package org.heather.hardlands.module.phase;

import java.time.Duration;

import org.heather.hardlands.Hardlands;
import org.heather.hardlands.gui.ConfigBuilder;
import org.heather.hardlands.core.configuration.Option;
import org.heather.hardlands.gui.OptionDef;
import org.heather.hardlands.core.configuration.Validator;
import org.heather.hardlands.core.SingleThreadScheduler;

@ConfigBuilder(
        identifier = "timer",
        options = {
                @OptionDef(type = Integer.class, validators = Validator.Keys.NON_NEGATIVE, name = "gracePeriodMinute"),
                @OptionDef(type = Integer.class, validators = Validator.Keys.NON_NEGATIVE, name = "pvpMinute"),
                @OptionDef(type = Integer.class, validators = Validator.Keys.NON_NEGATIVE, name = "borderShrinkMinute"),
                @OptionDef(type = Integer.class, validators = Validator.Keys.NON_NEGATIVE, name = "meetupMinute"),
                @OptionDef(type = Integer.class, validators = Validator.Keys.NON_NEGATIVE, name = "deathmatchMinute")
        })
public final class PhaseTimer extends PhaseTimerConfiguration {

    private final SingleThreadScheduler scheduler;

    private Phase phase;

    public PhaseTimer(Hardlands plugin) {
        this.scheduler = plugin.getThreadScheduler();
    }

    public void startPhase(Phase phase) {
        this.phase = phase;
    }

    public void scheduleForOption(Runnable runnable, Option<Integer> option) {
        this.scheduler.scheduleOnMainThread(runnable, Duration.ofMinutes(option.getValue()));
    }

    @Override
    public boolean isConfigurationValid() {
        if (!super.isConfigurationValid()) return false;

        int gracePeriod = super.gracePeriodMinute.getValue();
        int pvp = super.pvpMinute.getValue();
        int borderShrink = super.borderShrinkMinute.getValue();
        int meetup = super.meetupMinute.getValue();
        int deathmatch = super.deathmatchMinute.getValue();

        return gracePeriod < pvp
                && pvp < borderShrink
                && borderShrink < meetup
                && meetup < deathmatch;
    }
}