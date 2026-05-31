package dev.devce.rocketnautics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.devce.rocketnautics.RocketNauticsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @WrapOperation(
        method = "handleRespawn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
        )
    )
    private void rocketnautics$redirectSetScreen(Minecraft instance, Screen screen, Operation<Void> original) {
        if (RocketNauticsClient.seamlessTransitionTicks > 0 && screen instanceof ReceivingLevelScreen) {
            return;
        }
        
        original.call(instance, screen);
    }
}
