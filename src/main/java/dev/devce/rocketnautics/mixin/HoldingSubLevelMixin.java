package dev.devce.rocketnautics.mixin;

import dev.devce.rocketnautics.content.orbit.PointerListenable;
import dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

@Mixin(HoldingSubLevel.class)
public abstract class HoldingSubLevelMixin implements PointerListenable {

    @Shadow
    public abstract @NotNull SubLevelData data();

    @Unique
    private final List<BiConsumer<UUID, GlobalSavedSubLevelPointer>> rocketnautics$listeners = new ObjectArrayList<>();


    @Override
    public void rocketnautics$addListener(BiConsumer<UUID, GlobalSavedSubLevelPointer> listener) {
        rocketnautics$listeners.add(listener);
    }

    @Inject(method = "setPointer", at = @At("TAIL"))
    private void onPointerChange(GlobalSavedSubLevelPointer pointer, CallbackInfo ci) {
        rocketnautics$listeners.forEach(l -> l.accept(data().uuid(), pointer));
    }
}
