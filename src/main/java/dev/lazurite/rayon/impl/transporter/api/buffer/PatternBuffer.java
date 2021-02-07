package dev.lazurite.rayon.impl.transporter.api.buffer;

import dev.lazurite.rayon.impl.transporter.api.pattern.Pattern;
import dev.lazurite.rayon.impl.transporter.api.pattern.TypedPattern;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public interface PatternBuffer<T> {
    static PatternBuffer<BlockPos> getBlockBuffer(World world) {
        return ((BufferStorage) world).getBlockBuffer();
    }

    static PatternBuffer<Entity> getEntityBuffer(World world) {
        return ((BufferStorage) world).getEntityBuffer();
    }

    static PatternBuffer<Item> getItemBuffer(World world) {
        return ((BufferStorage) world).getItemBuffer();
    }

    Pattern pop(T identifier);
    Pattern get(T identifier);
    List<TypedPattern<T>> getAll();
    boolean contains(T key);
    int size();
}
