package dev.devce.rocketnautics.content.orbit.universe.builder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.devce.rocketnautics.api.orbit.DeepSpaceHelper;
import dev.devce.rocketnautics.api.orbit.FrameTree;
import net.minecraft.util.StringRepresentable;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedAngularCoordinates;

import java.util.Locale;
import java.util.Optional;

public interface SerializableRotation {
    Codec<SerializableRotation> CODEC = Type.CODEC.dispatch(SerializableRotation::type, Type::typeCodec);

    enum Type implements StringRepresentable {
        VELOCITY(Velocity.CODEC),
        PERIOD(Period.CODEC),
        TIDAL_LOCK(SpinOrbitResonance.CODEC),;
        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final MapCodec<? extends SerializableRotation> typeCodec;

        Type(MapCodec<? extends SerializableRotation> codec) {
            this.typeCodec = codec;
        }

        public MapCodec<? extends SerializableRotation> typeCodec() {
            return typeCodec;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    @NotNull Type type();

    @NotNull TimeStampedAngularCoordinates toCoordinates(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent, @NotNull SerializablePosition position);

    record Velocity(AbsoluteDate startingDate, Rotation startingRotation, Vector3D velocity) implements SerializableRotation {
        public static final MapCodec<Velocity> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                DeepSpaceHelper.DATE_CODEC.optionalFieldOf("starting_date", DeepSpaceHelper.EPOCH).forGetter(Velocity::startingDate),
                DeepSpaceHelper.ROTATION_CODEC.optionalFieldOf("starting_rotation", Rotation.IDENTITY).forGetter(Velocity::startingRotation),
                DeepSpaceHelper.VEC3D_CODEC.fieldOf("position").forGetter(Velocity::velocity)
        ).apply(instance, Velocity::new));

        public static Velocity of(Vector3D velocity) {
            return new Velocity(DeepSpaceHelper.EPOCH, Rotation.IDENTITY, velocity);
        }

        public static Velocity of(Vector3D velocity, Rotation startingRotation) {
            return new Velocity(DeepSpaceHelper.EPOCH, startingRotation, velocity);
        }

        public static Velocity of(Vector3D velocity, Rotation startingRotation, AbsoluteDate startingDate) {
            return new Velocity(startingDate, startingRotation, velocity);
        }

        @Override
        public @NotNull Type type() {
            return Type.VELOCITY;
        }

        @Override
        public @NotNull TimeStampedAngularCoordinates toCoordinates(@NotNull UniverseDefinitionBuilder builder, @NonNull FrameTree parent, @NonNull SerializablePosition position) {
            return new TimeStampedAngularCoordinates(startingDate, startingRotation, velocity, Vector3D.ZERO);
        }
    }

    record Period(AbsoluteDate startingDate, Rotation startingRotation, Vector3D rotationAxis, double periodSeconds) implements SerializableRotation {
        public static final MapCodec<Period> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                DeepSpaceHelper.DATE_CODEC.optionalFieldOf("starting_date", DeepSpaceHelper.EPOCH).forGetter(Period::startingDate),
                DeepSpaceHelper.ROTATION_CODEC.optionalFieldOf("starting_rotation", Rotation.IDENTITY).forGetter(Period::startingRotation),
                DeepSpaceHelper.VEC3D_CODEC.fieldOf("rotation_axis").forGetter(Period::rotationAxis),
                Codec.DOUBLE.fieldOf("period_seconds").forGetter(Period::periodSeconds)
        ).apply(instance, Period::new));

        public static Period of(Vector3D rotationAxis, double periodSeconds) {
            return new Period(DeepSpaceHelper.EPOCH, Rotation.IDENTITY, rotationAxis, periodSeconds);
        }

        public static Period of(Vector3D rotationAxis, double periodSeconds, Rotation startingRotation) {
            return new Period(DeepSpaceHelper.EPOCH, startingRotation, rotationAxis, periodSeconds);
        }

        public static Period of(Vector3D rotationAxis, double periodSeconds, Rotation startingRotation, AbsoluteDate startingDate) {
            return new Period(startingDate, startingRotation, rotationAxis, periodSeconds);
        }

        @Override
        public @NotNull Type type() {
            return Type.PERIOD;
        }

        @Override
        public @NotNull TimeStampedAngularCoordinates toCoordinates(@NotNull UniverseDefinitionBuilder builder, @NonNull FrameTree parent, @NonNull SerializablePosition position) {
            Vector3D angVel = rotationAxis.scalarMultiply(2 * Math.PI / (periodSeconds * rotationAxis.getNorm()));
            return new TimeStampedAngularCoordinates(startingDate, startingRotation, angVel, Vector3D.ZERO);
        }
    }

    record SpinOrbitResonance(AbsoluteDate startingDate, Rotation startingRotation, Optional<Vector3D> rotationAxis, double rotationsPerOrbit) implements SerializableRotation {
        public static final MapCodec<SpinOrbitResonance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                DeepSpaceHelper.DATE_CODEC.optionalFieldOf("starting_date", DeepSpaceHelper.EPOCH).forGetter(SpinOrbitResonance::startingDate),
                DeepSpaceHelper.ROTATION_CODEC.optionalFieldOf("starting_rotation", Rotation.IDENTITY).forGetter(SpinOrbitResonance::startingRotation),
                DeepSpaceHelper.VEC3D_CODEC.optionalFieldOf("rotation_axis").forGetter(SpinOrbitResonance::rotationAxis),
                Codec.DOUBLE.fieldOf("rotations_per_orbit").forGetter(SpinOrbitResonance::rotationsPerOrbit)
        ).apply(instance, SpinOrbitResonance::new));

        public static SpinOrbitResonance of(double rotationsPerOrbit) {
            return new SpinOrbitResonance(DeepSpaceHelper.EPOCH, Rotation.IDENTITY, Optional.empty(), rotationsPerOrbit);
        }

        public static SpinOrbitResonance of(double rotationsPerOrbit, @Nullable Vector3D rotationAxis) {
            return new SpinOrbitResonance(DeepSpaceHelper.EPOCH, Rotation.IDENTITY, Optional.ofNullable(rotationAxis), rotationsPerOrbit);
        }

        public static SpinOrbitResonance of(double rotationsPerOrbit, @Nullable Vector3D rotationAxis, Rotation startingRotation) {
            return new SpinOrbitResonance(DeepSpaceHelper.EPOCH, startingRotation, Optional.ofNullable(rotationAxis), rotationsPerOrbit);
        }

        public static SpinOrbitResonance of(double rotationsPerOrbit, @Nullable Vector3D rotationAxis, Rotation startingRotation, AbsoluteDate startingDate) {
            return new SpinOrbitResonance(startingDate, startingRotation, Optional.ofNullable(rotationAxis), rotationsPerOrbit);
        }

        @Override
        public @NotNull Type type() {
            return Type.TIDAL_LOCK;
        }

        @Override
        public @NotNull TimeStampedAngularCoordinates toCoordinates(@NotNull UniverseDefinitionBuilder builder, @NonNull FrameTree parent, @NonNull SerializablePosition position) {
            Vector3D axis = rotationAxis.orElseGet(() -> position.momentum(builder, parent));
            Vector3D angVel = axis.scalarMultiply(2 * Math.PI * rotationsPerOrbit / (position.period(builder, parent) * axis.getNorm()));
            return new TimeStampedAngularCoordinates(startingDate, startingRotation, angVel, Vector3D.ZERO);
        }
    }
}
