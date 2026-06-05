package dev.devce.rocketnautics.content.orbit.universe.builder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.devce.rocketnautics.api.orbit.DeepSpaceHelper;
import dev.devce.rocketnautics.api.orbit.FrameTree;
import dev.devce.rocketnautics.content.orbit.universe.PointGravitySource;
import net.minecraft.util.StringRepresentable;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Locale;
import java.util.Optional;

public interface SerializablePosition {
    Codec<SerializablePosition> CODEC = Type.CODEC.dispatch(SerializablePosition::type, Type::typeCodec);

    enum Type implements StringRepresentable {
        FIXED(FixedPosition.CODEC),
        FULL_ORBIT(FullOrbitPosition.CODEC),
        CIRCULAR_ORBIT_POSITION(CircularOrbitPosition.CODEC),
        CIRCULAR_ORBIT_PERIOD(CircularOrbitPeriod.CODEC);
        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final MapCodec<? extends SerializablePosition> typeCodec;

        Type(MapCodec<? extends SerializablePosition> codec) {
            this.typeCodec = codec;
        }

        public MapCodec<? extends SerializablePosition> typeCodec() {
            return typeCodec;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    @NotNull Type type();

    @Nullable FrameTree createChild(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent, @NotNull String name);

    double period(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent);

    Vector3D momentum(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent);

    double semiMajorAxis(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent);

    record FixedPosition(Vector3D position) implements SerializablePosition {
        public static final MapCodec<FixedPosition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                DeepSpaceHelper.VEC3D_CODEC.fieldOf("position").forGetter(FixedPosition::position)
        ).apply(instance, FixedPosition::new));

        public static FixedPosition of(Vector3D position) {
            return new FixedPosition(position);
        }

        @Override
        public @NotNull Type type() {
            return Type.FIXED;
        }

        @Override
        public @Nullable FrameTree createChild(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent, @NotNull String name) {
            return parent.createChild(name, position);
        }

        @Override
        public double period(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent) {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public Vector3D momentum(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent) {
            return Vector3D.ZERO;
        }

        @Override
        public double semiMajorAxis(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent) {
            return Double.POSITIVE_INFINITY;
        }
    }

    record FullOrbitPosition(AbsoluteDate coordinateDate, Vector3D position, Vector3D velocity) implements SerializablePosition {
        public static final MapCodec<FullOrbitPosition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                DeepSpaceHelper.DATE_CODEC.fieldOf("coordinate_date").forGetter(FullOrbitPosition::coordinateDate),
                DeepSpaceHelper.VEC3D_CODEC.fieldOf("position").forGetter(FullOrbitPosition::position),
                DeepSpaceHelper.VEC3D_CODEC.fieldOf("velocity").forGetter(FullOrbitPosition::velocity)
        ).apply(instance, FullOrbitPosition::new));

        public static FullOrbitPosition of(TimeStampedPVCoordinates coordinates) {
            return new FullOrbitPosition(coordinates.getDate(), coordinates.getPosition(), coordinates.getVelocity());
        }

        public static FullOrbitPosition of(AbsoluteDate coordinateDate, Vector3D position, Vector3D velocity) {
            return new FullOrbitPosition(coordinateDate, position, velocity);
        }

        @Override
        public @NotNull Type type() {
            return Type.FULL_ORBIT;
        }

        @Override
        public @Nullable FrameTree createChild(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent, @NotNull String name) {
            PointGravitySource fetch = builder.getGravitySource(parent, false);
            if (fetch == null) return null;
            return parent.createChild(name, new KeplerianOrbit(new TimeStampedPVCoordinates(coordinateDate, position, velocity), parent.getOrekitFrame(), fetch.mu()));
        }

        @Override
        public double period(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent) {
            PointGravitySource fetch = builder.getGravitySource(parent, false);
            if (fetch == null) return Double.POSITIVE_INFINITY;
            return new KeplerianOrbit(new TimeStampedPVCoordinates(coordinateDate, position, velocity), parent.getOrekitFrame(), fetch.mu()).getKeplerianPeriod();
        }

        @Override
        public Vector3D momentum(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent) {
            return position.crossProduct(velocity);
        }

