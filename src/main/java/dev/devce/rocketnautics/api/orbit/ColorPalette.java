package dev.devce.rocketnautics.api.orbit;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMaps;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.EnumSet;
import java.util.Map;

public record ColorPalette(int width, byte @NotNull [] dataArray, int @NotNull [] palette, @UnmodifiableView Byte2ObjectMap<EnumSet<ColorFlags>> flags) implements PaletteAccess {
    public static final ColorPalette EMPTY = new ColorPalette(256, new byte[256 * 256], new int[1], Byte2ObjectMaps.emptyMap());
    public static final StreamCodec<FriendlyByteBuf, ColorPalette> CODEC_S = StreamCodec.of(
            (buf, v) -> {
                buf.writeVarInt(v.width());
                buf.writeByteArray(v.dataArray());
                buf.writeVarInt(v.palette().length);
                for (int i : v.palette()) {
                    buf.writeInt(i);
                }
                buf.writeMap(v.flags(), ByteBufCodecs.BYTE, (b, c) -> b.writeEnumSet(c, ColorFlags.class));
            }, buf -> {
                int width = buf.readVarInt();
                byte[] data = buf.readByteArray();
                int s = buf.readVarInt();
                int[] palette = new int[s];
                for (int i = 0; i < s; i++) {
                    palette[i] = buf.readInt();
                }
                Map<Byte, EnumSet<ColorFlags>> map = buf.readMap(ByteBufCodecs.BYTE, b -> b.readEnumSet(ColorFlags.class));
                return new ColorPalette(width, data, palette, new Byte2ObjectOpenHashMap<>(map));
            }
    );

    @Override
    public int height() {
        return dataArray.length / width;
    }

    @Override
    public int size() {
        return dataArray.length;
    }

    @Override
    public int getColor(int x, int y) {
        if (x > width() || y > height()) throw new IndexOutOfBoundsException();
        return palette[dataArray[x + width() * y]];
    }

    public static int[] unpackColorARGB(int packed) {
        int a = (packed >> 24) & 0xFF;
        int b = (packed >> 16) & 0xFF;
        int g = (packed >> 8) & 0xFF;
        int r = packed & 0xFF;
        return new int[] {a, r, g, b};
    }

    // note -- do not use this packer if any of your inputs are byte type, because it will preserve negatives
    public static int packColor(int a, int r, int g, int b) {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    public static int packColor(byte a, byte r, byte g, byte b) {
        return ((a & 0xFF) << 24) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | r & 0xFF;
    }

    @Override
    public @NotNull EnumSet<ColorFlags> getFlags(int x, int y) {
        if (x > width() || y > height()) throw new IndexOutOfBoundsException();
        return flags.getOrDefault(dataArray[x + width() * y], ColorFlags.empty());
    }

    public static class PaletteBuilder {
        public final int width;
        public final byte[] data;
        public final Int2ByteMap palette = new Int2ByteOpenHashMap();
        public final Int2ObjectMap<EnumSet<ColorFlags>> flags = new Int2ObjectOpenHashMap<>();

        public PaletteBuilder(int fallback) {
            this(256, fallback);
        }

        public PaletteBuilder(int sideLength, int fallback) {
            this(sideLength, sideLength, fallback);
        }

        public PaletteBuilder(int width, int height, int fallback) {
            this.width = width;
            data = new byte[width * height];
            palette.put(fallback, (byte) 0);
        }

        public void attachFlags(int packedColor, EnumSet<ColorFlags> flags) {
            this.flags.put(packedColor, flags);
        }

        public void write(int x, int y, int packedColor) {
            if (x > width || y > data.length / width) throw new IndexOutOfBoundsException();
            if (!palette.containsKey(packedColor)) {
                if (palette.size() >= 256) {
                    throw new IllegalStateException("Palette already has 256 values defined!");
                }
                byte key = (byte) palette.size();
                palette.put(packedColor, key);
                data[x + width * y] = key;
            } else {
                data[x + width * y] = palette.get(packedColor);
            }
        }

        public ColorPalette build() {
            int[] builtPalette = new int[palette.size()];
            Byte2ObjectMap<EnumSet<ColorFlags>> builtFlags = new Byte2ObjectOpenHashMap<>();
            for (var entry : palette.int2ByteEntrySet()) {
                builtPalette[entry.getByteValue()] = entry.getIntKey();
                if (flags.containsKey(entry.getIntKey())) {
                    builtFlags.put(entry.getByteValue(), flags.get(entry.getIntKey()));
                }
            }
            return new ColorPalette(width, data, builtPalette, builtFlags.isEmpty() ? EMPTY.flags() : builtFlags);
        }
    }
}
