package dev.devce.rocketnautics.api.capability;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllEnchantments;
import com.simibubi.create.infrastructure.config.AllConfigs;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public interface IBacktank {

    default int getMaxAirCapacity(ItemStack backtank) {
        int enchantLevel = 0;
        ItemEnchantments enchants = backtank.getTagEnchantments();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            if (entry.getKey().is(AllEnchantments.CAPACITY)) {
                enchantLevel = entry.getIntValue();
                break;
            }
        }
        return AllConfigs.server().equipment.airInBacktank.get()
                + AllConfigs.server().equipment.enchantedBacktankCapacity.get() * enchantLevel;
    }

    default int getAir(ItemStack backtank) {
        return Math.min(backtank.getOrDefault(AllDataComponents.BACKTANK_AIR, 0), getMaxAirCapacity(backtank));
    }

    default Integer setAir(ItemStack backtank, int newAir) {
        return backtank.set(AllDataComponents.BACKTANK_AIR, Math.min(newAir, getMaxAirCapacity(backtank)));
    }
}
