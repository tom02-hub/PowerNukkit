package cn.nukkit.utils.spawners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.BaseEntity;
import cn.nukkit.entity.passive.EntityOcelot;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.generator.biome.Biome;
import cn.nukkit.utils.AbstractEntitySpawner;
import cn.nukkit.utils.EntityUtils;
import cn.nukkit.utils.Spawner;
import cn.nukkit.utils.SpawnResult;

public class OcelotSpawner extends AbstractEntitySpawner {

    public OcelotSpawner(Spawner spawnTask) {
        super(spawnTask);
    }

    public SpawnResult spawn(Player player, Position pos, Level level) {
        SpawnResult result = SpawnResult.OK;

        if (EntityUtils.rand(1, 3) == 1) {
            return SpawnResult.SPAWN_DENIED;
        }

        final int blockId = level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z);

        if (level.getBiomeId((int) pos.x, (int) pos.z) != Biome.JUNGLE) {
            result = SpawnResult.WRONG_BIOME;
        } else if (level.getName().equals("nether") || level.getName().equals("end")) {
            result = SpawnResult.WRONG_BIOME;
        } else if (blockId != Block.GRASS && blockId != Block.LEAVES) {
            result = SpawnResult.WRONG_BLOCK;
        } else if (pos.y > 127 || pos.y < 1 || blockId == Block.AIR) {
            result = SpawnResult.POSITION_MISMATCH;
        } else {
            BaseEntity entity = this.spawnTask.createEntity(getEntityName(), pos.add(0, 1, 0));
            if (EntityUtils.rand(0, 500) > 480) {
                entity.setBaby(true);
            }
        }

        return result;
    }

    @Override
    public final int getEntityNetworkId() {
        return EntityOcelot.NETWORK_ID;
    }

    @Override
    public final String getEntityName() {
        return "Ocelot";
    }
}
