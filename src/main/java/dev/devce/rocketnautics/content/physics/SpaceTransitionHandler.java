

package dev.devce.rocketnautics.content.physics;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.api.orbit.DeepSpaceHelper;
import dev.devce.rocketnautics.content.RocketDimensions;
import dev.devce.rocketnautics.content.orbit.DeepSpaceData;
import dev.devce.rocketnautics.content.orbit.DeepSpaceInstance;
import dev.devce.rocketnautics.content.orbit.universe.CubePlanet;
import dev.devce.rocketnautics.mixin.SubLevelHoldingChunkAccessor;
import dev.devce.rocketnautics.mixin.SubLevelHoldingChunkMapAccessor;
import dev.devce.rocketnautics.network.SeamlessTransitionPayload;
import dev.egg.SubLevelWarper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.*;
import org.jspecify.annotations.Nullable;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.*;

/**
 * Handles the seamless transition of ships and players between dimensions (Overworld <-> Space).
 * This includes saving ship data to NBT, managing teleportation, rebuilding ships in the target dimension,
 * and recovering player seating/entities.
 */
@EventBusSubscriber(modid = RocketNautics.MODID)
public class SpaceTransitionHandler {
    
    public static double WARP_ENTITY_DETECTION_TOLERANCE = 0;
    public static Quaterniondc PREMUL_ROTATION = null;

    public static final int CHUNK_LOADING_PARTITION_SIZES = 16;
    public static final double TRANSITION_SAFE_OFFSET = 1000.0;
    public static final double OVERWORLD_SPACE_Y = 20000.0;

    private static final Deque<Runnable> TICK_TASKS = new ArrayDeque<>();

    /**
     * Initializes autonomous transition listeners.
     * Ships that reach threshold altitudes will automatically jump dimensions and bring players with them.
     */
    public static void init() {
        SableEventPlatform.INSTANCE.onPhysicsTick((physicsSystem, timeStep) -> {
            ServerLevel level = physicsSystem.getLevel();

            // Handle ships currently in the world
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (!DeepSpaceHelper.isDeepSpace(level) && container != null) {

                List<UUID> shipIds = container.getAllSubLevels().stream().map(SubLevel::getUniqueId).toList();
                for (UUID id : shipIds) {
                    SubLevel sl = container.getSubLevel(id);
                    if (!(sl instanceof ServerSubLevel ship) || ship.isRemoved()) continue;
                    Vector3d pos = ship.logicalPose().position();
                    DeepSpaceData instance = DeepSpaceData.getInstance(level.getServer());
                    CubePlanet linked = instance.getUniverse().getPlanetByDimension(level.dimension());
                    if (linked != null && linked.linkedDimension() != null && linked.linkedDimension().allowedTransfer().allowToSpace() && linked.linkedDimension().transitionHeight() < pos.y()) {
                        RigidBodyHandle handle = physicsSystem.getPhysicsHandle(ship);
                        if (!handle.isValid()) continue;
                        double captureSize = ship.boundingBox().size().length();
                        DeepSpaceInstance claimed = instance.claimNewInstance((int) (captureSize / 8 + 2));
                        Quaterniond rotation = initInstance(claimed, ship.logicalPose().position(), handle.getLinearVelocity(new Vector3d()), linked, level);
                        ServerLevel deepSpace = level.getServer().getLevel(RocketDimensions.DEEP_SPACE);
                        // Handle all players nearby
                        Map<ServerPlayer, UUID> riding = new Object2ObjectOpenHashMap<>();
                        for (ServerPlayer pl : level.getPlayers(pl -> ship.boundingBox().toMojang().inflate(10).contains(pl.position()))) {
                            PacketDistributor.sendToPlayer(pl, new SeamlessTransitionPayload(true));
                            if (pl.isPassenger()) {
                                riding.put(pl, pl.getVehicle().getUUID());
                            }
                            Vector3f offset = pl.position().toVector3f().sub(pos.get(new Vector3f()));
                            Vector3f o = rotation.transform(offset);
                            pl.teleportTo(deepSpace, claimed.getCenter().x() + o.x(), claimed.getCenter().y() + o.y(), claimed.getCenter().z() + o.z(), pl.getYRot(), pl.getXRot());
                        }
                        // note that unlike when we're exiting a deep space instance, we know for a fact the involved sublevels are loaded.
                        WARP_ENTITY_DETECTION_TOLERANCE = 10;
                        PREMUL_ROTATION = rotation;
                        SubLevelWarper.WarpSubLevel(ship, deepSpace, claimed.getCenter().get(new Vector3d()));
                        WARP_ENTITY_DETECTION_TOLERANCE = 0;
                        PREMUL_ROTATION = null;
                        riding.forEach((p, e) -> p.startRiding(deepSpace.getEntity(e), true));
                    }
                }
            }
        });
    }

