package dev.devce.rocketnautics.content;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * @see dev.devce.rocketnautics.data.worldgen.LevelStems
 */
public class RocketDimensions {
    public static final ResourceKey<Level> DEEP_SPACE = ResourceKey.create(Registries.DIMENSION, RocketNautics.path("deep_space"));
    public static final ResourceKey<Level> MOON = ResourceKey.create(Registries.DIMENSION, RocketNautics.path("moon"));
}
