package dev.lazurite.rayon.impl.physics.body;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import dev.lazurite.rayon.api.builder.RigidBodyBuilder;
import dev.lazurite.rayon.api.builder.RigidBodyRegistry;
import dev.lazurite.rayon.api.event.EntityBodyCollisionEvent;
import dev.lazurite.rayon.api.event.EntityBodyStepEvents;
import dev.lazurite.rayon.Rayon;
import dev.lazurite.rayon.api.shape.factory.EntityShapeFactory;
import dev.lazurite.rayon.impl.physics.body.type.DebuggableBody;
import dev.lazurite.rayon.impl.physics.body.type.SteppableBody;
import dev.lazurite.rayon.impl.physics.helper.AirHelper;
import dev.lazurite.rayon.impl.physics.helper.math.QuaternionHelper;
import dev.lazurite.rayon.impl.physics.helper.math.VectorHelper;
import dev.lazurite.rayon.impl.physics.world.MinecraftDynamicsWorld;
import dev.lazurite.rayon.impl.util.DebugManager;
import dev.lazurite.rayon.impl.util.config.Config;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.tick.CommonTickingComponent;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * {@link EntityRigidBody} is the mainsail of Rayon. It's currently the only component that you're
 * able to register to an entity type using {@link RigidBodyBuilder} and {@link RigidBodyRegistry}.
 * Not only is it a CCA component, but it also represents a bullet {@link PhysicsRigidBody}. In
 * this way it can be directly added to a {@link MinecraftDynamicsWorld}.<br><br>
 *
 * Additionally, {@link EntityRigidBody} implements several interfaces which allow for more
 * functionality. {@link SteppableBody} allows the rigid body to be <i>stepped</i> during every
 * step of the {@link MinecraftDynamicsWorld}. {@link CommonTickingComponent} allows this class
 * to tick each time the provider ticks. {@link AutoSyncedComponent} is what is responsible for
 * sending packets containing position, velocity, orientation, etc. from the server to the client.<br><br>
 *
 * From an API user's standpoint, the only time you'll need to interact with this class is to
 * retrieve information from it (like position, velocity, orientation, etc). Otherwise, you can
 * modify it's behavior by registering an event in {@link EntityBodyCollisionEvent} or
 * {@link EntityBodyStepEvents}.<br><br>
 *
 * @see MinecraftDynamicsWorld
 * @see EntityBodyStepEvents
 * @see EntityBodyCollisionEvent
 */
public class EntityRigidBody extends PhysicsRigidBody implements SteppableBody, DebuggableBody, ComponentV3, CommonTickingComponent, AutoSyncedComponent {
    private final Quaternion prevRotation = new Quaternion();
    private final Quaternion tickRotation = new Quaternion();
    private final MinecraftDynamicsWorld dynamicsWorld;
    private final Entity entity;
    private float dragCoefficient;
    private boolean noclip;

    public EntityRigidBody(Entity entity, EntityShapeFactory shape, float mass, float dragCoefficient, float friction, float restitution) {
        super(shape.create(entity), mass);
        this.entity = entity;
        this.dragCoefficient = dragCoefficient;
        this.setFriction(friction);
        this.setRestitution(restitution);
        this.dynamicsWorld = Rayon.WORLD.get(entity.getEntityWorld());
        this.prevRotation.set(getPhysicsRotation(new Quaternion()));
//        this.setDeactivationTime(30);
        this.dynamicsWorld.addCollisionObject(this);
    }

    public static boolean is(Entity entity) {
        return Rayon.RIGID_BODY.maybeGet(entity).isPresent();
    }

    /**
     * Somewhat of a parallel to the traditional <i>tick</i>, this method is called every
     * time the physics simulation advances another step. The physics simulation can, but
     * doesn't always, run at Minecraft's traditional rate of 20 tps. The simulation can step
     * up to the same rate as the renderer. So don't rely on this method being called at a
     * constant rate, or any specific rate for that matter.<br><br>
     *
     * Also, it's important to note that mainly just physics related calls should be included here.
     * One example would be a call to {@link EntityRigidBody#applyCentralForce(Vector3f)} which
     * is best to do every step instead of every tick. The reason is that all forces to rigid bodies
     * are cleared after every step.<br><br>
     *
     * You can gain access to this method by registering an event handler in {@link EntityBodyStepEvents}.<br><br>
     * @param delta the amount of seconds since the last step
     * @see MinecraftDynamicsWorld#step(BooleanSupplier)
     * @see SteppableBody
     */
    @Override
    public void step(float delta) {
        /* Invoke all registered start step events */
        EntityBodyStepEvents.START_ENTITY_STEP.invoker().onStartStep(this, delta);

        /* Apply air resistance */
        if (Config.getInstance().getGlobal().isAirResistanceEnabled()) {
            applyCentralForce(AirHelper.getSimpleForce(this));
        }

        /* Invoke all registered end step events */
        EntityBodyStepEvents.END_ENTITY_STEP.invoker().onEndStep(this, delta);
    }

