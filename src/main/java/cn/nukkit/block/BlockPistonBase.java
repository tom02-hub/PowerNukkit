package cn.nukkit.block;

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityPistonArm;
import cn.nukkit.event.block.BlockPistonChangeEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.BlockPosition;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.Vector3f;
import cn.nukkit.math.Vector3i;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.player.Player;
import cn.nukkit.utils.Faceable;
import cn.nukkit.utils.Identifier;

import java.util.ArrayList;
import java.util.List;

import static cn.nukkit.block.BlockIds.*;

/**
 * @author CreeperFace
 */
public abstract class BlockPistonBase extends BlockSolid implements Faceable {

    public boolean sticky;

    public BlockPistonBase(Identifier id) {
        super(id);
    }

    @Override
    public double getResistance() {
        return 2.5;
    }

    @Override
    public double getHardness() {
        return 0.5;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        if (Math.abs(player.x - this.x) < 2 && Math.abs(player.z - this.z) < 2) {
            double y = player.y + player.getEyeHeight();

            if (y - this.y > 2) {
                this.setDamage(BlockFace.UP.getIndex());
            } else if (this.y - y > 0) {
                this.setDamage(BlockFace.DOWN.getIndex());
            } else {
                this.setDamage(player.getHorizontalFacing().getIndex());
            }
        } else {
            this.setDamage(player.getHorizontalFacing().getIndex());
        }
        this.level.setBlock(block, this, true, false);

        CompoundTag nbt = new CompoundTag("")
                .putString("id", BlockEntity.PISTON_ARM)
                .putInt("x", (int) this.x)
                .putInt("y", (int) this.y)
                .putInt("z", (int) this.z)
                .putBoolean("Sticky", this.sticky);

        BlockEntityPistonArm be = (BlockEntityPistonArm) BlockEntity.createBlockEntity(BlockEntity.PISTON_ARM, this.level.getChunk(this.getChunkX(), this.getChunkZ()), nbt);

        if (be == null) return false;
        //this.checkState();
        return true;
    }

    @Override
    public boolean onBreak(Item item) {
        super.onBreak(item);

        Block block = this.getSide(getFacing());

        if (block instanceof BlockPistonHead && ((BlockPistonHead) block).getFacing() == this.getFacing()) {
            block.onBreak(item);
        }
        return true;
    }

    public boolean isExtended() {
        BlockFace face = getFacing();
        Block block = getSide(face);
        return block instanceof BlockPistonHead && ((BlockPistonHead) block).getFacing() == face;
    }

    @Override
    public int onUpdate(int type) {
        if (type != 6 && type != 1) {
            return 0;
        } else {
            BlockEntity blockEntity = this.level.getBlockEntity(this);
            if (blockEntity instanceof BlockEntityPistonArm) {
                BlockEntityPistonArm arm = (BlockEntityPistonArm) blockEntity;
                boolean powered = this.isPowered();
                if (arm.powered != powered) {
                    this.level.getServer().getPluginManager().callEvent(new BlockPistonChangeEvent(this, powered ? 0 : 15, powered ? 15 : 0));
                    arm.powered = !arm.powered;
                    if (arm.chunk != null) {
                        arm.chunk.setDirty();
                    }
                }
            }

            return type;
        }
    }

    public static boolean canPush(Block block, BlockFace face, boolean destroyBlocks) {
        if (block.canBePushed() && block.getY() >= 0 && (face != BlockFace.DOWN || block.getY() != 0) &&
                block.getY() <= 255 && (face != BlockFace.UP || block.getY() != 255)) {
            if (!(block instanceof BlockPistonBase)) {

                if (block instanceof FloodableBlock) {
                    return destroyBlocks;
                }
            } else return !((BlockPistonBase) block).isExtended();
            return true;
        }
        return false;

    }

    public BlockFace getFacing() {
        return BlockFace.fromIndex(this.getDamage()).getOpposite();
    }

