package dev.devce.rocketnautics.content.orbit.universe.builder;

import dev.devce.rocketnautics.api.orbit.ColorFlags;
import dev.devce.rocketnautics.content.orbit.universe.DeepSpaceTextureDefinition;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class BiomeSamplingTextureBuilder {
    public final List<DeepSpaceTextureDefinition.BiomeSampleDriven.ColorEntry> finishedEntries = new ObjectArrayList<>();
    public List<ResourceKey<Biome>> buildingBiomes = new ObjectArrayList<>();
    public List<TagKey<Biome>> buildingTags = new ObjectArrayList<>();
    public int buildingPrio = 0;
    public EnumSet<ColorFlags> buildingFlags = ColorFlags.empty();

    public BiomeSamplingTextureBuilder addMatchingBiome(ResourceKey<Biome> key) {
        buildingBiomes.add(key);
        return this;
    }

    @SafeVarargs
    public final BiomeSamplingTextureBuilder addMatchingBiome(ResourceKey<Biome>... keys) {
        buildingBiomes.addAll(Arrays.asList(keys));
        return this;
    }

    public BiomeSamplingTextureBuilder addMatchingTag(TagKey<Biome> tag) {
        buildingTags.add(tag);
        return this;
    }

    @SafeVarargs
    public final BiomeSamplingTextureBuilder addMatchingTag(TagKey<Biome>... tags) {
        buildingTags.addAll(Arrays.asList(tags));
        return this;
    }

    public BiomeSamplingTextureBuilder setPriority(int prio) {
        buildingPrio = prio;
        return this;
    }

    public BiomeSamplingTextureBuilder addFlag(ColorFlags flag) {
        buildingFlags.add(flag);
        return this;
    }

    public BiomeSamplingTextureBuilder addFlag(ColorFlags... flag) {
        buildingFlags.addAll(Arrays.asList(flag));
        return this;
    }

    public BiomeSamplingTextureBuilder buildEntryToColor(int r, int g, int b) {
        finishedEntries.add(new DeepSpaceTextureDefinition.BiomeSampleDriven.ColorEntry(buildingBiomes, buildingTags, buildingPrio, (byte) r, (byte) g, (byte) b, buildingFlags));
        buildingBiomes = new ObjectArrayList<>();
        buildingTags = new ObjectArrayList<>();
        buildingPrio = 0;
        buildingFlags = ColorFlags.empty();
        return this;
    }

    public DeepSpaceTextureDefinition.BiomeSampleDriven build(int fallbackR, int fallbackG, int fallbackB) {
        return new DeepSpaceTextureDefinition.BiomeSampleDriven(finishedEntries.stream().sorted(Comparator.comparingInt(DeepSpaceTextureDefinition.BiomeSampleDriven.ColorEntry::prio).reversed()).toList(),
                (byte) fallbackR, (byte) fallbackG, (byte) fallbackB);
    }
}
