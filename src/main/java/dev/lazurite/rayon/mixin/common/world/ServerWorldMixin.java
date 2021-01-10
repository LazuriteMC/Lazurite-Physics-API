package dev.lazurite.rayon.mixin.common.world;

import dev.lazurite.rayon.physics.world.MinecraftDynamicsWorld;
import dev.lazurite.rayon.mixin.common.IntegratedServerMixin;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * This simply calls {@link MinecraftDynamicsWorld#step} each time the server ticks.
 * @see MinecraftDynamicsWorld#step
 * @see IntegratedServerMixin
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {
    /* sadge */
    private ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DimensionType dimensionType, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, dimensionType, profiler, isClient, debugWorld, seed);
    }

    @Inject(
            method = "tick(Ljava/util/function/BooleanSupplier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerChunkManager;tick(Ljava/util/function/BooleanSupplier;)V",
                    shift = At.Shift.AFTER
            )
    )
    public void tick(BooleanSupplier shouldKeepTicking, CallbackInfo info) {
        getProfiler().swap("physicsSimulation");
        MinecraftDynamicsWorld.get((ServerWorld) (Object) this).step(shouldKeepTicking);
    }
}