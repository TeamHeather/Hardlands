package team.heather.hardlands.module.enchantment.handler;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public final class SmeltingTouchHandler implements EnchantmentHandler<Event> {

    private static final Map<Material, SmeltingResult> ORE_RESULTS = oreResults(Map.of(
            Material.COAL_ORE, result(Material.COAL, 0.1F),
            Material.IRON_ORE, result(Material.IRON_INGOT, 0.7F),
            Material.COPPER_ORE, result(Material.COPPER_INGOT, 0.7F),
            Material.GOLD_ORE, result(Material.GOLD_INGOT, 1.0F),
            Material.DIAMOND_ORE, result(Material.DIAMOND, 1.0F),
            Material.EMERALD_ORE, result(Material.EMERALD, 1.0F),
            Material.LAPIS_ORE, result(Material.LAPIS_LAZULI, 0.2F),
            Material.REDSTONE_ORE, result(Material.REDSTONE, 0.7F)
    ));

    private static final Map<Material, Material> COOKED_FOODS = Map.of(
            Material.BEEF, Material.COOKED_BEEF,
            Material.PORKCHOP, Material.COOKED_PORKCHOP,
            Material.CHICKEN, Material.COOKED_CHICKEN,
            Material.MUTTON, Material.COOKED_MUTTON,
            Material.RABBIT, Material.COOKED_RABBIT,
            Material.COD, Material.COOKED_COD,
            Material.SALMON, Material.COOKED_SALMON,
            Material.POTATO, Material.BAKED_POTATO
    );

    @Override
    public void handle(Event event, int amplifier) {
        switch (event) {
            case BlockDropItemEvent blockEvent -> handleBlockDrops(blockEvent);
            case EntityDeathEvent deathEvent
                    when deathEvent.getEntity() instanceof Animals -> cookDrops(deathEvent);
            default -> {}
        }
    }

    public static void breakSmelted(Block block, ItemStack tool) {
        SmeltingResult result = ORE_RESULTS.get(block.getType());

        if (result == null) {
            block.breakNaturally(tool);
            return;
        }

        Collection<ItemStack> drops = block.getDrops(tool);

        block.setType(Material.AIR);

        int amount = dropSmelted(block, drops, result.material());
        dropExperience(block, amount, result.experiencePerItem());
    }

    private static void handleBlockDrops(BlockDropItemEvent event) {
        SmeltingResult result = ORE_RESULTS.get(event.getBlockState().getType());

        if (result == null) return;

        int amount = 0;

        for (Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();

            amount += stack.getAmount();
            item.setItemStack(stack.withType(result.material()));
        }

        dropExperience(event.getBlock(), amount, result.experiencePerItem());
    }

    private static void cookDrops(EntityDeathEvent event) {
        List<ItemStack> drops = event.getDrops();

        for (int index = 0; index < drops.size(); index++) {
            ItemStack drop = drops.get(index);
            Material cooked = COOKED_FOODS.get(drop.getType());

            if (cooked != null) drops.set(index, drop.withType(cooked));
        }
    }

    private static int dropSmelted(Block block, Collection<ItemStack> drops, Material material) {
        int amount = 0;
        Location location = block.getLocation();

        for (ItemStack drop : drops) {
            amount += drop.getAmount();
            block.getWorld().dropItemNaturally(location, drop.withType(material));
        }

        return amount;
    }

    private static void dropExperience(Block block, int amount, float experiencePerItem) {
        float experience = amount * experiencePerItem;
        int value = (int) experience
                + (ThreadLocalRandom.current().nextFloat() < experience % 1.0F ? 1 : 0);

        if (value <= 0) return;

        Location location = block.getLocation().add(0.5D, 0.5D, 0.5D);
        block.getWorld().spawn(location, ExperienceOrb.class, orb -> orb.setExperience(value));
    }

    private static SmeltingResult result(Material material, float experience) {
        return new SmeltingResult(material, experience);
    }

    private static Map<Material, SmeltingResult> oreResults(Map<Material, SmeltingResult> ores) {
        Map<Material, SmeltingResult> results = new EnumMap<>(ores);

        ores.forEach((ore, result) ->
                results.put(Material.valueOf("DEEPSLATE_" + ore.name()), result));

        return Map.copyOf(results);
    }

    private record SmeltingResult(Material material, float experiencePerItem) {}
}