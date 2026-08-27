package org.heather.hardlands.common.enchantment.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class DeadEyeHandler {

    private static final long COMBO_TIMEOUT = 1_500L;
    private static final double DAMAGE_PER_HIT = 0.02D;

    private final Map<UUID, Combo> combos = new HashMap<>();

    public void handle(EntityDamageByEntityEvent event, Player player, int level) {
        UUID playerId = player.getUniqueId();
        UUID targetId = event.getEntity().getUniqueId();
        long currentTime = System.currentTimeMillis();

        Combo combo = this.combos.get(playerId);
        boolean continuesCombo = combo != null && combo.targetId().equals(targetId) && currentTime - combo.lastHitTime() <= COMBO_TIMEOUT;

        int hitCount = continuesCombo ? combo.hitCount() + 1 : 1;
        double bonus = (hitCount - 1) * level * DAMAGE_PER_HIT;
        double damage = event.getDamage() * (1.0D + bonus);

        event.setDamage(damage);

        displayActionBar(player, hitCount, bonus, damage);
        playEffect(player, hitCount);

        this.combos.put(playerId, new Combo(targetId, hitCount, currentTime));
    }

    public void reset(Player player) {
        this.combos.remove(player.getUniqueId());
    }

    //* Record

    private record Combo(
            UUID targetId,
            int hitCount,
            long lastHitTime
    ) {}

    //* Internal Class Utilities

    private static void playEffect(Player player, int hitCount) {
        float pitch = Math.min(0.85F + (hitCount - 1) * 0.15F, 2.0F);
        int particles = Math.min(4 + (hitCount - 1) * 3, 32);

        World world = player.getWorld();
        world.playSound(player.getLocation(), "minecraft:block.end_portal_frame.fill", 5.0F, pitch);
        world.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), particles, 0.4D, 1.0D, 0.4D, 0.03D);
    }

    private static void displayActionBar(Player player, int hitCount, double bonus, double damage) {
        player.sendActionBar(
                Component.text("◆ ", NamedTextColor.DARK_AQUA)
                        .append(Component.text("DEAD EYE", NamedTextColor.AQUA))
                        .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("×" + hitCount, NamedTextColor.YELLOW))
                        .append(Component.text("  +", NamedTextColor.DARK_GRAY))
                        .append(Component.text(Math.round(bonus * 100) + "%", NamedTextColor.GREEN))
                        .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(String.format("%.1f", damage), NamedTextColor.RED))
                        .append(Component.text(" ❤", NamedTextColor.DARK_RED)));
    }
}