package team.heather.hardlands.util;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.block.Block;

/**
 * Performs a breadth-first flood fill over connected Minecraft blocks.
 *
 * <p>The algorithm begins at an origin block and explores every neighboring block
 * accepted by the supplied predicate. Connectivity includes all 26 blocks surrounding
 * a block in three dimensions, including horizontal, vertical, edge, and corner
 * diagonals.
 *
 * <p>Blocks are stored in a queue, causing the search to expand outward from the
 * origin layer by layer. A visited set ensures that every block is evaluated at most
 * once and prevents cycles from causing an infinite traversal.
 *
 * <p>A block is marked as visited as soon as it is discovered rather than when it is
 * removed from the queue. This prevents the same block from being queued repeatedly
 * by several adjacent blocks.
 */
public final class BlockFloodFill {

    private static final int[][] NEIGHBOR_OFFSETS = {
            {-1, -1, -1},
            {-1, -1, 0},
            {-1, -1, 1},
            {-1, 0, -1},
            {-1, 0, 0},
            {-1, 0, 1},
            {-1, 1, -1},
            {-1, 1, 0},
            {-1, 1, 1},
            {0, -1, -1},
            {0, -1, 0},
            {0, -1, 1},
            {0, 0, -1},
            {0, 0, 1},
            {0, 1, -1},
            {0, 1, 0},
            {0, 1, 1},
            {1, -1, -1},
            {1, -1, 0},
            {1, -1, 1},
            {1, 0, -1},
            {1, 0, 0},
            {1, 0, 1},
            {1, 1, -1},
            {1, 1, 0},
            {1, 1, 1}
    };

    private BlockFloodFill() {}

    /**
     * Traverses every connected block accepted by the given predicate.
     *
     * <p>If the origin does not satisfy the predicate, the traversal ends immediately.
     * Otherwise, the algorithm performs a breadth-first search through every matching
     * block connected to the origin.
     *
     * @param origin the block where the traversal begins
     * @param predicate determines whether a block belongs to the filled region
     */
    public static void fill(Block origin, Predicate<Block> predicate) {
        if (!predicate.test(origin)) {
            return;
        }

        Queue<Block> pending = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();

        pending.add(origin);
        visited.add(origin);

        while (!pending.isEmpty()) {
            Block block = pending.remove();

            for (int[] offset : NEIGHBOR_OFFSETS) {
                Block neighbor = block.getRelative(
                        offset[0],
                        offset[1],
                        offset[2]
                );

                if (visited.add(neighbor) && predicate.test(neighbor)) {
                    pending.add(neighbor);
                }
            }
        }
    }
}