package net.minestom.server.instance.block.rule;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemMeta;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BlockPlacementRule {
    protected final Block block;

    protected BlockPlacementRule(@NotNull Block block) {
        this.block = block;
    }

    /**
     * Called when the block state id can be updated (for instance if a neighbour block changed).
     * This is first called on a newly placed block, and then this is called for all neighbors of the block
     *
     * @param updateState The current parameters to the block update
     * @return the updated block
     */
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return updateState.currentBlock();
    }

    /**
     * Called when the block is placed.
     * It is recommended that you only set up basic properties on the block for this placement, such as determining facing, etc
     *
     * @param placementState The current parameters to the block placement
     * @return the block to place, {@code null} to cancel
     */
    public abstract @Nullable Block blockPlace(@NotNull PlacementState placementState);

    public boolean isSelfReplaceable(@NotNull Replacement replacement) {
        return false;
    }

    public @NotNull Block getBlock() {
        return block;
    }

    public record PlacementState(
            @NotNull Block.Getter instance,
            @NotNull Block block,
            @NotNull BlockFace blockFace,
            @NotNull Point placePosition,
            @NotNull Point cursorPosition,
            @NotNull Pos playerPosition,
            @NotNull ItemMeta usedItemMeta,
            boolean isPlayerShifting
    ) {
    }

    public record UpdateState(@NotNull Block.Getter instance,
                              @NotNull Point blockPosition,
                              @NotNull Block currentBlock) {
    }

    public record Replacement(
            @NotNull Block block,
            @NotNull BlockFace blockFace,
            @NotNull Point cursorPosition,
            @NotNull Material material
    ) {
    }
}
