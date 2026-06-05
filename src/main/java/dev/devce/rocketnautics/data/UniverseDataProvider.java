package dev.devce.rocketnautics.data;

import dev.devce.rocketnautics.content.orbit.universe.UniverseLoader;
import dev.devce.rocketnautics.content.orbit.universe.builder.PlanetDefinitionBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class UniverseDataProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;
    private final String modID;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public UniverseDataProvider(PackOutput output, String modID, CompletableFuture<HolderLookup.Provider> registries) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, modID + "/" + UniverseLoader.DIRECTORY);
        this.modID = modID;
        this.registries = registries;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        return this.registries.thenCompose(p_323115_ -> {
            Set<ResourceLocation> set = new HashSet<>();
            List<CompletableFuture<?>> list = new ArrayList<>();
            Consumer<PlanetDefinitionBuilder> consumer = planet -> {
                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("", planet.name);
                if (!set.add(loc)) {
                    throw new IllegalStateException("Duplicate planet definition " + loc);
                } else {
                    Path path = this.pathProvider.json(loc);
                    list.add(DataProvider.saveStable(output, p_323115_, PlanetDefinitionBuilder.CODEC, planet, path));
                }
            };
            addPlanets(consumer);

            return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
        });
    }

    protected abstract void addPlanets(Consumer<PlanetDefinitionBuilder> writer);

    @Override
    public @NotNull String getName() {
        return modID + "'s Universe Provider";
    }
}
