package dev.devce.rocketnautics.data;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.data.worldgen.RocketBiomes;
import dev.devce.rocketnautics.registry.RocketTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class RocketBiomeTagsProvider extends BiomeTagsProvider {
    protected RocketBiomeTagsProvider(PackOutput p_256596_, CompletableFuture<HolderLookup.Provider> p_256513_, @Nullable ExistingFileHelper existingFileHelper) {
        super(p_256596_, p_256513_, RocketNautics.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NonNull Provider provider) {
        tag(RocketTags.BiomeTags.LUNAR_HIGHLANDS.tag)
                .addOptional(RocketBiomes.LUNAR_HIGHLANDS.location())
                .addOptional(RocketBiomes.LUNAR_AGED_CHASM.location())
                .addOptional(RocketBiomes.LUNAR_AGED_SPIKES.location());
        tag(RocketTags.BiomeTags.LUNAR_MARIA.tag)
                .addOptional(RocketBiomes.LUNAR_MARIA.location())
                .addOptional(RocketBiomes.LUNAR_BASALT_CHASM.location())
                .addOptional(RocketBiomes.LUNAR_BASALT_SPIKES.location());
        tag(RocketTags.BiomeTags.LUNAR_CHASM.tag)
                .addOptional(RocketBiomes.LUNAR_AGED_CHASM.location())
                .addOptional(RocketBiomes.LUNAR_BASALT_CHASM.location());
    }
}
