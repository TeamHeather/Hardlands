package team.heather.hardlands.module.enchantment.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class DeadEyeProcessor {

    private static final Sound[] SOUND_FEEDBACK = new Sound[] {
            Sound.ITEM_TRIDENT_HIT_GROUND,
            Sound.BLOCK_END_PORTAL_FRAME_FILL
    };
    private static final double DAMAGE_DELTA = 0.025D;

    private final Map<UUID, ComboSnapshot> combos = new HashMap<>();

    public void handle(EntityDamageByEntityEvent event, int level) {
        Entity damager = event.getDamager();

        ComboSnapshot combo = resolveCombo(damager.getUniqueId());
        double bonusDamage = DAMAGE_DELTA * level * combo.streak();
        double totalDamage = event.getFinalDamage() + bonusDamage;

        event.setDamage(totalDamage);
        castFeedbackEffects(
                damager.getWorld(),
                damager.getLocation(),
                combo.streak()
        );

        combos.put(damager.getUniqueId(), combo);
    }

    private record ComboSnapshot(
            UUID damager,
            int streak,
            long lastHitTime
    ) {

        static final long COMBO_TIMEOUT = 1_500L;

        static ComboSnapshot create(UUID damager, int streak) {
            return new ComboSnapshot(damager, streak, System.currentTimeMillis());
        }

        ComboSnapshot withIncreasedStreak() {
            return create(damager, streak + 1);
        }

        boolean timedOut() {
            return System.currentTimeMillis() - lastHitTime() >= COMBO_TIMEOUT;
        }
    }

    private static void castFeedbackEffects(World world, Location location, int hits) {
        for (Sound sound : SOUND_FEEDBACK) {
            world.playSound(
                    location,
                    sound,
                    5.0F,
                    Math.max(1.3F - hits * 0.1F, 0.5F)
            );
        }

        world.spawnParticle(
                Particle.FLAME,
                location,
                4 + hits * 2,
                0.3D,
                0.4D,
                0.3D,
                0.0D
        );
    }

    private ComboSnapshot resolveCombo(UUID damager) {
        ComboSnapshot comboSnapshot = combos.get(damager);

        if (comboSnapshot == null || comboSnapshot.timedOut()) {
            return ComboSnapshot.create(damager, 1);
        }

        return comboSnapshot.withIncreasedStreak();
    }
}