    private boolean isPowered() {
        BlockFace face = getFacing();

        for (BlockFace side : BlockFace.values()) {
            if (side != face && this.level.isSidePowered(this.asVector3i().getSide(side), side)) {
                return true;
            }
        }

        if (this.level.isSidePowered(this, BlockFace.DOWN)) {
            return true;
        } else {
            Vector3i pos = this.asVector3i().up();

            for (BlockFace side : BlockFace.values()) {
                if (side != BlockFace.DOWN && this.level.isSidePowered(pos.getSide(side), side)) {
                    return true;
                }
            }

            return false;
        }
    }

    private void checkState() {
        BlockFace facing = getFacing();
        boolean isPowered = this.isPowered();

        if (isPowered && !isExtended()) {
            if ((new BlocksCalculator(this.level, this, facing, true)).canMove()) {
                if (!this.doMove(true)) {
                    return;
                }

                this.getLevel().addLevelSoundEvent(this.asVector3f(), LevelSoundEventPacket.SOUND_PISTON_OUT);
            } else {
            }
        } else if (!isPowered && isExtended()) {
            //this.level.setBlock() TODO: set piston extension?

            if (this.sticky) {
                BlockPosition pos = this.add(facing.getXOffset() * 2, facing.getYOffset() * 2, facing.getZOffset() * 2);
                Block block = this.level.getBlock(pos);

                if (block.getId() == AIR) {
                    this.level.setBlock(this.asVector3i().getSide(facing), Block.get(AIR), true, true);
                }
                if (canPush(block, facing.getOpposite(), false) && (!(block instanceof FloodableBlock) || block.getId() == PISTON || block.getId() == STICKY_PISTON)) {
                    this.doMove(false);
                }
            } else {
                this.level.setBlock(asVector3i().getSide(facing), Block.get(AIR), true, false);
            }

            this.getLevel().addLevelSoundEvent(this.asVector3f(), LevelSoundEventPacket.SOUND_PISTON_IN);
        }
    }

    private boolean doMove(boolean extending) {
        Vector3i pos = this.asVector3i();
        BlockFace direction = getFacing();

        if (!extending) {
            this.level.setBlock(pos.getSide(direction), Block.get(AIR), true, false);
        }

        BlocksCalculator calculator = new BlocksCalculator(this.level, this, direction, extending);

        if (!calculator.canMove()) {
            return false;
        } else {
            List<Block> blocks = calculator.getBlocksToMove();

            List<Block> newBlocks = new ArrayList<>(blocks);

            List<Block> destroyBlocks = calculator.getBlocksToDestroy();
            BlockFace side = extending ? direction : direction.getOpposite();

            for (int i = destroyBlocks.size() - 1; i >= 0; --i) {
                Block block = destroyBlocks.get(i);
                this.level.useBreakOn(block);
            }

            for (int i = blocks.size() - 1; i >= 0; --i) {
                Block block = blocks.get(i);
                this.level.setBlock(block, Block.get(AIR));
                Vector3i newPos = block.asVector3i().getSide(side);

                //TODO: change this to block entity
                this.level.setBlock(newPos, newBlocks.get(i));
            }

            Vector3i pistonHead = pos.getSide(direction);

            if (extending) {
                //extension block entity
                this.level.setBlock(pistonHead, Block.get(PISTON_ARM_COLLISION, this.getDamage()));
            }

            return true;
        }
    }

    @Override
    public Item toItem() {
        return Item.get(id, 0);
    }

    public class BlocksCalculator {

        private final Level level;
        private final Vector3i pistonPos;
        private final Block blockToMove;
        private final BlockFace moveDirection;

        private final List<Block> toMove = new ArrayList<>();
        private final List<Block> toDestroy = new ArrayList<>();

