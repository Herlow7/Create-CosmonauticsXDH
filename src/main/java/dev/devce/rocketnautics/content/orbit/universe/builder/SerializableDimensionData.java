package dev.devce.rocketnautics.content.orbit.universe.builder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.devce.rocketnautics.content.orbit.universe.PlanetDimensionData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record SerializableDimensionData(Optional<ResourceKey<Level>> key, Optional<PlanetDimensionData.AllowedTransfer> allowedTransfer,
                                        Optional<Integer> transitionHeight,
                                        Optional<Boolean> renderUniverseInDimension, Optional<String> dimensionDayTimeControllerName,
                                        Optional<Boolean> applyGravityCorrectionToEntities) {
    public static final Codec<SerializableDimensionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.optionalFieldOf("linked_dimension").forGetter(p -> p.key),
            PlanetDimensionData.AllowedTransfer.CODEC.optionalFieldOf("allowed_transfer").forGetter(p -> p.allowedTransfer),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("dimension_transfer_height").forGetter(p -> p.transitionHeight),
            Codec.BOOL.optionalFieldOf("render_universe_in_dimension").forGetter(p -> p.renderUniverseInDimension),
            Codec.STRING.optionalFieldOf("dimension_day_time_controller_name").forGetter(p -> p.dimensionDayTimeControllerName),
            Codec.BOOL.optionalFieldOf("apply_gravity_correction_to_entities_in_dimension").forGetter(p -> p.applyGravityCorrectionToEntities)
    ).apply(instance, SerializableDimensionData::new));

    public static Optional<SerializableDimensionData> of(Optional<ResourceKey<Level>> key, Optional<PlanetDimensionData.AllowedTransfer> allowedTransfer, Optional<Integer> transitionHeight,
                                                         Optional<Boolean> renderUniverseInDimension, Optional<String> dimensionDayTimeControllerName,
                                                         Optional<Boolean> applyGravityCorrectionToEntities) {
        if (key.isEmpty() && transitionHeight.isEmpty() && renderUniverseInDimension.isEmpty() && dimensionDayTimeControllerName.isEmpty() && applyGravityCorrectionToEntities.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SerializableDimensionData(key, allowedTransfer, transitionHeight, renderUniverseInDimension, dimensionDayTimeControllerName, applyGravityCorrectionToEntities));
    }
}