    public static void exitDeepSpace(MinecraftServer server, CubePlanet destination, TimeStampedPVCoordinates coords, DeepSpaceInstance instance, Runnable afterFinished) {
        assert destination.linkedDimension() != null;
        final ServerLevel deepSpace = server.getLevel(RocketDimensions.DEEP_SPACE);
        double targetHeight = destination.linkedDimension().transitionHeight() - SpaceTransitionHandler.TRANSITION_SAFE_OFFSET;
        ServerLevel destLevel = server.getLevel(destination.linkedDimension().key());
        if (destLevel == null) return;
        var dest = DeepSpaceHelper.globalPositionToLocalPositionAndRotation(coords, destination, destLevel);
        dest.left().setComponent(1, targetHeight);
        // Handle all players in the instance
        Map<ServerPlayer, UUID> riding = new Object2ObjectOpenHashMap<>();
        List<ServerPlayer> players = new ObjectArrayList<>();
        instance.collectOnlinePlayers(server, players::add);
        // since we teleport the player and thus remove it from the list of server players,
        // we have to collect and then teleport in different stages to avoid a CME
        for (ServerPlayer pl : players) {
            PacketDistributor.sendToPlayer(pl, new SeamlessTransitionPayload(true));
            if (pl.isPassenger()) {
                riding.put(pl, pl.getVehicle().getUUID());
            }
            Vec3 offset = pl.position().subtract(instance.boundingBox().getCenter());
            Vector3f o = dest.right().transform(offset.toVector3f());
            pl.teleportTo(destLevel, dest.left().x() + o.x(), dest.left().y() + o.y(), dest.left().z() + o.z(), pl.getYRot(), pl.getXRot());
        }
        for (var entry : instance.getOfflinePlayers().entrySet()) {
            Vec3 offset = entry.getValue().subtract(instance.boundingBox().getCenter());
            Vector3f o = dest.right().transform(offset.toVector3f());
            instance.getManager().trackOfflinePlayerTeleport(entry.getKey(), destLevel.dimension(), new Vec3(dest.left().x() + o.x(), dest.left().y() + o.y(), dest.left().z() + o.z()));
        }
        // Handle ships currently in the instance
        Deque<Pair<UUID, @Nullable GlobalSavedSubLevelPointer>> unprocessed = new ArrayDeque<>();
        instance.collectLoadedSublevels(server, l -> unprocessed.addLast(Pair.of(l.getUniqueId(), null)));
        exitLoadedFromDeepSpace(instance, deepSpace, dest.right(), destLevel, dest.left(), unprocessed);
        // whatever the player was riding was absolutely loaded
        riding.forEach((pl, e) -> pl.startRiding(destLevel.getEntity(e), true));

        unprocessed.clear();
        // remap into a deque since we do a lot of iterating and removal, but no searching.
        instance.getUnloadedSublevels().entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue().orElse(null)))
                .forEach(unprocessed::add);
        if (unprocessed.isEmpty()) {
            afterFinished.run();
            return;
        }
        // rephrase into tick tasks since we need to load the unloaded sublevels from disk, which is expensive.
        for (int i = 0; i < unprocessed.size(); i++) {
            TICK_TASKS.addLast(() -> {
                if (unprocessed.isEmpty()) return;
                while (true) {
                    var next = unprocessed.peekFirst();
                    if (next == null) break;
                    ServerSubLevelContainer container = SubLevelContainer.getContainer(deepSpace);
                    assert container != null;
                    if (next.value() != null) {
                        // snatch the unloaded sublevels and teleport them
                        container.getHoldingChunkMap().snatchAndLoad(next.value(), next.key());
                        exitLoadedFromDeepSpace(instance, deepSpace, dest.right(), destLevel, dest.left(), unprocessed);
                        // exitLoaded most likely popped the snatched sublevel, but it's not a guarantee
                        if (unprocessed.peekFirst() == next) {
                            unprocessed.removeFirst();
                        }
                        break;
                        // check if the sublevel is in a pseudoloaded state
                    } else if (container.getHoldingChunkMap().getHoldingSubLevel(next.key()) != null) {
                        // snatch from the holding chunk that is holding this sublevel
                        ((SubLevelHoldingChunkMapAccessor) container.getHoldingChunkMap()).rocketnautics$getHoldingChunks()
                                .values().stream().filter(c -> ((SubLevelHoldingChunkAccessor) c).rocketnautics$getHolding().containsKey(next.key()))
                                .forEach(c -> container.getHoldingChunkMap().snatchAndLoad(new GlobalSavedSubLevelPointer(c.getChunkPos(), (short) 0, (short) 0), next.key()));
                        exitLoadedFromDeepSpace(instance, deepSpace, dest.right(), destLevel, dest.left(), unprocessed);
                        // exitLoaded most likely popped the snatched sublevel, but it's not a guarantee
                        if (unprocessed.peekFirst() == next) {
                            unprocessed.removeFirst();
                        }
                        break;
                    }
                    unprocessed.removeFirst();
                }
            });
        }
        TICK_TASKS.addLast(afterFinished);
    }

    private static void exitLoadedFromDeepSpace(DeepSpaceInstance instance, ServerLevel deepSpace, Quaterniond rot, ServerLevel destLevel, Vector3d destPos, Iterable<Pair<UUID, GlobalSavedSubLevelPointer>> unprocessed) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(deepSpace);
        if (container != null) {
            for (Iterator<Pair<UUID, GlobalSavedSubLevelPointer>> iterator = unprocessed.iterator(); iterator.hasNext(); ) {
                UUID id = iterator.next().key();
                SubLevel sl = container.getSubLevel(id);
                if (sl == null) continue;
                if (!(sl instanceof ServerSubLevel ship) || ship.isRemoved()) {
                    iterator.remove();
                    continue;
                }
                Vector3d pos = ship.logicalPose().position();
                if (instance.boundingBox().contains(pos.x(), pos.y(), pos.z())) {
                    Vector3d offset = pos.sub(instance.getCenter(), new Vector3d());
                    WARP_ENTITY_DETECTION_TOLERANCE = 10;
                    PREMUL_ROTATION = rot;
                    SubLevelWarper.WarpSubLevel(ship, destLevel, rot.transform(offset).add(destPos));
                    WARP_ENTITY_DETECTION_TOLERANCE = 0;
                    PREMUL_ROTATION = null;
                }
                iterator.remove();
            }
        }
    }

    private static Quaterniond initInstance(DeepSpaceInstance instance, Vector3dc dimPosition, Vector3dc velocity, CubePlanet planet, Level level) {
        AbsoluteDate currentDate = instance.getManager().getUniverseTime();
        Vector3d safe = new Vector3d(0, TRANSITION_SAFE_OFFSET, 0).add(dimPosition);
        var globalCoords = DeepSpaceHelper.localPositionToGlobalPositionAndRotation(safe, velocity, level, planet, currentDate);
        instance.getPosition().init(instance.getManager().getUniverse(), planet.orekitFrame(), globalCoords.first());
        return DeepSpaceHelper.adapt(globalCoords.second());
    }

    // TODO delay warps until chunks are loaded?
    private static boolean areChunksReady(ServerPlayer player, ServerLevel level) {
        int cx = player.chunkPosition().x;
        int cz = player.chunkPosition().z;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!level.getChunkSource().hasChunk(cx + dx, cz + dz)) return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // TODO can't we just detect the dimension change on the client and turn it off without a packet?
        PacketDistributor.sendToPlayer(player, new SeamlessTransitionPayload(false));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handleTickTasks(ServerTickEvent.Post event) {
        if (!TICK_TASKS.isEmpty()) {
            TICK_TASKS.removeFirst().run();
        }
        while (event.hasTime() && !TICK_TASKS.isEmpty()) {
            TICK_TASKS.removeFirst().run();
        }
    }

    @SubscribeEvent
    public static void finishTickTasks(ServerStoppingEvent event) {
        if (!TICK_TASKS.isEmpty()) {
            RocketNautics.LOGGER.info("Finishing queued sublevel transfer operations between dimensions before server halts.");
            RocketNautics.LOGGER.info("This may take a while due to needing to read them all from disk and teleport them.");
            while (!TICK_TASKS.isEmpty()) {
                TICK_TASKS.removeFirst().run();
            }
            RocketNautics.LOGGER.info("Queued sublevel transfer operations complete.");
            RocketNautics.LOGGER.info("The rest of unloading may take a while as all of those sublevels are now written to disk again.");
        }
    }
}
