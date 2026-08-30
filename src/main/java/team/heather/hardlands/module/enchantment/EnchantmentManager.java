package team.heather.hardlands.module.enchantment;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import team.heather.hardlands.module.enchantment.handler.EnchantmentHandler;

public final class EnchantmentManager implements Listener {

    private static final Set<HardlandsEnchantment> BLOCK_BREAK_ENCHANTMENTS = EnumSet.of(
            HardlandsEnchantment.TIMBER,
            HardlandsEnchantment.VEIN_MINER,
            HardlandsEnchantment.WISDOM
    );

    private final Set<HardlandsEnchantment> activeEnchantments = EnumSet.noneOf(HardlandsEnchantment.class);
    private final Plugin plugin;

    public EnchantmentManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void activate(HardlandsEnchantment... enchantments) {
        this.activeEnchantments.addAll(Arrays.asList(enchantments));
        this.updateRegistrations();
    }

    public void deactivate(HardlandsEnchantment... enchantments) {
        Arrays.asList(enchantments).forEach(this.activeEnchantments::remove);
        this.updateRegistrations();
    }

    public void deactivateAll() {
        this.activeEnchantments.clear();
        HandlerList.unregisterAll(this);
    }

    private void updateRegistrations() {
        HandlerList.unregisterAll(this);

        if (this.activeEnchantments.stream().anyMatch(BLOCK_BREAK_ENCHANTMENTS::contains)) {
            this.register(BlockBreakEvent.class, this::onBlockBreak);
        }

        if (this.activeEnchantments.contains(HardlandsEnchantment.SMELTING_TOUCH)) {
            this.register(BlockDropItemEvent.class, this::onBlockDropItem);
            this.register(EntityDeathEvent.class, this::onEntityDeath);
        }
    }

    private void onBlockBreak(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        this.apply(event, tool, HardlandsEnchantment.TIMBER);
        if (event.isCancelled()) return;

        this.apply(event, tool, HardlandsEnchantment.VEIN_MINER);
        this.apply(event, tool, HardlandsEnchantment.WISDOM);
    }

    private void onBlockDropItem(BlockDropItemEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        this.apply(event, tool, HardlandsEnchantment.SMELTING_TOUCH);
    }

    private void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        this.apply(
                event,
                killer.getInventory().getItemInMainHand(),
                HardlandsEnchantment.SMELTING_TOUCH
        );
    }

    private <E extends Event> void apply(
            E event,
            ItemStack item,
            HardlandsEnchantment enchantment
    ) {
        if (!this.activeEnchantments.contains(enchantment)) return;

        enchantment.findMatchingLevel(item).ifPresent(level ->
                this.handle(enchantment, event, level));
    }

    @SuppressWarnings("unchecked")
    private <E extends Event> void handle(
            HardlandsEnchantment enchantment,
            E event,
            int amplifier
    ) {
        EnchantmentHandler<E> handler = (EnchantmentHandler<E>) enchantment.getHandler();
        handler.handle(event, amplifier);
    }

    private <E extends Event> void register(Class<E> eventType, Consumer<E> handler) {
        Bukkit.getPluginManager().registerEvent(
                eventType,
                this,
                EventPriority.HIGHEST,
                (listener, event) -> handler.accept(eventType.cast(event)),
                this.plugin,
                true
        );
    }
}