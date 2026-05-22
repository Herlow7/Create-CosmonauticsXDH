package dev.devce.rocketnautics.data.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class BiomeData {
    public static void bootstrap(BootstrapContext<Biome> p_321862_) {
        HolderGetter<PlacedFeature> features = p_321862_.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = p_321862_.lookup(Registries.CONFIGURED_CARVER);
    }
}
