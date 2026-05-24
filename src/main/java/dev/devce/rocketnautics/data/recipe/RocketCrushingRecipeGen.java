package dev.devce.rocketnautics.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.CrushingRecipeGen;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.registry.RocketBlocks;
import dev.devce.rocketnautics.registry.RocketItems;
import dev.devce.rocketnautics.registry.RocketTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static dev.devce.rocketnautics.registry.RocketBlocks.*;
import static dev.devce.rocketnautics.registry.RocketItems.*;
import static dev.devce.rocketnautics.registry.RocketTags.*;

public class RocketCrushingRecipeGen extends CrushingRecipeGen {

    GeneratedRecipe TITANIUM_ORE = stoneOre(RocketBlocks.TITANIUM_ORE::get, CRUSHED_TITANIUM::get, 1.75f, 350);
    GeneratedRecipe DEEP_TITANIUM_ORE = deepslateOre(DEEPSLATE_TITANIUM_ORE::get, CRUSHED_TITANIUM::get, 2.25f, 450);
    GeneratedRecipe RAW_TITANIUM_ORE = rawOre("titanium", MetalTags.TITANIUM::rawOres, CRUSHED_TITANIUM::get, 1);
    GeneratedRecipe RAW_TITANIUM_BLOCK = rawOreBlock("titanium", MetalTags.TITANIUM.rawStorageBlocks::items, CRUSHED_TITANIUM::get, 1);

    GeneratedRecipe LUNAR_AGED_BASALT = create(() -> RocketBlocks.LUNAR_AGED_BASALT, b -> b.duration(250)
            .output(RocketBlocks.LUNAR_SHATTERED_REGOLITH)
            .output(0.1f, LUNAR_LOOSE_REGOLITH));
    GeneratedRecipe LUNAR_REGOLITH = create(() -> RocketBlocks.LUNAR_REGOLITH, b -> b.duration(250)
            .output(RocketBlocks.LUNAR_SHATTERED_REGOLITH)
            .output(0.1f, LUNAR_LOOSE_REGOLITH));
    GeneratedRecipe LUNAR_SHATTERED_REGOLITH = create(() -> RocketBlocks.LUNAR_SHATTERED_REGOLITH, b -> b.duration(250)
            .output(LUNAR_LOOSE_REGOLITH)
            .output(0.1f, LUNAR_ROCK_SMOOTH)
            .output(0.1f, LUNAR_ROCK_SPIKY)
            .output(0.1f, LUNAR_ROCK_TALL));

    public RocketCrushingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, RocketNautics.MODID);
    }

    @Override
    protected GeneratedRecipe create(Supplier<ItemLike> singleIngredient, UnaryOperator<StandardProcessingRecipe.Builder<CrushingRecipe>> transform) {
        return super.create(RocketNautics.MODID, singleIngredient, transform);
    }
}