        public BlocksCalculator(Level level, Block pos, BlockFace facing, boolean extending) {
            this.level = level;
            this.pistonPos = pos.asVector3i();

            if (extending) {
                this.moveDirection = facing;
                this.blockToMove = pos.getSide(facing);
            } else {
                this.moveDirection = facing.getOpposite();
                this.blockToMove = pos.getSide(facing, 2);
            }
        }

        public boolean canMove() {
            this.toMove.clear();
            this.toDestroy.clear();
            Block block = this.blockToMove;

            if (!canPush(block, this.moveDirection, false)) {
                if (block instanceof FloodableBlock) {
                    this.toDestroy.add(this.blockToMove);
                    return true;
                } else {
                    return false;
                }
            } else if (!this.addBlockLine(this.blockToMove)) {
                return false;
            } else {
                for (Block b : this.toMove) {
                    if (b.getId() == SLIME && !this.addBranchingBlocks(b)) {
                        return false;
                    }
                }

                return true;
            }
        }

        private boolean addBlockLine(Block origin) {
            Block block = origin.clone();

            if (block.getId() == AIR) {
                return true;
            } else if (!canPush(origin, this.moveDirection, false)) {
                return true;
            } else if (origin.equals(this.pistonPos)) {
                return true;
            } else if (this.toMove.contains(origin)) {
                return true;
            } else {
                int count = 1;

                if (count + this.toMove.size() > 12) {
                    return false;
                } else {
                    while (block.getId() == SLIME) {
                        block = origin.getSide(this.moveDirection.getOpposite(), count);

                        if (block.getId() == AIR || !canPush(block, this.moveDirection, false) || block.equals(this.pistonPos)) {
                            break;
                        }

                        ++count;

                        if (count + this.toMove.size() > 12) {
                            return false;
                        }
                    }

                    int blockCount = 0;

                    for (int step = count - 1; step >= 0; --step) {
                        this.toMove.add(block.getSide(this.moveDirection.getOpposite(), step));
                        ++blockCount;
                    }

                    int steps = 1;

                    while (true) {
                        Block nextBlock = block.getSide(this.moveDirection, steps);
                        int index = this.toMove.indexOf(nextBlock);

                        if (index > -1) {
                            this.reorderListAtCollision(blockCount, index);

                            for (int l = 0; l <= index + blockCount; ++l) {
                                Block b = this.toMove.get(l);

                                if (b.getId() == SLIME && !this.addBranchingBlocks(b)) {
                                    return false;
                                }
                            }

                            return true;
                        }

                        if (nextBlock.getId() == AIR) {
                            return true;
                        }

                        if (!canPush(nextBlock, this.moveDirection, true) || nextBlock.equals(this.pistonPos)) {
                            return false;
                        }

                        if (nextBlock instanceof FloodableBlock) {
                            this.toDestroy.add(nextBlock);
                            return true;
                        }

                        if (this.toMove.size() >= 12) {
                            return false;
                        }

                        this.toMove.add(nextBlock);
                        ++blockCount;
                        ++steps;
                    }
                }
            }
        }

        private void reorderListAtCollision(int count, int index) {
            List<Block> list = new ArrayList<>(this.toMove.subList(0, index));
            List<Block> list1 = new ArrayList<>(this.toMove.subList(this.toMove.size() - count, this.toMove.size()));
            List<Block> list2 = new ArrayList<>(this.toMove.subList(index, this.toMove.size() - count));
            this.toMove.clear();
            this.toMove.addAll(list);
            this.toMove.addAll(list1);
            this.toMove.addAll(list2);
        }

        private boolean addBranchingBlocks(Block block) {
            for (BlockFace face : BlockFace.values()) {
                if (face.getAxis() != this.moveDirection.getAxis() && !this.addBlockLine(block.getSide(face))) {
                    return false;
                }
            }

            return true;
        }

        public List<Block> getBlocksToMove() {
            return this.toMove;
        }

        public List<Block> getBlocksToDestroy() {
            return this.toDestroy;
        }
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getDamage() & 0x07);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
