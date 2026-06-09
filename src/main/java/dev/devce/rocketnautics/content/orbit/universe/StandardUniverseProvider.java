package dev.devce.rocketnautics.content.orbit.universe;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.RocketDimensions;
import dev.devce.rocketnautics.content.orbit.universe.builder.PlanetDefinitionBuilder;
import dev.devce.rocketnautics.content.orbit.universe.builder.UniverseDefinitionBuilder;
import net.minecraft.world.level.Level;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

public final class StandardUniverseProvider {
    private static final double solRadius = 300_000_000D;
    private static final double overworldRadius = 3_000_000D; // 1 / 10th of the dimension radius
    private static final int overworldOrbitalYearInOverworldDays = 72 * 7; // one real-life week. Balance between a shorter time and having a large sphere of influence.
    private static final int overworldDaynightCycleLengthSeconds = 1200;
    private static final int lunarMonthInOverworldDays = 8 * 3; // 8 real-life hours
    private static final double overworldDistance = solRadius * 40 / 3; // roughly based on the angular size of the sun in the overworld
    // orbit duration in seconds = 2pi * sqrt(r^3 / mu)
    // mu = r^3 * (2pi / orbit duration in seconds)^2
    // compute solar mu based on this
    private static final double comp = overworldDistance * Math.PI / (overworldOrbitalYearInOverworldDays * overworldDaynightCycleLengthSeconds);
    private static final double solMu = 4 * overworldDistance * comp * comp;

    private StandardUniverseProvider() {}

    public static UniverseDefinitionBuilder createSolarSystem() {
        return createSunOverworldMoon()
                .cubePlanet(mars())
                .cubePlanet(gasGiant())
                .cubePlanet(iceWorld());
    }

    public static UniverseDefinitionBuilder createSunOverworldMoon() {
        return UniverseDefinition.builder()
                .cubePlanet(sol())
                .cubePlanet(overworld())
                .cubePlanet(moon());
    }

    public static PlanetDefinitionBuilder sol() {
        return new PlanetDefinitionBuilder("root", "sol")
                .setMu(solMu)
                .setStar(true)
                .setTextureOverride(RocketNautics.path("textures/planet/sol.png"))
                .setRadius(solRadius)
                .setRotationPeriod(Vector3D.PLUS_J, overworldDaynightCycleLengthSeconds * 32d)
                .setFixedPosition(Vector3D.ZERO)
                .setPriority(0);
    }

    public static PlanetDefinitionBuilder overworld() {
        return new PlanetDefinitionBuilder("sol", "overworld")
                .setAccelerationAtSurface(11)
                .setClouds(true)
                .setParentIsShadowLightSource()
                .setLinkedDimension(Level.OVERWORLD)
                .setDimensionTransferHeight(20000)
                .setRadius(overworldRadius)
                .setCircularOrbit(overworldOrbitalYearInOverworldDays * overworldDaynightCycleLengthSeconds, Vector3D.PLUS_J)
                .setRotationPeriod(Vector3D.MINUS_J, overworldDaynightCycleLengthSeconds)
                .setPriority(0);
    }

    public static PlanetDefinitionBuilder moon() {
        return new PlanetDefinitionBuilder("overworld", "moon")
                .setShadowLightSource("sol")
                .setAccelerationAtSurface(2)
                .setLinkedDimension(RocketDimensions.MOON)
                .setRenderUniverseInDimension(true)
                .setDimensionDayTimeController("sol")
                .setApplyGravityCorrectionToEntities(true)
                .setDimensionTransferHeight(20000)
                .setCircularOrbit(lunarMonthInOverworldDays * overworldDaynightCycleLengthSeconds, Vector3D.PLUS_J)
                .setRadius(overworldRadius / 4)
                .setTidalLocked()
                .setPriority(0);
    }

    public static PlanetDefinitionBuilder mars() {
        return new PlanetDefinitionBuilder("sol", "mars")
                .setAccelerationAtSurface(3.7)
                .setParentIsShadowLightSource()
                .setRadius(overworldRadius * 0.53) // Mars is smaller than Earth
                .setCircularOrbit((int) (overworldOrbitalYearInOverworldDays * 1.88) * overworldDaynightCycleLengthSeconds, Vector3D.PLUS_J)
                .setRotationPeriod(Vector3D.MINUS_J, 1230) // ~24.6 hours
                .setTextureOverride(RocketNautics.path("textures/planet/mars.png"))
                .setPriority(0);
    }

    public static PlanetDefinitionBuilder gasGiant() {
        return new PlanetDefinitionBuilder("sol", "gas_giant")
                .setAccelerationAtSurface(24.8)
                .setParentIsShadowLightSource()
                .setRadius(overworldRadius * 4.2) // Jupiter is massive!
                .setCircularOrbit(overworldOrbitalYearInOverworldDays * 4 * overworldDaynightCycleLengthSeconds, Vector3D.PLUS_J)
                .setRotationPeriod(Vector3D.MINUS_J, 500) // Spins very quickly
                .setTextureOverride(RocketNautics.path("textures/planet/gas_giant.png"))
                .setPriority(0);
    }

    public static PlanetDefinitionBuilder iceWorld() {
        return new PlanetDefinitionBuilder("sol", "ice_world")
                .setAccelerationAtSurface(11.0)
                .setParentIsShadowLightSource()
                .setRadius(overworldRadius * 1.8) // Neptune-like
                .setCircularOrbit(overworldOrbitalYearInOverworldDays * 8 * overworldDaynightCycleLengthSeconds, Vector3D.PLUS_J)
                .setRotationPeriod(Vector3D.MINUS_J, 800)
                .setTextureOverride(RocketNautics.path("textures/planet/ice_planet.png"))
                .setPriority(0);
    }
}
