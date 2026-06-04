package dev.devce.rocketnautics.content.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public record NoiseThreshold3dConditionSource(ResourceKey<NormalNoise.NoiseParameters> noise, double minThreshold, double maxThreshold)
        implements SurfaceRules.ConditionSource {
    public static final MapCodec<NoiseThreshold3dConditionSource> DATA_CODEC = RecordCodecBuilder.mapCodec(
            p_258995_ -> p_258995_.group(
                            ResourceKey.codec(Registries.NOISE).fieldOf("noise").forGetter(NoiseThreshold3dConditionSource::noise),
                            Codec.DOUBLE.fieldOf("min_threshold").forGetter(NoiseThreshold3dConditionSource::minThreshold),
                            Codec.DOUBLE.fieldOf("max_threshold").forGetter(NoiseThreshold3dConditionSource::maxThreshold)
                    )
                    .apply(p_258995_, NoiseThreshold3dConditionSource::new)
    );
    public static final KeyDispatchDataCodec<NoiseThreshold3dConditionSource> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);


    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    public SurfaceRules.Condition apply(final SurfaceRules.Context p_189640_) {
        final NormalNoise normalnoise = p_189640_.randomState.getOrCreateNoise(this.noise);

        class NoiseThresholdCondition extends SurfaceRules.LazyYCondition {
            NoiseThresholdCondition() {
                super(p_189640_);
            }

            @Override
            protected boolean compute() {
                double d0 = normalnoise.getValue(this.context.blockX, this.context.blockY, this.context.blockZ);
                return d0 >= NoiseThreshold3dConditionSource.this.minThreshold && d0 <= NoiseThreshold3dConditionSource.this.maxThreshold;
            }
        }

        return new NoiseThresholdCondition();
    }
}
