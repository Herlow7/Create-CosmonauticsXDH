package dev.devce.rocketnautics.content.orbit;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.api.orbit.DeepSpaceHelper;
import dev.devce.rocketnautics.content.RocketDimensions;
import dev.devce.rocketnautics.content.orbit.universe.UniverseDefinition;
import dev.devce.rocketnautics.content.orbit.universe.UniverseLoader;
import dev.devce.rocketnautics.network.UniverseDefinitionPayload;
import dev.devce.rocketnautics.network.UniverseTimeSyncPayload;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

@EventBusSubscriber(modid = RocketNautics.MODID)
public class DeepSpaceData extends SavedData {
    public static final int LOGICAL_INSTANCE_HEIGHT = 1000;

    public static final String ID = "cosmonautics_deep_space_data";

    public static boolean tooSoon(MinecraftServer server) {
        return server.getLevel(RocketDimensions.DEEP_SPACE) == null;
    }

    public static DeepSpaceData getInstance(MinecraftServer server) {
        ServerLevel deepSpace = server.getLevel(RocketDimensions.DEEP_SPACE);
        DeepSpaceData data = deepSpace.getChunkSource().getDataStorage().computeIfAbsent(new Factory<>(DeepSpaceData::new, DeepSpaceData::load, null), ID);
        return data;
    }

    @SubscribeEvent
    public static void advanceUniverse(ServerTickEvent.Post event) {
        getInstance(event.getServer()).tick(event.getServer());
    }

