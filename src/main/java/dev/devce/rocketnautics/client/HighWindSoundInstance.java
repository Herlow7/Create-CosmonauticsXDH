package dev.devce.rocketnautics.client;

import dev.devce.rocketnautics.registry.RocketSounds;
import dev.devce.rocketnautics.SkyDataHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A looping sound instance for the high altitude rushing wind effect.
 * Dynamically fades in as the rocket gains altitude and fades out as it transitions to vacuum.
 */
@OnlyIn(Dist.CLIENT)
public class HighWindSoundInstance extends AbstractTickableSoundInstance {

    public HighWindSoundInstance() {
        super(RocketSounds.HIGH_WIND.get(), SoundSource.WEATHER, Minecraft.getInstance().level.getRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.pitch = 1.0f;
        this.relative = true; // Direct stereo play (headphone space) instead of panning around
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            this.stop();
            return;
        }

        double camY = mc.gameRenderer.getMainCamera().getPosition().y + SkyDataHandler.getHeightOffsetForLevel(mc.level.dimension());

        // Atmospheric wind fade-in/fade-out limits
        float targetVolume = 0.0f;
        if (camY >= 800.0 && camY <= 2200.0) {
            if (camY < 1200.0) {
                targetVolume = (float) ((camY - 800.0) / 400.0);
            } else if (camY > 1800.0) {
                targetVolume = (float) (1.0 - (camY - 1800.0) / 400.0);
            } else {
                targetVolume = 1.0f;
            }
        }

        // Smoothly interpolate wind volume to prevent sharp pops/clicks
        this.volume = Mth.lerp(0.05f, this.volume, targetVolume);

        // Stop the sound instance completely when outside the atmospheric zone
        if (this.volume <= 0.001f && (camY < 800.0 || camY > 2200.0)) {
            this.stop();
        }
    }
}
