package dev.devce.rocketnautics.data.worldgen;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

import java.util.List;
import java.util.Optional;

public class LevelStems {
    public static final ResourceKey<LevelStem> DEEP_SPACE = register("deep_space");

    public static void bootstrap(BootstrapContext<LevelStem> context) {
        HolderGetter<DimensionType> types = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        context.register(DEEP_SPACE, new LevelStem(
                types.getOrThrow(DimensionTypes.DEEP_SPACE),
                new FlatLevelSource(new FlatLevelGeneratorSettings(
                        Optional.empty(),
                        biomes.getOrThrow(Biomes.THE_VOID),
                        List.of()
                ))
        ));
    }

    private static ResourceKey<LevelStem> register(String name) {
        return ResourceKey.create(Registries.LEVEL_STEM, RocketNautics.path(name));
    }
}
