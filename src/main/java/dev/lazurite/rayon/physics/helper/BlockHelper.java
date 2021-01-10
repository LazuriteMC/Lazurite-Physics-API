package dev.lazurite.rayon.physics.helper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.lazurite.rayon.physics.body.entity.EntityRigidBody;
import dev.lazurite.rayon.physics.helper.math.VectorHelper;
import dev.lazurite.rayon.physics.body.block.BlockRigidBody;
import dev.lazurite.rayon.physics.shape.BoundingBoxShape;
import dev.lazurite.rayon.physics.world.MinecraftDynamicsWorld;
import dev.lazurite.rayon.util.config.Config;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import javax.vecmath.Vector3f;
import java.util.*;

/**
 * This class is used primarily by {@link MinecraftDynamicsWorld} in order
 * to load and unload blocks from the simulation. The reason not every block is loaded is
 * because it is too resource intensive to track thousands of blocks within the physics
 * simulation. Instead, only a set amount are available within the world at a time. This
 * value is controlled in the {@link Config} class as the blockDistance
 * integer value.
 * @see MinecraftDynamicsWorld
 * @see Config
 */
public class BlockHelper {
    private final MinecraftDynamicsWorld dynamicsWorld;
    private final List<BlockRigidBody> toKeep;

    public BlockHelper(MinecraftDynamicsWorld dynamicsWorld) {
        this.dynamicsWorld = dynamicsWorld;
        this.toKeep = Lists.newArrayList();
    }

    /**
     * Load every block within a set distance from the given entities. Said
     * distance is defined earlier during execution and converted into a
     * {@link Box} area parameter.
     *
     * @param dynamicBodyEntities the {@link List} of {@link EntityRigidBody} objects
     * @param area                the {@link Box} area around the entities to search for blocks within
     * @see BlockHelper#load(EntityRigidBody, Box)
     */
    public void load(List<EntityRigidBody> dynamicBodyEntities, Box area) {
        dynamicBodyEntities.forEach(body -> load(body, area.offset(VectorHelper.vector3fToVec3d(body.getCenterOfMassPosition(new Vector3f())))));
        purge();
    }

    /**
     * Loads an individual entity's block area into the physics simulation. This
     * is also where each block's {@link BlockRigidBody} object is instantiated
     * and properties such as position, shape, friction, etc. are applied here.
     *
     * @param dynamicBodyEntity the {@link EntityRigidBody} to load blocks around
     * @param area              the {@link Box} area around the entity to search for blocks within
     * @see BlockHelper#load(List, Box)
     */
    public void load(EntityRigidBody dynamicBodyEntity, Box area) {
        World world = dynamicsWorld.getWorld();
        Entity entity = dynamicBodyEntity.getEntity();
        Map<BlockPos, BlockState> blockList = getBlockList(world, area);
        BlockView blockView = world.getChunkManager().getChunk(entity.chunkX, entity.chunkZ);

        blockList.forEach((blockPos, blockState) -> {
            float friction = 1.5f;

            /* Will be replaced with json code in a future version */
            if (blockState.getBlock() instanceof IceBlock) {
                friction = 0.05F;
            } else if (!(blockState.getBlock() instanceof HoneyBlock) && !(blockState.getBlock() instanceof SlimeBlock) && !(blockState.getBlock() instanceof SoulSandBlock)) {
                friction = 0.9F;
            }

            /* Check if block is solid or not */
            if (!blockState.getBlock().canMobSpawnInside()) {
                VoxelShape vox = blockState.getCollisionShape(blockView, blockPos);

                if (!vox.isEmpty()) {
                    BlockRigidBody body = BlockRigidBody.create(blockPos, blockState, new BoundingBoxShape(vox.getBoundingBox()), friction);

                    /* Check if the block isn't already in the dynamics world */
                    if (!dynamicsWorld.getCollisionObjectArray().contains(body)) {
                        dynamicsWorld.addRigidBody(body);
                    }

                    toKeep.add(body);
                }
            }
        });
    }

    /**
     * Prune out any unnecessary blocks from the world during each call
     * to {@link MinecraftDynamicsWorld#step}. The purpose is to prevent
     * any trailing or residual blocks from being left over from a
     * previous {@link BlockHelper#load(List, Box)} call.
     * <b>Note:</b> This method should only be called after every entity
     * has been passed through the loading process. Otherwise, blocks will
     * be removed from the simulation prematurely and cause you a headache.
     *
     * @see BlockHelper#load(List, Box)
     */
    public void purge() {
        List<BlockRigidBody> toRemove = Lists.newArrayList();

        dynamicsWorld.getCollisionObjectArray().forEach(body -> {
            if (body instanceof BlockRigidBody) {
                BlockRigidBody block = (BlockRigidBody) body;

                if (!toKeep.contains(block)) {
                    toRemove.add(block);
                }
            }
        });

        toRemove.forEach(dynamicsWorld::removeRigidBody);
        toKeep.clear();
    }

    /**
     * Simply returns a basic {@link Map} of {@link BlockPos} and {@link BlockState}
     * objects representing the blocks that make up the {@link Box} area parameter.
     *
     * @param world the {@link World} to retrieve block info from
     * @param area  the {@link Box} area within the world to retrieve block info from
     * @return the {@link Map} of {@link BlockPos} and {@link BlockState} objects
     * @see BlockHelper#load(EntityRigidBody, Box)
     */
    public static Map<BlockPos, BlockState> getBlockList(World world, Box area) {
        Map<BlockPos, BlockState> map = Maps.newHashMap();

        for (int i = (int) area.minX; i < area.maxX; i++) {
            for (int j = (int) area.minY; j < area.maxY; j++) {
                for (int k = (int) area.minZ; k < area.maxZ; k++) {
                    BlockPos blockPos = new BlockPos(i, j, k);
                    BlockState blockState = world.getBlockState(blockPos);
                    map.put(blockPos, blockState);
                }
            }
        }

        return map;
    }
}
