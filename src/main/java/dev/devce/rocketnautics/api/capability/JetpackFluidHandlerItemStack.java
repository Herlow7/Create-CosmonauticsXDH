package dev.devce.rocketnautics.api.capability;

import com.simibubi.create.AllEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

import java.util.function.Supplier;

public class JetpackFluidHandlerItemStack extends FluidHandlerItemStack {
    protected final int capacityPerEnchant;
    /**
     * @param componentType The data component type to use for data storage.
     * @param container     The container itemStack, data is stored on it directly under a component.
     * @param capacity      The maximum capacity of this fluid tank.
     * @param capacityPerEnchant The capacity gained per level of the capacity enchant
     */
    public JetpackFluidHandlerItemStack(Supplier<DataComponentType<SimpleFluidContent>> componentType, ItemStack container, int capacity, int capacityPerEnchant) {
        super(componentType, container, capacity);
        this.capacityPerEnchant = capacityPerEnchant;
    }

    @Override
    public int getTankCapacity(int tank) {
        int enchantLevel = 0;
        ItemEnchantments enchants = getContainer().getTagEnchantments();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            if (entry.getKey().is(AllEnchantments.CAPACITY)) {
                enchantLevel = entry.getIntValue();
                break;
            }
        }
        return capacity + enchantLevel * capacityPerEnchant;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.is(Tags.Fluids.LAVA);
    }

    public boolean canFillFluidType(FluidStack fluid) {
        return isFluidValid(0, fluid);
    }
}
