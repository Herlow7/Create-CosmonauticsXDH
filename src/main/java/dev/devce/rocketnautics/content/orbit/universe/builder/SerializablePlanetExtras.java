package dev.devce.rocketnautics.content.orbit.universe.builder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record SerializablePlanetExtras(Optional<Boolean> star, Optional<Boolean> clouds, Optional<String> lightSourceName) {
    public static final Codec<SerializablePlanetExtras> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("is_star").forGetter(p -> p.star),
            Codec.BOOL.optionalFieldOf("has_clouds").forGetter(p -> p.clouds),
            Codec.STRING.optionalFieldOf("light_source_name").forGetter(p -> p.lightSourceName)
    ).apply(instance, SerializablePlanetExtras::new));

    public static Optional<SerializablePlanetExtras> of(Optional<Boolean> star, Optional<Boolean> clouds, Optional<String> lightSourceName) {
        if (star.isEmpty() && clouds.isEmpty() && lightSourceName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SerializablePlanetExtras(star, clouds, lightSourceName));
    }
}
