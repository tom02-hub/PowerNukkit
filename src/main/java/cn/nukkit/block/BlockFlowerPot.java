package cn.nukkit.block;

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityFlowerPot;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemIds;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.Vector3f;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.Tag;
import cn.nukkit.player.Player;
import cn.nukkit.registry.BlockRegistry;
import cn.nukkit.utils.Identifier;

import static cn.nukkit.block.BlockIds.*;

/**
 * @author Nukkit Project Team
 */
public class BlockFlowerPot extends FloodableBlock {

    public BlockFlowerPot(Identifier id) {
        super(id);
    }

    protected static boolean canPlaceIntoFlowerPot(Identifier id) {
        return id == SAPLING || id == WEB || id == TALL_GRASS || id == DEADBUSH || id == YELLOW_FLOWER ||
                id == RED_FLOWER || id == RED_MUSHROOM || id == BROWN_MUSHROOM || id == CACTUS || id == REEDS;
        // TODO: 2016/2/4 case NETHER_WART:
    }

    @Override
    public double getHardness() {
        return 0;
    }

    @Override
    public double getResistance() {
        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        if (face != BlockFace.UP) return false;
        CompoundTag nbt = new CompoundTag()
                .putString("id", BlockEntity.FLOWER_POT)
                .putInt("x", (int) this.x)
                .putInt("y", (int) this.y)
                .putInt("z", (int) this.z)
                .putShort("item", 0)
                .putInt("data", 0);
        if (item.hasCustomBlockData()) {
            for (Tag aTag : item.getCustomBlockData().getAllTags()) {
                nbt.put(aTag.getName(), aTag);
            }
        }
        BlockEntityFlowerPot flowerPot = (BlockEntityFlowerPot) BlockEntity.createBlockEntity(BlockEntity.FLOWER_POT, getLevel().getChunk(block.getChunkX(), block.getChunkZ()), nbt);
        if (flowerPot == null) return false;

        this.getLevel().setBlock(block, this, true, true);
        return true;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Item item) {
        return this.onActivate(item, null);
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        BlockEntity blockEntity = getLevel().getBlockEntity(this);
        if (!(blockEntity instanceof BlockEntityFlowerPot)) return false;
        if (blockEntity.namedTag.getShort("item") != 0 || blockEntity.namedTag.getInt("mData") != 0) return false;
        Identifier itemType;
        int itemMeta;
        if (!canPlaceIntoFlowerPot(item.getId())) {
            if (!canPlaceIntoFlowerPot(item.getBlock().getId())) {
                return true;
            }
            itemType = item.getBlock().getId();
            itemMeta = item.getDamage();
        } else {
            itemType = item.getId();
            itemMeta = item.getDamage();
        }
        blockEntity.namedTag.putShort("item", BlockRegistry.get().getLegacyId(itemType));
        blockEntity.namedTag.putInt("data", itemMeta);

        this.setDamage(1);
        this.getLevel().setBlock(this, this, true);
        ((BlockEntityFlowerPot) blockEntity).spawnToAll();

        if (player.isSurvival()) {
            item.setCount(item.getCount() - 1);
            player.getInventory().setItemInHand(item.getCount() > 0 ? item : Item.get(AIR));
        }
        return true;
    }

    @Override
    public Item[] getDrops(Item item) {
        boolean dropInside = false;
        int insideID = 0;
        int insideMeta = 0;
        BlockEntity blockEntity = getLevel().getBlockEntity(this);
        if (blockEntity instanceof BlockEntityFlowerPot) {
            dropInside = true;
            insideID = blockEntity.namedTag.getShort("item");
            insideMeta = blockEntity.namedTag.getInt("data");
        }

        if (dropInside) {
            return new Item[]{
                    Item.get(ItemIds.FLOWER_POT),
                    Item.get(insideID, insideMeta, 1)
            };
        } else {
            return new Item[]{
                    Item.get(ItemIds.FLOWER_POT)
            };
        }
    }

    @Override
    protected AxisAlignedBB recalculateBoundingBox() {
        return this;
    }

    @Override
    public double getMinX() {
        return this.x + 0.3125;
    }

    @Override
    public double getMinZ() {
        return this.z + 0.3125;
    }

    @Override
    public double getMaxX() {
        return this.x + 0.6875;
    }

    @Override
    public double getMaxY() {
        return this.y + 0.375;
    }

    @Override
    public double getMaxZ() {
        return this.z + 0.6875;
    }

    @Override
    public boolean canPassThrough() {
        return false;
    }

    @Override
    public Item toItem() {
        return Item.get(ItemIds.FLOWER_POT);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
