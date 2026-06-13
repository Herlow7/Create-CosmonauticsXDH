

package dev.devce.rocketnautics;

import dev.devce.rocketnautics.api.orbit.ColorPalette;
import dev.devce.rocketnautics.content.orbit.DeepSpaceData;
import dev.devce.rocketnautics.content.orbit.universe.CubePlanet;
import dev.devce.rocketnautics.content.orbit.universe.DeepSpaceTextureDefinition;
import dev.devce.rocketnautics.network.PlanetMapPayload;
import it.unimi.dsi.fastutil.doubles.DoubleObjectPair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SkyDataHandler {

    public static final int MAX_POWER_SIZE = 24;
    public static final int MAX_TRUE_SIZE = 2 << MAX_POWER_SIZE; 
    public static final int MIN_POWER_SIZE = 14;

    public static final int SCALE_FACTOR = 3;

    public static final Map<ServerLevel, SkyDataHandler> HANDLERS = new HashMap<>();

    
    public static final Map<ResourceKey<Level>, DoubleObjectPair<ResourceKey<Level>>> OVERRIDES = new HashMap<>();

    public final ServerLevel level;
    protected final RecursiveDataSquare root;
    protected final DeepSpaceDataSquare deepSpace;
    protected final DeepSpaceTextureDefinition.BiomeSampleDriven mapper;

    public SkyDataHandler(ServerLevel level, DeepSpaceTextureDefinition.BiomeSampleDriven mapper) {
        this.level = level;
        this.root = new RecursiveDataSquare(null, MAX_POWER_SIZE + 1, -MAX_TRUE_SIZE, -MAX_TRUE_SIZE);
        this.mapper = mapper;
        this.deepSpace = new DeepSpaceDataSquare(level.getWorldBorder());
        level.getWorldBorder().addListener(deepSpace);
    }

    
    public static double getHeightOffsetForLevel(ResourceKey<Level> level) {
        DoubleObjectPair<ResourceKey<Level>> pair = OVERRIDES.get(level);
        if (pair == null) return 0;
        return pair.firstDouble();
    }

    
    public static SkyDataHandler getHandlerForLevel(ServerLevel level) {
        if (OVERRIDES.containsKey(level.dimension())) {
            level = level.getServer().getLevel(OVERRIDES.get(level.dimension()).right());
        }
        return HANDLERS.computeIfAbsent(level, l -> {
            DeepSpaceData d = DeepSpaceData.getInstance(l.getServer());
            CubePlanet associated = d.getUniverse().getPlanetByDimension(l.dimension());
            DeepSpaceTextureDefinition.BiomeSampleDriven mapper = associated == null ? null : (associated.textureDefinition() instanceof DeepSpaceTextureDefinition.BiomeSampleDriven sample ? sample : null);
            return new SkyDataHandler(l, mapper);
        });
    }

    public ColorPalette getRenderDataForDeepSpace(int powerSizeClamp) {
        // TODO respect power size clamp
        return deepSpace.getRenderData();
    }

    public PlanetMapPayload getRenderDataAtScaleAndPosition(int powerSize, int trueX, int trueZ) {
        
        DataSquare square = getSquareAtPosition(powerSize, trueX, trueZ);
        
        boolean posX = square.isPosX(trueX);
        boolean posZ = square.isPosZ(trueZ);
        DataSquare posXPosZ;
        DataSquare posXNegZ;
        DataSquare negXPosZ;
        DataSquare negXNegZ;
        int shift = toTrueSize(square.powerSize);
        if (posX) {
            if (posZ) {
                negXNegZ = square;

                posXPosZ = getSquareAtPosition(powerSize, trueX + shift, trueZ + shift);
                posXNegZ = getSquareAtPosition(powerSize, trueX + shift, trueZ);
                negXPosZ = getSquareAtPosition(powerSize, trueX, trueZ + shift);
            } else {
                negXPosZ = square;

                posXPosZ = getSquareAtPosition(powerSize, trueX + shift, trueZ);
                posXNegZ = getSquareAtPosition(powerSize, trueX + shift, trueZ - shift);
                negXNegZ = getSquareAtPosition(powerSize, trueX, trueZ - shift);
            }
        } else {
            if (posZ) {
                posXNegZ = square;

                posXPosZ = getSquareAtPosition(powerSize, trueX, trueZ + shift);
                negXPosZ = getSquareAtPosition(powerSize, trueX - shift, trueZ + shift);
                negXNegZ = getSquareAtPosition(powerSize, trueX - shift, trueZ);
            } else {
                posXPosZ = square;

                posXNegZ = getSquareAtPosition(powerSize, trueX, trueZ - shift);
                negXPosZ = getSquareAtPosition(powerSize, trueX - shift, trueZ);
                negXNegZ = getSquareAtPosition(powerSize, trueX - shift, trueZ - shift);
            }
        }

        return new PlanetMapPayload(square.powerSize, posXPosZ.trueNegXCorner, posXPosZ.trueNegZCorner, posXPosZ.getRenderData(), posXNegZ.getRenderData(), negXPosZ.getRenderData(), negXNegZ.getRenderData());
    }

    protected DataSquare getSquareAtPosition(int powerSize, int trueX, int trueZ) {
        DataSquare square = root;
        do { 
            if (square instanceof RecursiveDataSquare r) {
                square = r.getChildAtTruePosition(trueX, trueZ);
            } else {
                break;
            }
        } while (powerSize < square.powerSize);
        return square;
    }

    public static int targetSizeForHeight(double y) {
        int log2Height = (int) (Math.log(y) / Math.log(2));
        return Math.clamp(log2Height + SCALE_FACTOR, MIN_POWER_SIZE, MAX_POWER_SIZE);
    }

    public static double targetSizeForHeightContinuous(double y) {
        double log2Height = Math.log(y) / Math.log(2);
        return Math.clamp(log2Height + SCALE_FACTOR, MIN_POWER_SIZE, MAX_POWER_SIZE);
    }

    public static int toTrueSize(int powerSize) {
        return 2 << powerSize;
    }

    protected class RecursiveDataSquare extends DataSquare {
        private DataSquare childPosXPosZ;
        private DataSquare childPosXNegZ;
        private DataSquare childNegXPosZ;
        private DataSquare childNegXNegZ;

        public RecursiveDataSquare(@Nullable RecursiveDataSquare parent, int powerSize, int trueNegXCorner, int trueNegZCorner) {
            super(parent, powerSize, trueNegXCorner, trueNegZCorner);
        }

        public DataSquare getChildAtTruePosition(int trueX, int trueZ) {
            boolean posX = isPosX(trueX);
            boolean posZ = isPosZ(trueZ);
            if (posX) {
                if (posZ) {
                    return getChildPosXPosZ();
                } else {
                    return getChildPosXNegZ();
                }
            } else {
                if (posZ) {
                    return getChildNegXPosZ();
                } else {
                    return getChildNegXNegZ();
                }
            }
        }

        private DataSquare createChild(int trueNegXCorner, int trueNegZCorner) {
            if (powerSize > MIN_POWER_SIZE + 1) {
                return new RecursiveDataSquare(this, powerSize - 1, trueNegXCorner, trueNegZCorner);
            }
            return new DataSquare(this, powerSize - 1, trueNegXCorner, trueNegZCorner);
        }

        public DataSquare getChildNegXNegZ() {
            if (childNegXNegZ == null) {
                childNegXNegZ = createChild(trueNegXCorner, trueNegZCorner);
            }
            return childNegXNegZ;
        }

        public DataSquare getChildNegXPosZ() {
            if (childNegXPosZ == null) {
                childNegXPosZ = createChild(trueNegXCorner, trueNegZCorner + toTrueSize(powerSize - 1));
            }
            return childNegXPosZ;
        }

        public DataSquare getChildPosXNegZ() {
            if (childPosXNegZ == null) {
                childPosXNegZ = createChild(trueNegXCorner + toTrueSize(powerSize - 1), trueNegZCorner);
            }
            return childPosXNegZ;
        }

        public DataSquare getChildPosXPosZ() {
            if (childPosXPosZ == null) {
                childPosXPosZ = createChild(trueNegXCorner + toTrueSize(powerSize - 1), trueNegZCorner + toTrueSize(powerSize - 1));
            }
            return childPosXPosZ;
        }
    }

    protected class DataSquare {
        public final @Nullable RecursiveDataSquare parent;
        public final int powerSize;
        public final int trueNegXCorner;
        public final int trueNegZCorner;

        protected ColorPalette renderData;

        public DataSquare(@Nullable RecursiveDataSquare parent, int powerSize, int trueNegXCorner, int trueNegZCorner) {
            this.parent = parent;
            this.powerSize = powerSize;
            this.trueNegXCorner = trueNegXCorner;
            this.trueNegZCorner = trueNegZCorner;
        }

        public ColorPalette getRenderData() {
            if (renderData == null) {
                buildRenderData();
            }
            return renderData;
        }

        public boolean isPosX(int trueX) {
            return trueX - trueNegXCorner >= toTrueSize(powerSize - 1);
        }

        public boolean isPosZ(int trueZ) {
            return trueZ - trueNegZCorner >= toTrueSize(powerSize - 1);
        }

        protected void buildRenderData() {
            renderData = ColorPalette.EMPTY;
            if (mapper == null) return;
            var builder = new ColorPalette.PaletteBuilder(mapper.packedFallback());
            try {
                BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
                Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
                Object2IntMap<Holder<Biome>> cache = new Object2IntOpenHashMap<>();

                int step = toTrueSize(powerSize - 8); 

                for (int x = 0; x < 256; x++) {
                    for (int z = 0; z < 256; z++) {
                        int worldX = trueNegXCorner + x * step;
                        int worldZ = trueNegZCorner + z * step;
                        // use some arbitrarily large value as our y picker so we don't get underground biomes
                        Holder<Biome> biome = source.getNoiseBiome(worldX >> 2, 1000, worldZ >> 2, sampler);
                        builder.write(x, z, cache.computeIfAbsent(biome, mapper::match));
                    }
                }

                for (DeepSpaceTextureDefinition.BiomeSampleDriven.ColorEntry entry : mapper.colors()) {
                    builder.attachFlags(entry.packedColor(), entry.flags());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            renderData = builder.build();
        }
    }

    protected class DeepSpaceDataSquare implements BorderChangeListener {
        protected ColorPalette renderData = null;
        protected double diameter;
        protected double centerX;
        protected double centerZ;

        public DeepSpaceDataSquare(WorldBorder border) {
            if (mapper == null) {
                renderData = ColorPalette.EMPTY;
            }
            this.diameter = border.getSize();
            this.centerX = border.getCenterX();
            this.centerZ = border.getCenterZ();
        }

        public ColorPalette getRenderData() {
            synchronized (this) {
                if (renderData == null) {
                    var builder = new ColorPalette.PaletteBuilder(mapper.packedFallback());
                    try {
                        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
                        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
                        Object2IntMap<Holder<Biome>> cache = new Object2IntOpenHashMap<>();

                        for (int x = 0; x < 256; x++) {
                            for (int z = 0; z < 256; z++) {
                                int worldX = (int) (diameter * (x - 128) / 128 + centerX);
                                int worldZ = (int) (diameter * (z - 128) / 128 + centerZ);
                                // use some arbitrarily large value as our y picker so we don't get underground biomes
                                Holder<Biome> biome = source.getNoiseBiome(worldX >> 2, 1000, worldZ >> 2, sampler);
                                builder.write(x, z, cache.computeIfAbsent(biome, mapper::match));
                            }
                        }

                        for (DeepSpaceTextureDefinition.BiomeSampleDriven.ColorEntry entry : mapper.colors()) {
                            builder.attachFlags(entry.packedColor(), entry.flags());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    renderData = builder.build();
                }
                return renderData;
            }
        }

        @Override
        public void onBorderSizeSet(@NotNull WorldBorder p_61847_, double p_61848_) {
            renderData = null;
            diameter = p_61848_;
        }

        @Override
        public void onBorderSizeLerping(@NotNull WorldBorder p_61852_, double p_61853_, double p_61854_, long p_61855_) {
            this.onBorderSizeSet(p_61852_, p_61854_);
        }

        @Override
        public void onBorderCenterSet(@NotNull WorldBorder p_61849_, double p_61850_, double p_61851_) {
            renderData = null;
            centerX = p_61850_;
            centerZ = p_61851_;
        }

        @Override
        public void onBorderSetWarningTime(@NotNull WorldBorder p_61856_, int p_61857_) {}

        @Override
        public void onBorderSetWarningBlocks(@NotNull WorldBorder p_61860_, int p_61861_) {}

        @Override
        public void onBorderSetDamagePerBlock(@NotNull WorldBorder p_61858_, double p_61859_) {}

        @Override
        public void onBorderSetDamageSafeZOne(@NotNull WorldBorder p_61862_, double p_61863_) {}
    }
}
