package team.heather.hardlands.module.enchantment.handler;

import org.bukkit.event.Event;

public interface EnchantmentHandler<E extends Event> {

    void handle(E event, int amplifier);
}