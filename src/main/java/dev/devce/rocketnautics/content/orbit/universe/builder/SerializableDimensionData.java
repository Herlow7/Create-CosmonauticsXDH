package dev.devce.rocketnautics.content.orbit.universe.builder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.devce.rocketnautics.api.orbit.AllowedTransfer;
import dev.devce.rocketnautics.api.orbit.AtmosphereFlags;
import dev.ryanhcode.sable.physics.config.dimension_physics.BezierResourceFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;

import java.util.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record SerializableDimensionData(Optional<ResourceKey<Level>> key, Optional<AllowedTransfer> allowedTransfer,
                                        Optional<Integer> transitionHeight, Optional<Int2ObjectRBTreeMap<EnumSet<AtmosphereFlags>>> atmosphere,
                                        Optional<Boolean> renderUniverseInDimension, Optional<String> dimensionDayTimeControllerName,
                                        Optional<Boolean> applyGravityCorrectionToEntities, Optional<BezierResourceFunction> entityDragMultiplier) {
    public static final Codec<SerializableDimensionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.optionalFieldOf("linked_dimension").forGetter(p -> p.key),
            AllowedTransfer.CODEC.optionalFieldOf("allowed_transfer").forGetter(p -> p.allowedTransfer),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("dimension_transfer_height").forGetter(p -> p.transitionHeight),
            Codec.unboundedMap(Codec.STRING.xmap(Integer::parseInt, String::valueOf), AtmosphereFlags.CODEC.listOf().xmap(SerializableDimensionData::properCopy, ArrayList::new)).xmap(Int2ObjectRBTreeMap::new, i -> i).optionalFieldOf("atmosphere_composition").forGetter(p -> p.atmosphere),
            Codec.BOOL.optionalFieldOf("render_universe_in_dimension").forGetter(p -> p.renderUniverseInDimension),
            Codec.STRING.optionalFieldOf("dimension_day_time_controller_name").forGetter(p -> p.dimensionDayTimeControllerName),
            Codec.BOOL.optionalFieldOf("apply_gravity_correction_to_entities_in_dimension").forGetter(p -> p.applyGravityCorrectionToEntities),
            BezierResourceFunction.CODEC.optionalFieldOf("entity_drag_multiplier").forGetter(p -> p.entityDragMultiplier)
    ).apply(instance, SerializableDimensionData::new));

    public static Optional<SerializableDimensionData> of(Optional<ResourceKey<Level>> key, Optional<AllowedTransfer> allowedTransfer,
                                                         Optional<Integer> transitionHeight, Optional<Int2ObjectRBTreeMap<EnumSet<AtmosphereFlags>>> atmosphere,
                                                         Optional<Boolean> renderUniverseInDimension, Optional<String> dimensionDayTimeControllerName,
                                                         Optional<Boolean> applyGravityCorrectionToEntities, Optional<BezierResourceFunction> entityDragMultiplier) {
        if (key.isEmpty() && allowedTransfer.isEmpty() &&
                transitionHeight.isEmpty() && atmosphere.isEmpty() &&
                renderUniverseInDimension.isEmpty() && dimensionDayTimeControllerName.isEmpty() &&
                applyGravityCorrectionToEntities.isEmpty() && entityDragMultiplier.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SerializableDimensionData(key, allowedTransfer, transitionHeight, atmosphere, renderUniverseInDimension, dimensionDayTimeControllerName, applyGravityCorrectionToEntities, entityDragMultiplier));
    }

    private static EnumSet<AtmosphereFlags> properCopy(Collection<AtmosphereFlags> collection) {
        if (collection.isEmpty()) return AtmosphereFlags.empty();
        return EnumSet.copyOf(collection);
    }
}
