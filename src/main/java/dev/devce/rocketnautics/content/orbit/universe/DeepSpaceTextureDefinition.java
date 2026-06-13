package dev.devce.rocketnautics.content.orbit.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.devce.rocketnautics.api.orbit.ColorFlags;
import dev.devce.rocketnautics.api.orbit.ColorPalette;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface DeepSpaceTextureDefinition {
    Codec<DeepSpaceTextureDefinition> CODEC = Type.CODEC.dispatch(DeepSpaceTextureDefinition::type, Type::typeCodec);

    enum Type implements StringRepresentable {
        RESLOC(ResourceLocationDriven.CODEC),
        BIOME_SAMPLER(BiomeSampleDriven.CODEC);
        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final MapCodec<? extends DeepSpaceTextureDefinition> typeCodec;

        Type(MapCodec<? extends DeepSpaceTextureDefinition> codec) {
            this.typeCodec = codec;
        }

        public MapCodec<? extends DeepSpaceTextureDefinition> typeCodec() {
            return typeCodec;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    @NotNull Type type();


    record ResourceLocationDriven(ResourceLocation texture) implements DeepSpaceTextureDefinition {
        public static final MapCodec<ResourceLocationDriven> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("texture").forGetter(ResourceLocationDriven::texture)
        ).apply(instance, ResourceLocationDriven::new));

        @Override
        public @NotNull Type type() {
            return Type.RESLOC;
        }
    }

    record BiomeSampleDriven(List<ColorEntry> colors, byte fallbackR, byte fallbackG, byte fallbackB) implements DeepSpaceTextureDefinition {
        public static final MapCodec<BiomeSampleDriven> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ColorEntry.CODEC.listOf().fieldOf("colors").forGetter(BiomeSampleDriven::colors),
                Codec.INT.fieldOf("fallback_red").forGetter(d -> d.fallbackR & 0xFF),
                Codec.INT.fieldOf("fallback_green").forGetter(d -> d.fallbackG & 0xFF),
                Codec.INT.fieldOf("fallback_blue").forGetter(d -> d.fallbackB & 0xFF)
        ).apply(instance, (l, r, g, b) -> new BiomeSampleDriven(l.stream().sorted(Comparator.comparingInt(ColorEntry::prio).reversed()).toList(), r.byteValue(), g.byteValue(), b.byteValue())));

        public int match(Holder<Biome> biome) {
            for (ColorEntry entry : colors) {
                if (entry.match(biome)) return entry.packedColor();
            }
            return packedFallback();
        }

        public int packedFallback() {
            return ColorPalette.packColor((byte) 255, fallbackR, fallbackG, fallbackB);
        }

        @Override
        public @NotNull Type type() {
            return Type.BIOME_SAMPLER;
        }

        public record ColorEntry(List<ResourceKey<Biome>> matchingBiomes, List<TagKey<Biome>> matchingTags, int prio, byte r, byte g, byte b, EnumSet<ColorFlags> flags) {
            public static final Codec<ColorEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ResourceKey.codec(Registries.BIOME).listOf().optionalFieldOf("matching_biomes", List.of()).forGetter(ColorEntry::matchingBiomes),
                    TagKey.codec(Registries.BIOME).listOf().optionalFieldOf("matching_tags", List.of()).forGetter(ColorEntry::matchingTags),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(ColorEntry::prio),
                    Codec.INT.fieldOf("red").forGetter(d -> d.r & 0xFF),
                    Codec.INT.fieldOf("green").forGetter(d -> d.g & 0xFF),
                    Codec.INT.fieldOf("blue").forGetter(d -> d.b & 0xFF),
                    ColorFlags.CODEC.listOf().xmap(ColorFlags::properCopy, ArrayList::new).optionalFieldOf("flags", ColorFlags.empty()).forGetter(ColorEntry::flags)
            ).apply(instance, (mb, mt, p, r, g, b, f) -> new ColorEntry(mb, mt, p, r.byteValue(), g.byteValue(), b.byteValue(), f)));

            public int packedColor() {
                return ColorPalette.packColor((byte) 255, r, g, b);
            }

            public boolean match(Holder<Biome> biome) {
                for (ResourceKey<Biome> match : matchingBiomes) {
                    if (biome.is(match)) return true;
                }
                for (TagKey<Biome> tag : matchingTags) {
                    if (biome.is(tag)) return true;
                }
                return false;
            }
        }
    }
}
