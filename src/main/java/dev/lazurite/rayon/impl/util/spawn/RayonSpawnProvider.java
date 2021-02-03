package dev.lazurite.rayon.impl.util.spawn;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import dev.lazurite.rayon.Rayon;
import dev.lazurite.rayon.impl.physics.body.EntityRigidBody;
import dev.lazurite.rayon.impl.util.math.QuaternionHelper;
import dev.lazurite.rayon.impl.util.math.VectorHelper;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class RayonSpawnProvider {
    public static Packet<?> get (Entity entity) {
        if (!EntityRigidBody.is(entity)) {
            throw new RayonSpawnHandler.RayonSpawnException(entity.getEntityName() + " is not registered!");
        }

        PacketByteBuf buf = PacketByteBufs.create();
        EntityRigidBody body = Rayon.ENTITY.get(entity);

        buf.writeVarInt(Registry.ENTITY_TYPE.getRawId(entity.getType()));
        buf.writeInt(entity.getEntityId());
        buf.writeUuid(entity.getUuid());
        body.setPhysicsLocation(VectorHelper.vec3dToVector3f(entity.getPos().add(0, body.boundingBox(new BoundingBox()).getYExtent() / 2.0, 0)));

        QuaternionHelper.toBuffer(buf, body.getPhysicsRotation(new Quaternion()));
        VectorHelper.toBuffer(buf, body.getPhysicsLocation(new Vector3f()));
        VectorHelper.toBuffer(buf, body.getLinearVelocity(new Vector3f()));
        VectorHelper.toBuffer(buf, body.getAngularVelocity(new Vector3f()));

        return ServerPlayNetworking.createS2CPacket(new Identifier(Rayon.MODID, "rayon_spawn_s2c_packet"), buf);
    }
}
