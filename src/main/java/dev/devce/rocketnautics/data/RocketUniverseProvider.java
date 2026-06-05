package dev.devce.rocketnautics.data;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.orbit.universe.StandardUniverseProvider;
import dev.devce.rocketnautics.content.orbit.universe.builder.PlanetDefinitionBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RocketUniverseProvider extends UniverseDataProvider{
    public RocketUniverseProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, RocketNautics.MODID, registries);
    }

    @Override
    protected void addPlanets(Consumer<PlanetDefinitionBuilder> writer) {
        writer.accept(StandardUniverseProvider.sol());
        writer.accept(StandardUniverseProvider.overworld());
        writer.accept(StandardUniverseProvider.moon());
        writer.accept(StandardUniverseProvider.mars());
        writer.accept(StandardUniverseProvider.gasGiant());
        writer.accept(StandardUniverseProvider.iceWorld());
    }
}