        @Override
        public double semiMajorAxis(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent) {
            PointGravitySource fetch = builder.getGravitySource(parent, false);
            if (fetch == null) return Double.POSITIVE_INFINITY;
            return new KeplerianOrbit(new TimeStampedPVCoordinates(coordinateDate, position, velocity), parent.getOrekitFrame(), fetch.mu()).getA();
        }
    }

    record CircularOrbitPosition(AbsoluteDate coordinateDate, Vector3D position, Vector3D orbitAxis) implements SerializablePosition {
        public static final MapCodec<CircularOrbitPosition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                DeepSpaceHelper.DATE_CODEC.fieldOf("coordinate_date").forGetter(CircularOrbitPosition::coordinateDate),
                DeepSpaceHelper.VEC3D_CODEC.fieldOf("position").forGetter(CircularOrbitPosition::position),
                DeepSpaceHelper.VEC3D_CODEC.fieldOf("orbit_axis").forGetter(CircularOrbitPosition::orbitAxis)
        ).apply(instance, CircularOrbitPosition::new));

        public static CircularOrbitPosition of(AbsoluteDate coordinateDate, Vector3D position, Vector3D orbitAxis) {
            return new CircularOrbitPosition(coordinateDate, position, orbitAxis);
        }

        @Override
        public @NotNull Type type() {
            return Type.CIRCULAR_ORBIT_POSITION;
        }

        @Override
        public @Nullable FrameTree createChild(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent, @NotNull String name) {
            PointGravitySource fetch = builder.getGravitySource(parent, false);
            if (fetch == null) return null;
            double velMagnitudeSquared = fetch.mu() / position.getNorm();
            Vector3D vel = orbitAxis.crossProduct(position);
            vel = vel.scalarMultiply(Math.sqrt(velMagnitudeSquared / vel.getNormSq()));
            return parent.createChild(name, new KeplerianOrbit(new TimeStampedPVCoordinates(coordinateDate, position, vel), parent.getOrekitFrame(), fetch.mu()));
        }

        @Override
        public double period(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent) {
            PointGravitySource fetch = builder.getGravitySource(parent, false);
            if (fetch == null) return Double.POSITIVE_INFINITY;
            double velMagnitudeSquared = fetch.mu() / position.getNorm();
            Vector3D vel = orbitAxis.crossProduct(position);
            vel = vel.scalarMultiply(Math.sqrt(velMagnitudeSquared / vel.getNormSq()));
            return new KeplerianOrbit(new TimeStampedPVCoordinates(coordinateDate, position, vel), parent.getOrekitFrame(), fetch.mu()).getKeplerianPeriod();
        }

        @Override
        public Vector3D momentum(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent) {
            PointGravitySource fetch = builder.getGravitySource(parent, false);
            if (fetch == null) return orbitAxis;
            double velMagnitudeSquared = fetch.mu() / position.getNorm();
            return orbitAxis.scalarMultiply(Math.sqrt(velMagnitudeSquared / orbitAxis.getNormSq()));
        }

        @Override
        public double semiMajorAxis(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent) {
            return position.getNorm();
        }
    }

    record CircularOrbitPeriod(AbsoluteDate coordinateDate, Optional<Vector3D> positionDirection, Vector3D orbitAxis, double periodSeconds) implements SerializablePosition {
        public static final MapCodec<CircularOrbitPeriod> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                DeepSpaceHelper.DATE_CODEC.fieldOf("coordinate_date").forGetter(CircularOrbitPeriod::coordinateDate),
                DeepSpaceHelper.VEC3D_CODEC.optionalFieldOf("position_direction").forGetter(CircularOrbitPeriod::positionDirection),
                DeepSpaceHelper.VEC3D_CODEC.fieldOf("orbit_axis").forGetter(CircularOrbitPeriod::orbitAxis),
                Codec.DOUBLE.fieldOf("period_seconds").forGetter(CircularOrbitPeriod::periodSeconds)
        ).apply(instance, CircularOrbitPeriod::new));

        public static CircularOrbitPeriod of(AbsoluteDate coordinateDate, Vector3D orbitAxis, double periodSeconds) {
            return new CircularOrbitPeriod(coordinateDate, Optional.empty(), orbitAxis, periodSeconds);
        }

        public static CircularOrbitPeriod of(AbsoluteDate coordinateDate, Vector3D orbitAxis, double periodSeconds, @Nullable Vector3D positionDirection) {
            return new CircularOrbitPeriod(coordinateDate, Optional.ofNullable(positionDirection), orbitAxis, periodSeconds);
        }

        @Override
        public @NotNull Type type() {
            return Type.CIRCULAR_ORBIT_PERIOD;
        }

        @Override
        public @Nullable FrameTree createChild(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent, @NotNull String name) {
            PointGravitySource fetch = builder.getGravitySource(parent, false);
            if (fetch == null) return null;
            double comp = periodSeconds / (2 * Math.PI);
            double r = Math.cbrt(fetch.mu() * comp * comp);
            Vector3D position = positionDirection.orElseGet(orbitAxis::orthogonal);
            position = position.scalarMultiply(r / position.getNorm());
            double velMagnitudeSquared = fetch.mu() / r;
            Vector3D vel = orbitAxis.crossProduct(position);
            vel = vel.scalarMultiply(Math.sqrt(velMagnitudeSquared / vel.getNormSq()));
            return parent.createChild(name, new KeplerianOrbit(new TimeStampedPVCoordinates(coordinateDate, position, vel), parent.getOrekitFrame(), fetch.mu()));
        }

        @Override
        public double period(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent) {
            return periodSeconds;
        }

        @Override
        public Vector3D momentum(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent) {
            PointGravitySource fetch = builder.getGravitySource(parent, false);
            if (fetch == null) return orbitAxis;
            double comp = periodSeconds / (2 * Math.PI);
            double r = Math.cbrt(fetch.mu() * comp * comp);
            double velMagnitudeSquared = fetch.mu() / r;
            return orbitAxis.scalarMultiply(Math.sqrt(velMagnitudeSquared / orbitAxis.getNormSq()));
        }

        @Override
        public double semiMajorAxis(@NotNull UniverseDefinitionBuilder builder, @NotNull FrameTree parent) {
            PointGravitySource fetch = builder.getGravitySource(parent, false);
            if (fetch == null) return Double.POSITIVE_INFINITY;
            return Math.cbrt(fetch.mu() * periodSeconds * periodSeconds / (4 * Math.PI * Math.PI));
        }
    }
}
