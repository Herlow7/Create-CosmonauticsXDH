package dev.devce.rocketnautics.data.worldgen.noise;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;

public class NoiseGenSettings {
    public static final ResourceKey<NoiseGeneratorSettings> MOON_GENERATOR = register("moon");
    // reminders:
    // for noise router
    // | "ridges" is also known as "weirdness" and is used to further generate "ridges folded" which is then used as "ridges"
    // | confusing? yep. The goal is a specific subdivision structure in the "ridges folded" noise.
    public static void bootstrap(BootstrapContext<NoiseGeneratorSettings> context) {
        // general ideas behind moon worldgen:
        // continents determines highlands or maria. Major influence on average world height.
        // ridges determines appearance of special biomes -- very positive is unnatural basalt spikes, very negative is volcanic domes?
        // ridges folded determines some terrain detail. Minor, localized influence of average world height; primarily influences the highlands to form rugged terrain
        // depth is stretched massively along one axis, primarily influences the maria to form wrinkle ridges
        // erosion determines depth of regolith. Influenced by continents.
        // utilize canyon carvers with extreme values to generate lunar rilles
        // utilize a custom carver to generate craters
//        context.register(MOON_GENERATOR, new NoiseGeneratorSettings(
//                NoiseSettings.create(-64, 384, 1, 2),
//
//        ));
    }

    private static ResourceKey<NoiseGeneratorSettings> register(String name) {
        return ResourceKey.create(Registries.NOISE_SETTINGS, RocketNautics.path(name));
    }
}
