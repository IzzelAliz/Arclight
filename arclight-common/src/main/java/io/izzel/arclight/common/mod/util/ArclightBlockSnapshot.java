package io.izzel.arclight.common.mod.util;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.BlockSnapshot;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v.block.*;

public class ArclightBlockSnapshot extends CraftBlock {

    private final BlockSnapshot blockSnapshot;
    private final BlockState blockState;

    public ArclightBlockSnapshot(BlockSnapshot blockSnapshot, boolean current) {
        super(blockSnapshot.getWorld(), blockSnapshot.getPos());
        this.blockSnapshot = blockSnapshot;
        this.blockState = current ? blockSnapshot.getCurrentBlock() : blockSnapshot.getReplacedBlock();
    }

    @Override
    public BlockState getNMS() {
        return blockState;
    }

    @SuppressWarnings("unchecked")
    @Override
    public org.bukkit.block.BlockState getState() {
        Material material = getType();

        switch (material) {
            case ACACIA_SIGN:
            case ACACIA_WALL_SIGN:
            case BIRCH_SIGN:
            case BIRCH_WALL_SIGN:
            case DARK_OAK_SIGN:
            case DARK_OAK_WALL_SIGN:
            case JUNGLE_SIGN:
            case JUNGLE_WALL_SIGN:
            case OAK_SIGN:
            case OAK_WALL_SIGN:
            case SPRUCE_SIGN:
            case SPRUCE_WALL_SIGN:
                return new CraftSign(this);
            case CHEST:
            case TRAPPED_CHEST:
                return new CraftChest(this);
            case FURNACE:
                return new CraftFurnaceFurnace(this);
            case DISPENSER:
                return new CraftDispenser(this);
            case DROPPER:
                return new CraftDropper(this);
            case END_GATEWAY:
                return new CraftEndGateway(this);
            case HOPPER:
                return new CraftHopper(this);
            case SPAWNER:
                return new CraftCreatureSpawner(this);
            case JUKEBOX:
                return new CraftJukebox(this);
            case BREWING_STAND:
                return new CraftBrewingStand(this);
            case CREEPER_HEAD:
            case CREEPER_WALL_HEAD:
            case DRAGON_HEAD:
            case DRAGON_WALL_HEAD:
            case PLAYER_HEAD:
            case PLAYER_WALL_HEAD:
            case SKELETON_SKULL:
            case SKELETON_WALL_SKULL:
            case WITHER_SKELETON_SKULL:
            case WITHER_SKELETON_WALL_SKULL:
            case ZOMBIE_HEAD:
            case ZOMBIE_WALL_HEAD:
                return new CraftSkull(this);
            case COMMAND_BLOCK:
            case CHAIN_COMMAND_BLOCK:
            case REPEATING_COMMAND_BLOCK:
                return new CraftCommandBlock(this);
            case BEACON:
                return new CraftBeacon(this);
            case BLACK_BANNER:
            case BLACK_WALL_BANNER:
            case BLUE_BANNER:
            case BLUE_WALL_BANNER:
            case BROWN_BANNER:
            case BROWN_WALL_BANNER:
            case CYAN_BANNER:
            case CYAN_WALL_BANNER:
            case GRAY_BANNER:
            case GRAY_WALL_BANNER:
            case GREEN_BANNER:
            case GREEN_WALL_BANNER:
            case LIGHT_BLUE_BANNER:
            case LIGHT_BLUE_WALL_BANNER:
            case LIGHT_GRAY_BANNER:
            case LIGHT_GRAY_WALL_BANNER:
            case LIME_BANNER:
            case LIME_WALL_BANNER:
            case MAGENTA_BANNER:
            case MAGENTA_WALL_BANNER:
            case ORANGE_BANNER:
            case ORANGE_WALL_BANNER:
            case PINK_BANNER:
            case PINK_WALL_BANNER:
            case PURPLE_BANNER:
            case PURPLE_WALL_BANNER:
            case RED_BANNER:
            case RED_WALL_BANNER:
            case WHITE_BANNER:
            case WHITE_WALL_BANNER:
            case YELLOW_BANNER:
            case YELLOW_WALL_BANNER:
                return new CraftBanner(this);
            case STRUCTURE_BLOCK:
                return new CraftStructureBlock(this);
            case SHULKER_BOX:
            case WHITE_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case RED_SHULKER_BOX:
            case BLACK_SHULKER_BOX:
                return new CraftShulkerBox(this);
            case ENCHANTING_TABLE:
                return new CraftEnchantingTable(this);
            case ENDER_CHEST:
                return new CraftEnderChest(this);
            case DAYLIGHT_DETECTOR:
                return new CraftDaylightDetector(this);
            case COMPARATOR:
                return new CraftComparator(this);
            case BLACK_BED:
            case BLUE_BED:
            case BROWN_BED:
            case CYAN_BED:
            case GRAY_BED:
            case GREEN_BED:
            case LIGHT_BLUE_BED:
            case LIGHT_GRAY_BED:
            case LIME_BED:
            case MAGENTA_BED:
            case ORANGE_BED:
            case PINK_BED:
            case PURPLE_BED:
            case RED_BED:
            case WHITE_BED:
            case YELLOW_BED:
                return new CraftBed(this);
            case CONDUIT:
                return new CraftConduit(this);
            case BARREL:
                return new CraftBarrel(this);
            case BELL:
                return new CraftBell(this);
            case BLAST_FURNACE:
                return new CraftBlastFurnace(this);
            case CAMPFIRE:
                return new CraftCampfire(this);
            case JIGSAW:
                return new CraftJigsaw(this);
            case LECTERN:
                return new CraftLectern(this);
            case SMOKER:
                return new CraftSmoker(this);
            default:
                TileEntity tileEntity = blockSnapshot.getTileEntity();
                if (tileEntity != null) {
                    // block with unhandled TileEntity:
                    return new CraftBlockEntityState<>(this, (Class<TileEntity>) tileEntity.getClass());
                } else {
                    // Block without TileEntity:
                    return new CraftBlockState(this);
                }
        }
    }

    public static ArclightBlockSnapshot fromBlockSnapshot(BlockSnapshot blockSnapshot, boolean current) {
        return new ArclightBlockSnapshot(blockSnapshot, current);
    }
}
