package dev.devce.rocketnautics.data.recipe;

import com.simibubi.create.api.data.recipe.MillingRecipeGen;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.registry.RocketBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static dev.devce.rocketnautics.registry.RocketBlocks.LUNAR_LOOSE_REGOLITH;

public class RocketMillingRecipeGen extends MillingRecipeGen {
    GeneratedRecipe LUNAR_AGED_BASALT = create(() -> RocketBlocks.LUNAR_AGED_BASALT, b -> b.duration(250).output(RocketBlocks.LUNAR_SHATTERED_REGOLITH));
    GeneratedRecipe LUNAR_REGOLITH = create(() -> RocketBlocks.LUNAR_REGOLITH, b -> b.duration(250).output(RocketBlocks.LUNAR_SHATTERED_REGOLITH));
    GeneratedRecipe LUNAR_REGOLITH_BRICK = create(() -> RocketBlocks.LUNAR_REGOLITH_BRICK, b -> b.duration(250).output(RocketBlocks.LUNAR_SHATTERED_REGOLITH));
    GeneratedRecipe LUNAR_SHATTERED_REGOLITH = create(() -> RocketBlocks.LUNAR_SHATTERED_REGOLITH, b -> b.duration(250).output(LUNAR_LOOSE_REGOLITH));

    public RocketMillingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, RocketNautics.MODID);
    }

    @Override
    protected GeneratedRecipe create(Supplier<ItemLike> singleIngredient, UnaryOperator<StandardProcessingRecipe.Builder<MillingRecipe>> transform) {
        return super.create(RocketNautics.MODID, singleIngredient, transform);
    }
}
