package dev.lazurite.rayon.api.event;

import dev.lazurite.rayon.physics.body.block.BlockRigidBody;
import dev.lazurite.rayon.physics.body.entity.EntityRigidBody;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Callbacks for when a {@link EntityRigidBody} on both the client and the server
 * collides with either another {@link EntityRigidBody} or a {@link BlockRigidBody}.
 * @since 1.0.0
 * @see EntityRigidBody#step(float)
 */
public final class EntityBodyCollisionEvent {
    public static final Event<BlockCollision> BLOCK_COLLISION = EventFactory.createArrayBacked(BlockCollision.class, (callbacks) -> (body) -> {
        for (BlockCollision event : callbacks) {
            event.onBlockCollision(body);
        }
    });

    public static final Event<EntityCollision> ENTITY_COLLISION = EventFactory.createArrayBacked(EntityCollision.class, (callbacks) -> (body) -> {
        for (EntityCollision event : callbacks) {
            event.onEntityCollision(body);
        }
    });

    private EntityBodyCollisionEvent() {
    }

    @FunctionalInterface
    public interface BlockCollision {
        void onBlockCollision(BlockRigidBody body);
    }

    @FunctionalInterface
    public interface EntityCollision {
        void onEntityCollision(EntityRigidBody body);
    }
}
