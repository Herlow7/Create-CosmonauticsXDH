package dev.devce.rocketnautics.content.orbit.universe.builder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.devce.rocketnautics.api.orbit.DeepSpaceHelper;
import dev.devce.rocketnautics.api.orbit.FrameTree;
import dev.devce.rocketnautics.content.orbit.universe.CubePlanet;
import dev.devce.rocketnautics.content.orbit.universe.PlanetDimensionData;
import dev.devce.rocketnautics.content.orbit.universe.PlanetExtras;
import dev.devce.rocketnautics.content.orbit.universe.PointGravitySource;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class PlanetDefinitionBuilder {
    public static final Codec<PlanetDefinitionBuilder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("parent").forGetter(p -> p.parent),
            Codec.STRING.fieldOf("name").forGetter(p -> p.name),
            Codec.DOUBLE.optionalFieldOf("radius").forGetter(p -> p.radius),
            Codec.DOUBLE.optionalFieldOf("mu").forGetter(p -> p.mu),
            Codec.DOUBLE.optionalFieldOf("acceleration_at_surface").forGetter(p -> p.accelerationAtSurface),
            SerializablePosition.CODEC.optionalFieldOf("position").forGetter(p -> p.position),
            SerializableRotation.CODEC.optionalFieldOf("rotation").forGetter(p -> p.rotation),
            SerializableDimensionData.CODEC.optionalFieldOf("dimension_data").forGetter(PlanetDefinitionBuilder::serializeDimensionData),
            SerializablePlanetExtras.CODEC.optionalFieldOf("planet_extras").forGetter(PlanetDefinitionBuilder::serializePlanetExtras),
            ResourceLocation.CODEC.optionalFieldOf("texture_override").forGetter(p -> p.textureOverride),
            Codec.BOOL.optionalFieldOf("use_texture_override").forGetter(p -> p.useTextureOverride),
            Codec.INT.optionalFieldOf("priority", 1000).forGetter(p -> p.priority),
            Codec.BOOL.optionalFieldOf("disabled").forGetter(p -> p.disabled)
            ).apply(instance, PlanetDefinitionBuilder::new));

    // dimension data
    public Optional<ResourceKey<Level>> linkedDimension = Optional.empty();
    public Optional<PlanetDimensionData.AllowedTransfer> allowedTransfer = Optional.empty();
    public Optional<Integer> transferHeight = Optional.empty();
    public Optional<Boolean> renderUniverseInDimension = Optional.empty();
    public Optional<String> dimensionDayTimeControllerName = Optional.empty();
    public Optional<Boolean> applyGravityCorrectionToEntities = Optional.empty();
    // planet extras
    public Optional<Boolean> clouds = Optional.empty();
    public Optional<Boolean> star = Optional.empty();
    public Optional<String> lightSource = Optional.empty();
    // everything else
    public final Optional<String> parent;
    public final @NotNull String name;
    public Optional<Double> radius = Optional.empty();
    public Optional<Boolean> useTextureOverride = Optional.empty();
    public Optional<ResourceLocation> textureOverride = Optional.empty();

    public Optional<Double> mu = Optional.empty();
    public Optional<Double> accelerationAtSurface = Optional.empty();

    public Optional<SerializableRotation> rotation = Optional.empty();
    public Optional<SerializablePosition> position = Optional.empty();

    // universe loader data
    public Optional<Boolean> disabled = Optional.empty();
    public int priority = 1000;
    public final Set<String> dependencies;

    public PlanetDefinitionBuilder(@Nullable String parent, @NotNull String name) {
        this.parent = (parent == null ? "root" : parent).describeConstable();
        this.name = name;
        this.dependencies = new ObjectOpenHashSet<>();
    }

    protected PlanetDefinitionBuilder(Optional<String> parent, @NotNull String name, Optional<Double> radius,
                                      Optional<Double> mu, Optional<Double> accelerationAtSurface,
                                      Optional<SerializablePosition> position, Optional<SerializableRotation> rotation,
                                      Optional<SerializableDimensionData> dimData, Optional<SerializablePlanetExtras> extras,
                                      Optional<ResourceLocation> textureOverride, Optional<Boolean> useTextureOverride,
                                      int priority, Optional<Boolean> disabled) {
        this.parent = parent;
        this.name = name;
        this.dependencies = new ObjectOpenHashSet<>();
        parent.ifPresent(dependencies::add);
        this.radius = radius;
        this.mu = mu;
        this.accelerationAtSurface = accelerationAtSurface;
        this.position = position;
        this.rotation = rotation;
        if (dimData.isPresent()) {
            this.linkedDimension = dimData.get().key();
            this.allowedTransfer = dimData.get().allowedTransfer();
            this.transferHeight = dimData.get().transitionHeight();
            this.renderUniverseInDimension = dimData.get().renderUniverseInDimension();
            this.dimensionDayTimeControllerName = dimData.get().dimensionDayTimeControllerName();
            dimensionDayTimeControllerName.ifPresent(dependencies::add);
            this.applyGravityCorrectionToEntities = dimData.get().applyGravityCorrectionToEntities();
        }
        if (extras.isPresent()) {
            this.clouds = extras.get().clouds();
            this.star = extras.get().star();
            this.lightSource = extras.get().lightSourceName();
            lightSource.ifPresent(dependencies::add);
        }
        this.textureOverride = textureOverride;
        this.useTextureOverride = useTextureOverride;
        this.priority = priority;
        this.disabled = disabled;
    }

    public PlanetDefinitionBuilder subsume(PlanetDefinitionBuilder other) {
        return new PlanetDefinitionBuilder(
                resolve(this.parent, other.parent), this.name, resolve(this.radius, other.radius),
                resolve(this.mu, other.mu), resolve(this.accelerationAtSurface, other.accelerationAtSurface),
                resolve(this.position, other.position), resolve(this.rotation, other.rotation),
                resolve(this.serializeDimensionData(), other.serializeDimensionData()),
                resolve(this.serializePlanetExtras(), other.serializePlanetExtras()),
                resolve(this.textureOverride, other.textureOverride),
                resolve(this.useTextureOverride, other.useTextureOverride),
                this.priority, this.disabled
        );
    }

    protected <T> Optional<T> resolve(Optional<T> our, Optional<T> their) {
        return our.isPresent() ? our : their;
    }

    protected Optional<SerializableDimensionData> serializeDimensionData() {
        return SerializableDimensionData.of(linkedDimension, allowedTransfer, transferHeight, renderUniverseInDimension, dimensionDayTimeControllerName, applyGravityCorrectionToEntities);
    }

    protected Optional<SerializablePlanetExtras> serializePlanetExtras() {
        return SerializablePlanetExtras.of(star, clouds, lightSource);
    }

    public CubePlanet build(UniverseDefinitionBuilder destination) {
        if (this.parent.isEmpty()) {
            throw new IllegalStateException("Builder has no parent frame! To lock to the unmoving root frame, use 'root'!");
        }
        if (radius.isEmpty()) {
            throw new IllegalStateException("Builder has no radius!");
        } else if (radius.get() <= 0) {
            throw new IllegalStateException("Builder has a nonpositive radius [" + radius.get() + "]!");
        }
        double radius = this.radius.get();
        FrameTree parent = destination.getFrameByName(this.parent.get());
        if (parent == null) {
            throw new IllegalStateException("Builder could not find its parent!");
        }
        if (position.isEmpty()) {
            throw new IllegalStateException("Builder has no position defined!");
        }
        FrameTree ourFrame = position.get().createChild(destination, parent, name);
        if (ourFrame == null) {
            throw new IllegalStateException("Builder's configured name is already reserved!");
        }
        if (rotation.isEmpty()) {
            throw new IllegalStateException("Builder has no rotation defined!");
        }
        TimeStampedAngularCoordinates angularCoordinates = rotation.get().toCoordinates(destination, parent, position.get());
        // mu / radius^2 = acceleration at surface + centrifugal acceleration
        double mu;
        if (this.mu.isEmpty()) {
            if (accelerationAtSurface.isEmpty()) {
                throw new IllegalStateException("Builder has no mu or surface acceleration defined!");
            }
            double centrifugal = angularCoordinates.getRotationRate().getNormSq() * radius;
            mu = (accelerationAtSurface.get() + centrifugal) * radius * radius;
            if (mu <= 0) {
                throw new IllegalStateException("Builder results in nonpositive mu [" + mu + "] with acceleration at surface [" + accelerationAtSurface.get() + "]!");
            }
        } else {
            mu = this.mu.get();
            if (mu <= 0) {
                throw new IllegalStateException("Builder has a nonpositive mu [" + mu + "]!");
            }
        }
        if (textureOverride.isEmpty() && linkedDimension.isEmpty()) {
            throw new IllegalStateException("Builder does not have a render option available!");
        }
        double roi;
        PointGravitySource parentSource = destination.getGravitySource(parent, false);
        if (parentSource != null) {
            roi = position.get().semiMajorAxis(destination, parent) * Math.pow(mu / parentSource.mu(), 2/5d);
        } else {
            roi = Double.POSITIVE_INFINITY;
        }
        ResourceLocation override;
        destination.gravitySource(new PointGravitySource(ourFrame, mu, roi));
        CubePlanet p = new CubePlanet(ourFrame, radius, angularCoordinates, constructDimensionData(destination), useTextureOverride.orElse(true) ? textureOverride.orElse(null) : null, constructExtras(destination));
        destination.cubePlanet(p);
        return p;
    }

    protected PlanetDimensionData constructDimensionData(UniverseDefinitionBuilder destination) {
        if (linkedDimension.isEmpty()) return null;
        int id = -1;
        if (dimensionDayTimeControllerName.isPresent()) {
            FrameTree f = destination.getFrameByName(dimensionDayTimeControllerName.get());
            if (f != null) {
                id = f.getId();
            }
        }
        return new PlanetDimensionData(linkedDimension.get(), allowedTransfer.orElse(PlanetDimensionData.AllowedTransfer.NONE), transferHeight.orElse(20_000), renderUniverseInDimension.orElse(false), id, applyGravityCorrectionToEntities.orElse(false));
    }

    protected PlanetExtras constructExtras(UniverseDefinitionBuilder destination) {
        int id = -1;
        if (lightSource.isPresent()) {
            FrameTree f = destination.getFrameByName(lightSource.get());
            if (f != null) {
                id = f.getId();
            }
        }
        return new PlanetExtras(star.orElse(false), clouds.orElse(false), id);
    }

    public PlanetDefinitionBuilder setLinkedDimension(@Nullable ResourceKey<Level> linkedDimension) {
        return setLinkedDimension(linkedDimension, PlanetDimensionData.AllowedTransfer.ALL);
    }

    public PlanetDefinitionBuilder setLinkedDimension(@Nullable ResourceKey<Level> linkedDimension, PlanetDimensionData.AllowedTransfer allowedTransfer) {
        this.linkedDimension = Optional.ofNullable(linkedDimension);
        this.allowedTransfer = Optional.ofNullable(allowedTransfer);
        return this;
    }

    public PlanetDefinitionBuilder setDimensionTransferHeight(int transferHeight) {
        this.transferHeight = Optional.of(transferHeight);
        return this;
    }

    public PlanetDefinitionBuilder setRenderUniverseInDimension(boolean renderUniverseInDimension) {
        this.renderUniverseInDimension = Optional.of(renderUniverseInDimension);
        return this;
    }

    public PlanetDefinitionBuilder setDimensionDayTimeController(String name) {
        this.dimensionDayTimeControllerName = name.describeConstable();
        return this;
    }

    public PlanetDefinitionBuilder setApplyGravityCorrectionToEntities(boolean applyGravityCorrectionToEntities) {
        this.applyGravityCorrectionToEntities = Optional.of(applyGravityCorrectionToEntities);
        return this;
    }

    public PlanetDefinitionBuilder setTextureOverride(ResourceLocation textureAlternative) {
        this.useTextureOverride = Optional.of(true);
        this.textureOverride = Optional.ofNullable(textureAlternative);
        return this;
    }

    public PlanetDefinitionBuilder setRenderDataOverride(@NotNull IntFunction<byte[]> override) {
        return this;
    }

    public PlanetDefinitionBuilder setStar(boolean star) {
        this.star = Optional.of(star);
        return this;
    }

    public PlanetDefinitionBuilder setClouds(boolean clouds) {
        this.clouds = Optional.of(clouds);
        return this;
    }

    public PlanetDefinitionBuilder setParentIsShadowLightSource() {
        parent.ifPresent(s -> this.lightSource = s.describeConstable());
        return this;
    }

    public PlanetDefinitionBuilder setShadowLightSource(String name) {
        this.lightSource = name.describeConstable();
        this.dependencies.add(name);
        return this;
    }

    /**
     * @param radius meters/blocks
     */
    public PlanetDefinitionBuilder setRadius(double radius) {
        this.radius = Optional.of(radius);
        return this;
    }

    public PlanetDefinitionBuilder setRotation(SerializableRotation rotation) {
        this.rotation = Optional.ofNullable(rotation);
        return this;
    }

    public PlanetDefinitionBuilder setRotation(Vector3D velocity) {
        this.rotation = Optional.of(SerializableRotation.Velocity.of(velocity));
        return this;
    }

    public PlanetDefinitionBuilder setRotation(Vector3D velocity, Rotation startingRotation) {
        this.rotation = Optional.of(SerializableRotation.Velocity.of(velocity, startingRotation));
        return this;
    }

    public PlanetDefinitionBuilder setRotationPeriod(Vector3D rotationAxis, double periodSeconds) {
        this.rotation = Optional.of(SerializableRotation.Period.of(rotationAxis, periodSeconds));
        return this;
    }

    public PlanetDefinitionBuilder setRotationPeriod(Vector3D rotationAxis, double periodSeconds, Rotation startingRotation) {
        this.rotation = Optional.of(SerializableRotation.Period.of(rotationAxis, periodSeconds, startingRotation));
        return this;
    }

    public PlanetDefinitionBuilder setTidalLocked() {
        this.rotation = Optional.of(SerializableRotation.SpinOrbitResonance.of(1));
        return this;
    }

    public PlanetDefinitionBuilder setTidalLocked(Rotation correction) {
        this.rotation = Optional.of(SerializableRotation.SpinOrbitResonance.of(1, null, correction));
        return this;
    }

    public PlanetDefinitionBuilder setRotationResonance(double rotationsPerOrbit) {
        this.rotation = Optional.of(SerializableRotation.SpinOrbitResonance.of(rotationsPerOrbit));
        return this;
    }

    public PlanetDefinitionBuilder setRotationResonance(double rotationsPerOrbit, Vector3D rotationAxis) {
        this.rotation = Optional.of(SerializableRotation.SpinOrbitResonance.of(rotationsPerOrbit, rotationAxis));
        return this;
    }

    public PlanetDefinitionBuilder setRotationResonance(double rotationsPerOrbit, Rotation startingRotation) {
        this.rotation = Optional.of(SerializableRotation.SpinOrbitResonance.of(rotationsPerOrbit, null, startingRotation));
        return this;
    }

    public PlanetDefinitionBuilder setRotationResonance(double rotationsPerOrbit, Vector3D rotationAxis, Rotation startingRotation) {
        this.rotation = Optional.of(SerializableRotation.SpinOrbitResonance.of(rotationsPerOrbit, rotationAxis, startingRotation));
        return this;
    }

    public PlanetDefinitionBuilder setPosition(SerializablePosition position) {
        this.position = Optional.ofNullable(position);
        return this;
    }

    public PlanetDefinitionBuilder setOrbit(@NotNull TimeStampedPVCoordinates orbitCoords) {
        this.position = Optional.of(SerializablePosition.FullOrbitPosition.of(orbitCoords));
        return this;
    }

    public PlanetDefinitionBuilder setCircularOrbit(@NotNull Vector3D position, @NotNull Vector3D orbitAxis) {
        return this.setCircularOrbit(position, orbitAxis, DeepSpaceHelper.EPOCH);
    }

    public PlanetDefinitionBuilder setCircularOrbit(@NotNull Vector3D position, @NotNull Vector3D orbitAxis, @NotNull AbsoluteDate positionDate) {
        this.position = Optional.of(SerializablePosition.CircularOrbitPosition.of(positionDate, position, orbitAxis));
        return this;
    }

    public PlanetDefinitionBuilder setCircularOrbit(double periodSeconds, @NotNull Vector3D orbitAxis) {
        return this.setCircularOrbit(periodSeconds, orbitAxis, null, DeepSpaceHelper.EPOCH);
    }

    public PlanetDefinitionBuilder setCircularOrbit(double periodSeconds, @NotNull Vector3D orbitAxis, @Nullable Vector3D positionDirection, @NotNull AbsoluteDate positionDate) {
        this.position = Optional.of(SerializablePosition.CircularOrbitPeriod.of(positionDate, orbitAxis, periodSeconds, positionDirection));
        return this;
    }

    public PlanetDefinitionBuilder setFixedPosition(@NotNull Vector3D position) {
        this.position = Optional.of(SerializablePosition.FixedPosition.of(position));
        return this;
    }

    /**
     * @param accelerationAtSurface meters per second squared
     */
    public PlanetDefinitionBuilder setAccelerationAtSurface(double accelerationAtSurface) {
        this.accelerationAtSurface = Optional.of(accelerationAtSurface);
        return this;
    }

    /**
     * @param mu meters cubed per second squared
     */
    public PlanetDefinitionBuilder setMu(double mu) {
        this.mu = Optional.of(mu);
        return this;
    }

    public PlanetDefinitionBuilder setDisabled(boolean disabled) {
        this.disabled = Optional.of(disabled);
        return this;
    }

    public PlanetDefinitionBuilder setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public PlanetDefinitionBuilder addDependency(String name) {
        this.dependencies.add(name);
        return this;
    }
}
