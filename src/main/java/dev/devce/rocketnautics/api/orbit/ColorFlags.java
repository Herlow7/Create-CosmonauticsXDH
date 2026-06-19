package dev.devce.rocketnautics.api.orbit;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;

public enum ColorFlags implements StringRepresentable {
    OCEAN;
    public static final Codec<ColorFlags> CODEC = StringRepresentable.fromEnum(ColorFlags::values);

    public static EnumSet<ColorFlags> empty() {
        return EnumSet.noneOf(ColorFlags.class);
    }

    public static EnumSet<ColorFlags> properCopy(Collection<ColorFlags> collection) {
        if (collection.isEmpty()) return empty();
        return EnumSet.copyOf(collection);
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
