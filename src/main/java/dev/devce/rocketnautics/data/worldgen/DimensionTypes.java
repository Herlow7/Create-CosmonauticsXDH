package dev.devce.rocketnautics.data.worldgen;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.OptionalLong;

public class DimensionTypes {
    public static final ResourceKey<DimensionType> DEEP_SPACE = register("deep_space");
    public static final ResourceKey<DimensionType> MOON = register("moon");
    public static final ResourceLocation DEEP_SPACE_EFFECTS = RocketNautics.path("deep_space");
    public static final ResourceLocation MOON_EFFECTS = RocketNautics.path("moon");
    public static void bootstrap(BootstrapContext<DimensionType> context) {
        context.register(
                DEEP_SPACE, new DimensionType(
                        OptionalLong.empty(),
                        true,
                        true,
                        false,
                        false,
                        1.0,
                        false,
                        false,
                        -64,
                        384,
                        384,
                        BlockTags.INFINIBURN_OVERWORLD,
                        DEEP_SPACE_EFFECTS,
                        0.0f,
                        new DimensionType.MonsterSettings(
                                true,
                                false,
                                ConstantInt.of(0),
                                0
                        )
                )
        );
        context.register(
                MOON, new DimensionType(
                        OptionalLong.empty(),
                        true,
                        false,
                        false,
                        true,
                        1.0,
                        false,
                        true,
                        -64,
                        384,
                        384,
                        BlockTags.INFINIBURN_OVERWORLD,
                        MOON_EFFECTS,
                        0.0f,
                        new DimensionType.MonsterSettings(
                                true,
                                false,
                                UniformInt.of(0, 7),
                                0
                        )
                )
        );
    }

    private static ResourceKey<DimensionType> register(String name) {
        return ResourceKey.create(Registries.DIMENSION_TYPE, RocketNautics.path(name));
    }
}
