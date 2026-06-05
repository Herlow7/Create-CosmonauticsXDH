package dev.devce.rocketnautics.registry;

import com.mojang.serialization.MapCodec;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.world.NoiseThreshold3dConditionSource;
import dev.devce.rocketnautics.content.world.StretchedNoise;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RocketSurfaceRules {
    public static final DeferredRegister<MapCodec<? extends SurfaceRules.ConditionSource>> CONDITIONS = DeferredRegister.create(Registries.MATERIAL_CONDITION, RocketNautics.MODID);

    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<NoiseThreshold3dConditionSource>> NOISE_THRESHOLD_3D = CONDITIONS.register("noise_condition_3d", () -> NoiseThreshold3dConditionSource.DATA_CODEC);

    public static void register(IEventBus bus) {
        CONDITIONS.register(bus);
    }

}
