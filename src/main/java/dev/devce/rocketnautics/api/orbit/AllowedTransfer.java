package dev.devce.rocketnautics.api.orbit;

import com.mojang.serialization.Codec;
import dev.devce.rocketnautics.content.orbit.universe.PlanetDimensionData;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum AllowedTransfer implements StringRepresentable {
    ALL, NONE, TO_SPACE, TO_DIMENSION;
    public static final Codec<AllowedTransfer> CODEC = StringRepresentable.fromEnum(AllowedTransfer::values);

    public boolean allowToSpace() {
        return this == ALL || this == TO_SPACE;
    }

    public boolean allowToDimension() {
        return this == ALL || this == TO_DIMENSION;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