    @SubscribeEvent
    public static void syncUniverse(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer splayer)) return;
        PacketDistributor.sendToPlayer(splayer, new UniverseDefinitionPayload(getInstance(splayer.server).getUniverse()));
    }

    // end static //

    private final UniverseDefinition universe = UniverseLoader.INSTANCE.getLoaded();

    private final Int2ObjectMap<InstanceList> instances = new Int2ObjectOpenHashMap<>();

    private long universeTicks;
    private int nextFreeID = 0;

    protected float lastTickRate = 20;

    public void tick(MinecraftServer server) {
        universeTicks += 1;
        instances.values().forEach(i -> i.tick(server));
        setDirty();
        if (server.tickRateManager().tickrate() != lastTickRate || shouldSendRegularPackets(1)) {
            lastTickRate = server.tickRateManager().tickrate();
            PacketDistributor.sendToAllPlayers(new UniverseTimeSyncPayload(universeTicks, lastTickRate));
        }
    }

    public boolean shouldSendRegularPackets(int secondsPerPacket) {
        return universeTicks % (int) lastTickRate * secondsPerPacket == 0;
    }

    private void debugInstance() {
        // execute in rocketnautics:deep_space run tp Dev 48 1016 16
        DeepSpaceInstance instance = claimNewInstance(2);
        instance.getPosition().init(universe, "overworld", new TimeStampedPVCoordinates(DeepSpaceHelper.EPOCH, new Vector3D(0, 0, 9_000_000D), new Vector3D(0, 3_300, 0)));
    }

    public DeepSpaceInstance claimNewInstance(int chunkSize) {
        setDirty();
        int powerSize = (int) Math.ceil(Math.log(chunkSize) / Math.log(2)) - 1;
        return instances.computeIfAbsent(powerSize, InstanceList::new).createInstance(this);
    }

    @Nullable
    public DeepSpaceInstance getInstanceForPos(int xPos, int zPos) {
        int[] params = getChunkPowerSizeIdWithinSizeForParameters(xPos, zPos);
        return getInstance(params[0], params[1]);
    }

    @Nullable
    public DeepSpaceInstance getInstance(long id) {
        return getInstance(unpackSize(id), unpackIdWithinSize(id));
    }

    @Nullable
    public DeepSpaceInstance getInstance(int chunkPowerSize, int idWithinSize) {
        InstanceList l = instances.get(chunkPowerSize);
        if (l == null) return null;
        return l.getInstance(idWithinSize);
    }

    @Nullable
    public DeepSpaceInstance retireInstance(long id) {
        return retireInstance(unpackSize(id), unpackIdWithinSize(id));
    }

    @Nullable
    public DeepSpaceInstance retireInstance(int chunkPowerSize, int idWithinSize) {
        InstanceList l = instances.get(chunkPowerSize);
        if (l == null) return null;
        setDirty();
        return l.retireInstance(idWithinSize);
    }

    public static long pack(int chunkPowSize, int idWithinSize) {
        return ((long) chunkPowSize << 32) + (idWithinSize & 0xFFFFFFFFL);
    }

    public static int unpackSize(long id) {
        return (int) (id >> 32);
    }

    public static int unpackIdWithinSize(long id) {
        return (int) (id & 0xFFFFFFFFL);
    }

    public UniverseDefinition getUniverse() {
        return universe;
    }

    public long getUniverseTicks() {
        return universeTicks;
    }

    public AbsoluteDate getUniverseTime() {
        return DeepSpaceHelper.getDateByTicks(universeTicks);
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        compoundTag.putLong("UniverseTicks", universeTicks);
        compoundTag.putInt("NextID", nextFreeID);
        ListTag list = new ListTag();
        for (InstanceList instanceList : instances.values()) {
            list.add(instanceList.write());
        }
        compoundTag.put("InstanceLists", list);
        return compoundTag;
    }

    private static DeepSpaceData load(CompoundTag tag, HolderLookup.Provider registries) {
        DeepSpaceData data = new DeepSpaceData();
        data.universeTicks = tag.getLong("UniverseTicks");
        data.nextFreeID = tag.getInt("NextID");
        ListTag list = tag.getList("InstanceLists", ListTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            InstanceList instance = new InstanceList(data, list.getCompound(i));
            data.instances.put(instance.getChunkPowerSize(), instance);
        }
        return data;
    }

    // TODO mixin to CollisionGetter#borderCollision and Entity#collectColliders to add this
    // collision box at the same place the world border's collision box is added.
    public static VoxelShape getColliderForPosition(Vec3 position) {
        // subtract the instance bounds from the infinity box
        return Shapes.join(
                Shapes.INFINITY,
                getBoxForPosition(position),
                BooleanOp.ONLY_FIRST
        );
    }

    public static VoxelShape getBoxForPosition(Vec3 position) {
        // compute the instance we are in
        int[] sizeAndId = getChunkPowerSizeIdWithinSizeForParameters((int) position.x, (int) position.z);
        ChunkPos corner = getMinCornerForParameters(sizeAndId[0], sizeAndId[1]);
        int blockSize = 16 * (2 << sizeAndId[0]);
        return Shapes.box(
                corner.getMinBlockX(),
                LOGICAL_INSTANCE_HEIGHT,
                corner.getMinBlockZ(),
                corner.getMinBlockX() + blockSize + 1,
                LOGICAL_INSTANCE_HEIGHT + blockSize + 1,
                corner.getMinBlockZ() + blockSize + 1
        );
    }

    public static int[] getChunkPowerSizeIdWithinSizeForParameters(int negX, int negZ) {
        if (negX < 0 || negZ < 0) return new int[] { 1, 0 };
        // convert to chunkpos
        negX /= 16;
        negZ /= 16;
        // derive chunk size from X position
        // since the power term dominates at large scale, get a definite upper bound
        int chunkPowerSize = Math.max((int) (Math.log(negX * 1.1) / Math.log(2)) + 1, 1);
        // descend until we are below or equal to the target; at large scales, we will need to do this once.
        int size = chunkPowerSize * 16 + (2 << chunkPowerSize);
        while (size > negX && chunkPowerSize > 0) {
            chunkPowerSize--;
            size = chunkPowerSize * 16 + (2 << chunkPowerSize);
        }
        // derive id from Z position and chunk size
        return new int[] { chunkPowerSize, negZ / (16 + size - chunkPowerSize * 16) };
    }

    public static ChunkPos getMinCornerForParameters(int chunkPowerSize, int idWithinSize) {
        int chunkSize = 2 << chunkPowerSize;
        return new ChunkPos((chunkPowerSize * 16 + chunkSize), (idWithinSize * (16 + chunkSize)));
    }
}
