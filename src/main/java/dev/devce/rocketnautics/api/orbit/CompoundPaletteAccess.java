package dev.devce.rocketnautics.api.orbit;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public record CompoundPaletteAccess(PaletteAccess posPos, PaletteAccess posNeg, PaletteAccess negPos, PaletteAccess negNeg) implements PaletteAccess {

    public CompoundPaletteAccess(PaletteAccess posPos, PaletteAccess posNeg, PaletteAccess negPos, PaletteAccess negNeg) {
        if (posPos.width() != posNeg.width() || negPos.width() != negNeg.width()) throw new IllegalArgumentException("Widths do not match!");
        if (posPos.height() != negPos.height() || posNeg.height() != negNeg.height()) throw new IllegalArgumentException("Heights do not match!");
        this.posPos = posPos;
        this.posNeg = posNeg;
        this.negPos = negPos;
        this.negNeg = negNeg;
    }

    @Override
    public int width() {
        return posPos.width() + negNeg.width();
    }

    @Override
    public int height() {
        return posPos.height() + negNeg.height();
    }

    @Override
    public int size() {
        return height() * width();
    }

    @Override
    public int getColor(int x, int y) {
        if (x >= negNeg().width()) {
            if (y >= negNeg().height()) {
                return posPos().getColor(x - negNeg().width(), y - negNeg().height());
            } else {
                return posNeg().getColor(x - negNeg().width(), y);
            }
        } else {
            if (y >= negNeg().height()) {
                return negPos().getColor(x, y - negNeg().height());
            } else {
                return negNeg().getColor(x, y);
            }
        }
    }

    @Override
    public @NotNull EnumSet<ColorFlags> getFlags(int x, int y) {
        if (x >= negNeg().width()) {
            if (y >= negNeg().height()) {
                return posPos().getFlags(x - negNeg().width(), y - negNeg().height());
            } else {
                return posNeg().getFlags(x - negNeg().width(), y);
            }
        } else {
            if (y >= negNeg().height()) {
                return negPos().getFlags(x, y - negNeg().height());
            } else {
                return negNeg().getFlags(x, y);
            }
        }
    }
}
