package dev.devce.rocketnautics.content.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.registry.RocketFloatProviders;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviderType;

public class ConfigurablyBiasedToBottomFloat extends FloatProvider {
    public static final MapCodec<ConfigurablyBiasedToBottomFloat> CODEC = RecordCodecBuilder.<ConfigurablyBiasedToBottomFloat>mapCodec(
                    p_146601_ -> p_146601_.group(
                                    Codec.FLOAT.fieldOf("min_inclusive").forGetter(p_146612_ -> p_146612_.minInclusive),
                                    Codec.FLOAT.fieldOf("max_exclusive").forGetter(p_146609_ -> p_146609_.maxExclusive),
                                    Codec.FLOAT.fieldOf("inner").forGetter(p_146609_ -> p_146609_.inner),
                                    ExtraCodecs.POSITIVE_INT.fieldOf("bias").forGetter(p_146609_ -> p_146609_.bias)
                            )
                            .apply(p_146601_, ConfigurablyBiasedToBottomFloat::new)
            )
            .validate(p_274956_ -> {
                if (p_274956_.maxExclusive <= p_274956_.minInclusive) {
                    return DataResult.error(() -> "Max must be larger than min, min_inclusive: " + p_274956_.minInclusive + ", max_exclusive: " + p_274956_.maxExclusive);
                }
                if (p_274956_.inner < 0) {
                    return DataResult.error(() -> "Inner must be nonnegative, inner: " + p_274956_.inner);
                }
                return DataResult.success(p_274956_);
            });

    protected final float minInclusive;
    protected final float maxExclusive;
    protected final float inner;
    protected final int bias;

    protected ConfigurablyBiasedToBottomFloat(float p_146595_, float p_146596_, float inner, int bias) {
        this.minInclusive = p_146595_;
        this.maxExclusive = p_146596_;
        this.inner = inner;
        this.bias = bias;
    }

    public static ConfigurablyBiasedToBottomFloat of(float min, float max, int bias) {
        return of(min, max, bias, 0);
    }

    public static ConfigurablyBiasedToBottomFloat of(float min, float max, int bias, float inner) {
        if (max <= min) {
            throw new IllegalArgumentException("Max must exceed min");
        }
        if (inner < 0) {
            throw new IllegalArgumentException("Inner cannot be negative");
        }
        if (bias <= 0) { // bias of 0 is equivalent to a uniform float
            throw new IllegalArgumentException("Bias must be positive");
        }
        return new ConfigurablyBiasedToBottomFloat(min, max, inner, bias);
    }

    @Override
    public float getMinValue() {
        return minInclusive;
    }

    @Override
    public float getMaxValue() {
        return maxExclusive;
    }

    @Override
    public FloatProviderType<?> getType() {
        return RocketFloatProviders.VERY_BIASED_TO_BOTTOM.get();
    }

    @Override
    public float sample(RandomSource random) {
        float span = maxExclusive - minInclusive - inner;
        if (span <= 0) {
            RocketNautics.LOGGER.warn("Empty width range: {}", this);
            return minInclusive;
        } else {
            float k = 1;
            for (int i = 0; i < bias; i++) {
                k *= random.nextFloat();
            }
            return random.nextFloat() * (k * span + inner) + minInclusive;
        }
    }

    @Override
    public String toString() {
        return "config_biased[" + this.minInclusive + "-" + this.maxExclusive + " inner: " + this.inner + "]";
    }
}
