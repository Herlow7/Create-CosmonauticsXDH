package dev.devce.rocketnautics.content.world;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class RilleWorldCarver extends WorldCarver<RilleCarverConfiguration> {
    protected static final ThreadLocal<WorldgenRandom> random = ThreadLocal.withInitial(() -> new WorldgenRandom(RandomSource.create(0)));

    protected final Map<RilleCarverConfiguration, LoadingCache<ChunkPos, ChunkData>> caches = new Object2ObjectOpenHashMap<>();

    public RilleWorldCarver(Codec<RilleCarverConfiguration> p_64711_) {
        super(p_64711_);
    }

    @Override
    public boolean carve(CarvingContext context, RilleCarverConfiguration config, ChunkAccess chunkAccess, Function<BlockPos, Holder<Biome>> biome, RandomSource chunkRandom, Aquifer aquifer, ChunkPos chunkPos, CarvingMask carvingMask) {
        ChunkData ourData = getCacheEntry(config, chunkPos);
        if (ourData == null) return false;
        List<ChunkPos> neighbors = ourData.getNeighbors(config);
        if (neighbors.isEmpty()) return false;
        RandomSource randomForUs = randomForChunk(config, chunkPos);
        int posX = chunkPos.getBlockX(randomForUs.nextInt(16));
        int posZ = chunkPos.getBlockZ(randomForUs.nextInt(16));
        for (ChunkPos neighborPos : neighbors) {
            ChunkData neighborData = getCacheEntry(config, neighborPos);
            if (neighborData == null) continue;
            int termination = ourData.getDistanceFromTermination(config, neighborPos, config.searchLength);
            if (termination <= 0) continue;
            termination = Math.min(termination, neighborData.getDistanceFromTermination(config, chunkPos, config.searchLength));
            if (termination <= 0) continue;
            int dx = neighborPos.x - chunkPos.x;
            int dz = neighborPos.z - chunkPos.z;
            RandomSource randomForThem = randomForChunk(config, neighborPos);
            int neighborPosX = neighborPos.getBlockX(randomForThem.nextInt(16));
            int neighborPosZ = neighborPos.getBlockZ(randomForThem.nextInt(16));

            ChunkPos ourAverage = neighborData.getConnectionsWithinAngle(config, chunkPos, null).getFilteredAverage(config);
            int ourControlX = (posX + (ourAverage == null ? 0 : (posX - ourAverage.getMiddleBlockX())) + neighborPosX) / 2;
            int ourControlZ = (posZ + (ourAverage == null ? 0 : (posZ - ourAverage.getMiddleBlockZ())) + neighborPosZ) / 2;
            ChunkPos neighborAverage = ourData.getConnectionsWithinAngle(config, neighborPos, null).getFilteredAverage(config);
            int neighborControlX = (neighborPosX + (neighborAverage == null ? 0 : (neighborPosX - neighborAverage.getMiddleBlockX())) + posX) / 2;
            int neighborControlZ = (neighborPosZ + (neighborAverage == null ? 0 : (neighborPosZ - neighborAverage.getMiddleBlockZ())) + posZ) / 2;
            // we interpolate with cubic Bézier curves -- compute three derivative vectors to approximate constant speed
            int v1X = -3 * posX + 9 * ourControlX - 9 * neighborControlX + 3 * neighborPosX;
            int v1Z = -3 * posZ + 9 * ourControlZ - 9 * neighborControlZ + 3 * neighborPosZ;
            int v2X = 6 * posX - 12 * ourControlX + 6 * neighborControlX;
            int v2Z = 6 * posZ - 12 * ourControlZ + 6 * neighborControlZ;
            int v3X = -3 * posX + 3 * ourControlX;
            int v3Z = -3 * posZ + 3 * ourControlZ;
            float t = 0;
            while (t < 1) {
                float xComp = t * t * v1X + t * v2X + v3X;
                float zComp = t * t * v1Z + t * v2Z + v3Z;
                t += (float) ((2 * termination) / Math.sqrt(xComp * xComp + zComp * zComp));
                float tM1 = 1 - t;
                double x = (tM1 * tM1 * tM1) * posX + 3 * (tM1 * tM1 * t) * ourControlX + 3 * (tM1 * t * t) * neighborControlX + (t * t * t) * neighborPosX;
                double z = (tM1 * tM1 * tM1) * posZ + 3 * (tM1 * tM1 * t) * ourControlZ + 3 * (tM1 * t * t) * neighborControlZ + (t * t * t) * neighborPosZ;
                double y = chunkAccess.getHeight(Heightmap.Types.WORLD_SURFACE, (int) x, (int) z);
                this.carveEllipsoid(
                        context,
                        config,
                        chunkAccess,
                        biome,
                        aquifer,
                        x,
                        y + termination / 2d + 1.5,
                        z,
                        termination + 2,
                        termination + 2,
                        carvingMask,
                        (p_159082_, xOffset, yOffset, zOffset, absoluteY) -> xOffset * xOffset + yOffset * yOffset + zOffset * zOffset >= 1
                );
            }

//            int[] xs = new int[] { posX, ourControlX, neighborControlX, neighborPosX};
//            int[] zs = new int[] { posZ, ourControlZ, neighborControlZ, neighborPosZ};
//            for (int i = 0; i < 3; i++) {
////                float factor = (float) (i / distance);
////                double x = Mth.lerp2(factor, factor, posX, ourControlX, neighborControlX, neighborPosX);
////                double z = Mth.lerp2(factor, factor, posZ, ourControlZ, neighborControlZ, neighborPosZ);
//                for (int j = 0; j < distance; j++) {
//                    float factor = (float) (j / distance);
//                    double x = Mth.lerp(factor, xs[i], xs[i + 1]);
//                    double z = Mth.lerp(factor, zs[i], zs[i + 1]);
//                    double y = chunkAccess.getHeight(Heightmap.Types.WORLD_SURFACE, (int) x, (int) z);
//                    this.carveEllipsoid(
//                            context,
//                            config,
//                            chunkAccess,
//                            biome,
//                            aquifer,
//                            x,
//                            y + termination / 2d,
//                            z,
//                            termination + 1,
//                            termination + 1,
//                            carvingMask,
//                            (p_159082_, xOffset, yOffset, zOffset, absoluteY) -> xOffset * xOffset + yOffset * yOffset + zOffset * zOffset >= 1
//                    );
//                }
//            }
        }
        return true;
    }

    @Override
    public boolean isStartChunk(RilleCarverConfiguration p_224908_, RandomSource p_224909_) {
        return true;
    }

    protected RandomSource randomForChunk(RilleCarverConfiguration configuration, ChunkPos pos) {
        // TODO add the world seed to salt somehow
        WorldgenRandom random = RilleWorldCarver.random.get();
        random.setLargeFeatureSeed(configuration.salt, pos.x, pos.z);
        return random.fork();
    }

    protected boolean isNodeChunk(RilleCarverConfiguration config, ChunkPos pos) {
        return randomForChunk(config, pos).nextFloat() < config.probability;
    }

    protected @Nullable ChunkData getCacheEntry(RilleCarverConfiguration config, ChunkPos pos) {
        if (!isNodeChunk(config, pos)) return null;
        var cache = caches.computeIfAbsent(config, c -> CacheBuilder.newBuilder()
                .maximumSize(((long) c.maxConnectionDistance * c.maxConnectionDistance - (long) c.minConnectionDistance * c.minConnectionDistance) * c.searchLength * c.searchLength)
                .expireAfterAccess(Duration.ofSeconds(10))
                .softValues()
                .build(new CacheLoader<>() {
                    @Override
                    public ChunkData load(ChunkPos key) {
                        return new ChunkData(key);
                    }
                }));
        try {
            return cache.get(pos);
        } catch (ExecutionException ignored) {
            return null;
        }
    }

    protected final class ChunkData {
        final ChunkPos ourPos;
        @Nullable List<ChunkPos> knownNeighbors = null;
        @Nullable Map<ChunkPos, IAngleConnections> knownWithinAngleConnections;

        protected ChunkData(ChunkPos pos) {
            this.ourPos = pos;
        }

        // 0: this connection itself terminates
        // 1: this connection is alone
        // 2+: distance from nearest termination, capped by distance remaining
        public int getDistanceFromTermination(RilleCarverConfiguration config, ChunkPos frontier, int distanceRemaining) {
            if (distanceRemaining <= 0 || !getNeighbors(config).contains(frontier)) return 0;
            ChunkData frontierEntry = getCacheEntry(config, frontier);
            if (frontierEntry == null) return 0;
            IAngleConnections connections = getConnectionsWithinAngle(config, frontier, frontierEntry);
            if (connections.getBackingList().isEmpty()) return 0;
            int len = 0;
            for (ChunkPos pos : connections.getBackingList()) {
                len = Math.max(len, frontierEntry.getDistanceFromTermination(config, pos, distanceRemaining - 1));
            }
            return Math.min(len + 1, distanceRemaining);
        }

        public @NotNull IAngleConnections getConnectionsWithinAngle(RilleCarverConfiguration config, ChunkPos frontier, @Nullable ChunkData frontierEntry) {
            if (knownWithinAngleConnections == null) {
                knownWithinAngleConnections = new Object2ObjectOpenHashMap<>();
            }
            return knownWithinAngleConnections.computeIfAbsent(frontier, f -> {
                int dx = f.x - ourPos.x;
                int dz = f.z - ourPos.z;
                ChunkData actualFrontierEntry = frontierEntry;
                if (frontierEntry == null) {
                    actualFrontierEntry = getCacheEntry(config, f);
                    if (actualFrontierEntry == null) return EMPTY;
                }
                AngleConnections computed = new AngleConnections(f);
                for (ChunkPos pos : actualFrontierEntry.getNeighbors(config)) {
                    if (ourPos.equals(pos)) continue;
                    int odx = pos.x - f.x;
                    int odz = pos.z - f.z;
                    double dot = dx * odx + dz * odz;
                    if (dot < 0) continue;
                    // normalize
                    dot /= Math.sqrt((dx * dx + dz * dz) * (odx * odx + odz * odz));
                    if (dot < config.getMinAllowedDot() || dot > config.getMaxAllowedDot()) continue;
                    computed.add(pos);
                }
                return computed;
            });
        }

        public @NotNull List<ChunkPos> getNeighbors(RilleCarverConfiguration config) {
            if (knownNeighbors == null) {
                List<ChunkPos> building = new ObjectArrayList<>();
                for (int x = -config.maxConnectionDistance; x <= config.maxConnectionDistance; x++) {
                    for (int z = -config.maxConnectionDistance; z <= config.maxConnectionDistance; z++) {
                        if (x == 0 && z == 0) continue;
                        if (x * x + z * z > config.maxConnectionDistance * config.maxConnectionDistance) continue;
                        if (x * x + z * z < config.minConnectionDistance * config.minConnectionDistance) continue;
                        ChunkPos test = new ChunkPos(ourPos.x + x, ourPos.z + z);
                        if (isNodeChunk(config, test)) {
                            building.add(test);
                        }
                    }
                }
                knownNeighbors = building.stream().sorted(Comparator.comparingLong(ChunkPos::toLong)).toList();
            }
            return knownNeighbors;
        }
    }

    protected final class AngleConnections extends ObjectArrayList<ChunkPos> implements IAngleConnections {
        final ChunkPos neighbor;
        boolean averageFound = false;
        @Nullable ChunkPos average;

        protected AngleConnections(ChunkPos neighbor) {
            this.neighbor = neighbor;
        }

        public @Nullable ChunkPos getFilteredAverage(RilleCarverConfiguration config) {
            if (isEmpty()) return null;
            if (!averageFound) {
                ChunkData neighborData = getCacheEntry(config, neighbor);
                if (neighborData == null) {
                    averageFound = true;
                    return null;
                }
                int sumX = 0;
                int sumZ = 0;
                int count = 0;
                for (ChunkPos pos : this) {
                    int weight = neighborData.getConnectionsWithinAngle(config, pos, null).getBackingList().isEmpty() ? 1 : 10;
                    sumX += pos.x * weight;
                    sumZ += pos.z * weight;
                    count += weight;
                }
                if (count != 0) {
                average = new ChunkPos(sumX / count, sumZ / count);
                }
                averageFound = true;
            }
            return average;
        }

        @Override
        public @NotNull List<ChunkPos> getBackingList() {
            return this;
        }
    }

    protected interface IAngleConnections {
        @Nullable ChunkPos getFilteredAverage(RilleCarverConfiguration config);

        @NotNull List<ChunkPos> getBackingList();
    }

    protected static final IAngleConnections EMPTY = new IAngleConnections() {
        @Override
        public @Nullable ChunkPos getFilteredAverage(RilleCarverConfiguration config) {
            return null;
        }

        @Override
        public @NotNull List<ChunkPos> getBackingList() {
            return List.of();
        }
    };
}
