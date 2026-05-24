package dev.devce.rocketnautics.data.recipe;

import com.simibubi.create.api.data.recipe.WashingRecipeGen;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.items.RocketItem;
import dev.devce.rocketnautics.registry.RocketBlocks;
import dev.devce.rocketnautics.registry.RocketItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static dev.devce.rocketnautics.registry.RocketBlocks.*;
import static dev.devce.rocketnautics.registry.RocketBlocks.LUNAR_ROCK_TALL;
import static dev.devce.rocketnautics.registry.RocketItems.*;

public class RocketWashingRecipeGen extends WashingRecipeGen {

    GeneratedRecipe CRUSHED_TITANIUM = crushedOreRocket(RocketItems.CRUSHED_TITANIUM, TITANIUM_NUGGET::get, () -> Items.NETHERITE_SCRAP, .05f);
    GeneratedRecipe LUNAR_LOOSE_REGOLITH = create(() -> RocketBlocks.LUNAR_LOOSE_REGOLITH, b -> b
            .output(Blocks.GRAVEL)
            .output(0.1f, LUNAR_MOSS_SHORT)
            .output(0.1f, LUNAR_MOSS_SCRAGGLY)
            .output(0.1f, LUNAR_MOSS_STIFF));

    public RocketWashingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, RocketNautics.MODID);
    }

    public GeneratedRecipe crushedOreRocket(ItemEntry<RocketItem> crushed, Supplier<ItemLike> nugget, Supplier<ItemLike> secondary,
                                            float secondaryChance) {
        return create(crushed::get, b -> b.output(nugget.get(), 9)
                .output(secondaryChance, secondary.get(), 1));
    }

    @Override
    protected GeneratedRecipe create(Supplier<ItemLike> singleIngredient, UnaryOperator<StandardProcessingRecipe.Builder<SplashingRecipe>> transform) {
        return super.create(RocketNautics.MODID, singleIngredient, transform);
    }
}
