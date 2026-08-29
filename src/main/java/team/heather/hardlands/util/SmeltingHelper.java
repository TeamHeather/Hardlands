package team.heather.hardlands.util;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

public final class SmeltingHelper {

    private static final Map<Material, SmeltingResult> ORE_RESULTS = Map.ofEntries(
            Map.entry(Material.COAL_ORE, new SmeltingResult(Material.COAL, 0.1F)),
            Map.entry(Material.DEEPSLATE_COAL_ORE, new SmeltingResult(Material.COAL, 0.1F)),
            Map.entry(Material.IRON_ORE, new SmeltingResult(Material.IRON_INGOT, 0.7F)),
            Map.entry(Material.DEEPSLATE_IRON_ORE, new SmeltingResult(Material.IRON_INGOT, 0.7F)),
            Map.entry(Material.COPPER_ORE, new SmeltingResult(Material.COPPER_INGOT, 0.7F)),
            Map.entry(Material.DEEPSLATE_COPPER_ORE, new SmeltingResult(Material.COPPER_INGOT, 0.7F)),
            Map.entry(Material.GOLD_ORE, new SmeltingResult(Material.GOLD_INGOT, 1.0F)),
            Map.entry(Material.DEEPSLATE_GOLD_ORE, new SmeltingResult(Material.GOLD_INGOT, 1.0F)),
            Map.entry(Material.DIAMOND_ORE, new SmeltingResult(Material.DIAMOND, 1.0F)),
            Map.entry(Material.DEEPSLATE_DIAMOND_ORE, new SmeltingResult(Material.DIAMOND, 1.0F)),
            Map.entry(Material.EMERALD_ORE, new SmeltingResult(Material.EMERALD, 1.0F)),
            Map.entry(Material.DEEPSLATE_EMERALD_ORE, new SmeltingResult(Material.EMERALD, 1.0F)),
            Map.entry(Material.LAPIS_ORE, new SmeltingResult(Material.LAPIS_LAZULI, 0.2F)),
            Map.entry(Material.DEEPSLATE_LAPIS_ORE, new SmeltingResult(Material.LAPIS_LAZULI, 0.2F)),
            Map.entry(Material.REDSTONE_ORE, new SmeltingResult(Material.REDSTONE, 0.7F)),
            Map.entry(Material.DEEPSLATE_REDSTONE_ORE, new SmeltingResult(Material.REDSTONE, 0.7F)));

    private static final Map<Material, Material> FOOD_RESULTS = Map.ofEntries(
            Map.entry(Material.BEEF, Material.COOKED_BEEF),
            Map.entry(Material.PORKCHOP, Material.COOKED_PORKCHOP),
            Map.entry(Material.CHICKEN, Material.COOKED_CHICKEN),
            Map.entry(Material.MUTTON, Material.COOKED_MUTTON),
            Map.entry(Material.RABBIT, Material.COOKED_RABBIT),
            Map.entry(Material.COD, Material.COOKED_COD),
            Map.entry(Material.SALMON, Material.COOKED_SALMON),
            Map.entry(Material.POTATO, Material.BAKED_POTATO));

    private SmeltingHelper() {}

    public static void smeltDrops(BlockDropItemEvent event) {
        SmeltingResult result = ORE_RESULTS.get(event.getBlockState().getType());
        if (result == null) return;

        int amount = 0;

        for (Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();

            amount += stack.getAmount();
            stack.setType(result.material());
        }

        dropExperience(event.getBlock(), amount, result.experience());
    }

    public static void breakSmelted(Block block, ItemStack tool) {
        SmeltingResult result = ORE_RESULTS.get(block.getType());
        if (result == null) {
            block.breakNaturally(tool);
            return;
        }

        Collection<ItemStack> drops = block.getDrops(tool);
        int amount = 0;

        for (ItemStack drop : drops) {
            amount += drop.getAmount();
            drop.setType(result.material());
        }

        block.setType(Material.AIR);

        for (ItemStack drop : drops) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }

        dropExperience(block, amount, result.experience());
    }

    public static void smeltFood(ItemStack item) {
        Material result = FOOD_RESULTS.get(item.getType());
        if (result != null) item.setType(result);
    }

    private static void dropExperience(Block block, int amount, float experiencePerItem) {
        float experience = amount * experiencePerItem;
        int value = (int) experience;

        if (ThreadLocalRandom.current().nextFloat() < experience - value) value++;
        if (value == 0) return;

        Location location = block.getLocation().add(0.5D, 0.5D, 0.5D);
        int finalValue = value;

        block.getWorld().spawn(location, ExperienceOrb.class, orb -> orb.setExperience(finalValue));
    }

    private record SmeltingResult(Material material, float experience) {}
}