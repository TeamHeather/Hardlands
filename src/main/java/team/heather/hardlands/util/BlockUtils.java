package team.heather.hardlands.util;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;

public final class BlockUtils {

    private BlockUtils() {}

    public static void floodFill(Block origin, Predicate<Block> predicate) {
        Queue<Block> pending = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();

        pending.add(origin);

        while (!pending.isEmpty()) {
            Block block = pending.remove();

            if (visited.add(block) && predicate.test(block)) {
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x != 0 || y != 0 || z != 0) pending.add(block.getRelative(x, y, z));
                        }
                    }
                }
            }
        }
    }

    public static boolean isOre(Material material) {
        return Tag.COAL_ORES.isTagged(material)
                || Tag.IRON_ORES.isTagged(material)
                || Tag.COPPER_ORES.isTagged(material)
                || Tag.GOLD_ORES.isTagged(material)
                || Tag.DIAMOND_ORES.isTagged(material)
                || Tag.EMERALD_ORES.isTagged(material)
                || Tag.LAPIS_ORES.isTagged(material)
                || Tag.REDSTONE_ORES.isTagged(material);
    }
}