package dev.devce.rocketnautics.data.worldgen;

import dev.devce.rocketnautics.data.worldgen.biome.OverworldMoonBiomes;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class BiomeData {
    public static ResourceKey<Biome> LUNAR_MARIA = register("lunar_maria");

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        context.register(LUNAR_MARIA, OverworldMoonBiomes.lunarMaria(features, carvers));
    }

    private static ResourceKey<Biome> register(String p_48229_) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(p_48229_));
    }
}
