package dev.devce.rocketnautics.api.orbit;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Locale;

public enum AtmosphereFlags implements StringRepresentable {
    LOW_DENSITY, DROWNING;
    public static final Codec<AtmosphereFlags> CODEC = StringRepresentable.fromEnum(AtmosphereFlags::values);

    public static EnumSet<AtmosphereFlags> empty() {
        return EnumSet.noneOf(AtmosphereFlags.class);
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
