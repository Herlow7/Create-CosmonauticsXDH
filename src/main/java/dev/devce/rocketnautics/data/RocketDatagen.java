package dev.devce.rocketnautics.data;

import com.tterrag.registrate.providers.ProviderType;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.data.recipe.*;
import dev.devce.rocketnautics.data.worldgen.*;
import dev.devce.rocketnautics.data.worldgen.noise.NoiseData;
import dev.devce.rocketnautics.data.worldgen.noise.NoiseRouterData;
import dev.devce.rocketnautics.data.worldgen.noise.NoiseGenSettings;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.concurrent.CompletableFuture;

public class RocketDatagen {

    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();

        event.addProvider(new RocketCrushingRecipeGen(output, registries));
        event.addProvider(new RocketMechanicalCraftingRecipeGen(output, registries));
        event.addProvider(new RocketMillingRecipeGen(output, registries));
        event.addProvider(new RocketMixingRecipeGen(output, registries));
        event.addProvider(new RocketPressingRecipeGen(output, registries));
        event.addProvider(new RocketStandardRecipeGen(output, registries));
        event.addProvider(new RocketWashingRecipeGen(output, registries));

        event.addProvider(new RocketBiomeTagsProvider(output, registries, event.getExistingFileHelper()));
        RocketNautics.getRegistrate().addDataGenerator(ProviderType.BLOCK_TAGS, RocketBlockTagsProvider::addTags);

        RegistrySetBuilder registry = new RegistrySetBuilder();
        registry.add(Registries.DIMENSION_TYPE, DimensionTypes::bootstrap);
        registry.add(Registries.LEVEL_STEM, LevelStems::bootstrap);
        registry.add(Registries.BIOME, RocketBiomes::bootstrap);
        registry.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, BiomeModifiers::bootstrap);
        registry.add(Registries.PLACED_FEATURE, PlacedFeatures::bootstrap);
        registry.add(Registries.CONFIGURED_FEATURE, ConfiguredFeatures::bootstrap);
        registry.add(Registries.NOISE_SETTINGS, NoiseGenSettings::bootstrap);
        registry.add(Registries.NOISE, NoiseData::bootstrap);
        registry.add(Registries.DENSITY_FUNCTION, NoiseRouterData::bootstrap);
        registry.add(Registries.CONFIGURED_CARVER, Carvers::bootstrap);
        event.createDatapackRegistryObjects(registry);
    }
}
