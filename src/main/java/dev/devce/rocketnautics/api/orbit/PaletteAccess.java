package dev.devce.rocketnautics.api.orbit;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public interface PaletteAccess {

    int width();

    int height();

    int size();

    int getColor(int x, int y);

    @NotNull EnumSet<ColorFlags> getFlags(int x, int y);
}
