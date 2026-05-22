package dev.devce.rocketnautics.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import dev.devce.rocketnautics.api.capability.IBacktank;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BacktankUtil.class, remap = false)
public class BacktankUtilMixin {

    @WrapMethod(method = "maxAir(Lnet/minecraft/world/item/ItemStack;)I")
    private static int maxAirInterface(ItemStack instance, Operation<Integer> original) {
        if (instance.getItem() instanceof IBacktank backtank) {
            return backtank.getMaxAirCapacity(instance);
        }
        return original.call(instance);
    }

    @WrapMethod(method = "getAir")
    private static int getAirInterface(ItemStack instance, Operation<Integer> original) {
        if (instance.getItem() instanceof IBacktank backtank) {
            return backtank.getAir(instance);
        }
        return original.call(instance);
    }

    @WrapOperation(method = "consumeAir", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static <T> T setAirInterface(ItemStack instance, DataComponentType<? super T> p_331064_, T p_330775_, Operation<T> original) {
        if (instance.getItem() instanceof IBacktank backtank) {
            if (p_331064_ == AllDataComponents.BACKTANK_AIR) {
                return (T) backtank.setAir(instance, (int) p_330775_);
            }
        }
        return original.call(instance, p_331064_, p_330775_);
    }
}
