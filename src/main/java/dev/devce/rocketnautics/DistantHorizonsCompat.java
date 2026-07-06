package devce.rocketnautics;

import com.seibel.distanthorizons.api.DhApi.Delayed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class DistantHorizonsCompat {
    private static final double DH_UNLOAD_Y = 1049.0;
    private static boolean isDhDisabled = false;

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && isDhReady()) {
            if (player.getY() > 1049.0) {
                if (!isDhDisabled) {
                    unloadDistantHorizonsChunks();
                    isDhDisabled = true;
                }
            } else if (isDhDisabled || shouldForceLoadDistantHorizonsChunks()) {
                forceLoadDistantHorizonsChunks();
                isDhDisabled = false;
            }
        }
    }

    private static boolean isDhReady() {
        return Delayed.configs != null && Delayed.renderProxy != null && Delayed.worldProxy != null && Delayed.worldProxy.worldLoaded();
    }

    private static void unloadDistantHorizonsChunks() {
        Delayed.configs.graphics().renderingEnabled().setValue(false);
        Delayed.configs.worldGenerator().enableDistantWorldGeneration().setValue(false);
        Delayed.worldProxy.setReadOnly(true);
        Delayed.renderProxy.clearRenderDataCache();
    }

    private static boolean shouldForceLoadDistantHorizonsChunks() {
        return !(Boolean)Delayed.configs.graphics().renderingEnabled().getValue() || !(Boolean)Delayed.configs.worldGenerator().enableDistantWorldGeneration().getValue() || getCurrentReadOnly();
    }

    private static boolean getCurrentReadOnly() {
        try {
            return Delayed.worldProxy.getReadOnly();
        } catch (IllegalStateException var1) {
            return false;
        }
    }

    private static void forceLoadDistantHorizonsChunks() {
        Delayed.configs.graphics().renderingEnabled().setValue(true);
        Delayed.configs.worldGenerator().enableDistantWorldGeneration().setValue(true);
        Delayed.worldProxy.setReadOnly(false);
        Delayed.renderProxy.clearRenderDataCache();
    }
}