    /**
     * A traditional tick method, this is simply called each time the entity
     * provider is ticked. It's responsible for doing a variety of things that
     * either don't require being called every physics step or <i>shouldn't</i> be
     * called every physics step.
     */
    @Override
    public void tick() {
        if (!dynamicsWorld.getWorld().isClient()) {
            Rayon.RIGID_BODY.sync(entity);
        }

        prevRotation.set(tickRotation);
        tickRotation.set(getPhysicsRotation(new Quaternion()));

        Vector3f position = getPhysicsLocation(new Vector3f());
        entity.updatePosition(position.x, position.y - boundingBox(new BoundingBox()).getYExtent() / 2.0f, position.z);

        entity.yaw = QuaternionHelper.getYaw(tickRotation);
        entity.pitch = QuaternionHelper.getPitch(tickRotation);
    }

    @Override
    public Vector3f getOutlineColor() {
        return new Vector3f(1.0f, 0.6f, 0);
    }

    @Override
    public DebugManager.DebugLayer getDebugLayer() {
        return DebugManager.DebugLayer.ENTITY;
    }

    public void setDragCoefficient(float dragCoefficient) {
        this.dragCoefficient = dragCoefficient;
    }

    public void setNoClip(boolean noclip) {
        this.noclip = noclip;
    }

    public float getDragCoefficient() {
        return this.dragCoefficient;
    }

    public boolean isNoClipEnabled() {
        return this.noclip;
    }

    public Quaternion getPhysicsRotation(Quaternion quaternion, float delta) {
        quaternion.set(QuaternionHelper.slerp(prevRotation, tickRotation, delta));
        return quaternion;
    }

    public Entity getEntity() {
        return entity;
    }

    public MinecraftDynamicsWorld getDynamicsWorld() {
        return dynamicsWorld;
    }

    @Override
    public void applySyncPacket(PacketByteBuf buf) {
        setPhysicsRotation(QuaternionHelper.fromBuffer(buf));
        setPhysicsLocation(VectorHelper.fromBuffer(buf));
        setLinearVelocity(VectorHelper.fromBuffer(buf));
        setAngularVelocity(VectorHelper.fromBuffer(buf));
        setDragCoefficient(buf.readFloat());
    }

    @Override
    public void writeSyncPacket(PacketByteBuf buf, ServerPlayerEntity recipient) {
        QuaternionHelper.toBuffer(buf, getPhysicsRotation(new Quaternion()));
        VectorHelper.toBuffer(buf, getPhysicsLocation(new Vector3f()));
        VectorHelper.toBuffer(buf, getLinearVelocity(new Vector3f()));
        VectorHelper.toBuffer(buf, getAngularVelocity(new Vector3f()));
        buf.writeFloat(getDragCoefficient());
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        setPhysicsRotation(QuaternionHelper.fromTag(tag.getCompound("orientation")));
        setPhysicsLocation(VectorHelper.fromTag(tag.getCompound("position")));
        setLinearVelocity(VectorHelper.fromTag(tag.getCompound("linear_velocity")));
        setAngularVelocity(VectorHelper.fromTag(tag.getCompound("angular_velocity")));
        setDragCoefficient(tag.getFloat("drag_coefficient"));
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.put("orientation", QuaternionHelper.toTag(getPhysicsRotation(new Quaternion())));
        tag.put("position", VectorHelper.toTag(getPhysicsLocation(new Vector3f())));
        tag.put("linear_velocity", VectorHelper.toTag(getLinearVelocity(new Vector3f())));
        tag.put("angular_velocity", VectorHelper.toTag(getAngularVelocity(new Vector3f())));
        tag.putFloat("drag_coefficient", getDragCoefficient());
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s[id=%d, shape='%s', mass=%f, drag=%f, pos=%s, vel=%s]", getClass().getSimpleName(), getEntity().getEntityId(), getCollisionShape().getClass().getSimpleName(), getMass(), getDragCoefficient(), getPhysicsLocation(new Vector3f()).toString(), getLinearVelocity(new Vector3f()).toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EntityRigidBody) {
            return ((EntityRigidBody) obj).getEntity().equals(getEntity());
        }

        return false;
    }
}