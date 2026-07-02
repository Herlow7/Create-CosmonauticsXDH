package dev.devce.rocketnautics.content.orbit;

import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;

import java.util.UUID;
import java.util.function.BiConsumer;

public interface PointerListenable {

    void rocketnautics$addListener(BiConsumer<UUID, GlobalSavedSubLevelPointer> listener);
}
