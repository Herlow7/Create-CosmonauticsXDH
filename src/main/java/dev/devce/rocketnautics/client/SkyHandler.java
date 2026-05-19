

// Webhook test commit
package dev.devce.rocketnautics.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.SkyDataHandler;
import dev.devce.rocketnautics.network.PlanetMapRequestPayload;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Handles custom sky rendering for high altitudes and the space dimension.
 * This includes atmospheric fog color adjustments, procedural planet rendering,
 * and dynamic planet map texture management.
 */
@EventBusSubscriber(modid = RocketNautics.MODID, value = Dist.CLIENT)
public class SkyHandler {

    /**
     * Adjusts the fog color towards black as the player ascends into space.
     */
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        double y = mc.gameRenderer.getMainCamera().getPosition().y + SkyDataHandler.getHeightOffsetForLevel(mc.level.dimension());
        if (y > 1000.0) {
            // Gradually fade fog to black above 1000m
            float factor = (float) Mth.clamp((y - 1000.0) / 1000.0, 0.0, 1.0);
            
            event.setRed(Mth.lerp(factor, event.getRed(), 0.0f));
            event.setGreen(Mth.lerp(factor, event.getGreen(), 0.0f));
            event.setBlue(Mth.lerp(factor, event.getBlue(), 0.0f));
        }
    }

    /**
     * Renders the custom planet geometry after the vanilla sky has been drawn.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        double camY = camera.getPosition().y + SkyDataHandler.getHeightOffsetForLevel(mc.level.dimension());
        if (camY < 1000.0) return;

        // Determine visibility based on altitude
        float visibility = (float) Mth.clamp((camY - 1000.0) / 500.0, 0.0, 1.0);
        if (visibility <= 0) return;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        Matrix4f matrix = poseStack.last().pose();
        matrix.identity();
        
        // Counteract camera rotation to render in fixed screen-space or world-aligned space
        Quaternionf invRot = new Quaternionf(camera.rotation());
        invRot.conjugate();
        poseStack.mulPose(invRot);

        int renderDist = mc.options.renderDistance().get();
        // Calculate parallax based on altitude: higher means less parallax (planet seems further)
        float parallaxFactor = (float) (renderDist / Math.max(100.0, camY)); 
        double camX = camera.getPosition().x;
        double camZ = camera.getPosition().z;

        // Ensure all procedural textures are initialized
        ensurePlanetTexObj();
        ensureCloudTexture();
        ensureHaloTexture();
        
        // Request map updates from server if player moved too far
        updatePlanetTex(camX, camY, camZ);
        
        // Cross-fade between old and new planet map textures
        if (texFade > 0) {
            texFade = Math.max(0, texFade - event.getPartialTick().getRealtimeDeltaTicks() / 20);
        }
        
        // Render planet with layered effects (Map + Clouds + Halo)
        renderPlanet(PLANET_TEXTURE_OBJ_LAST, camX, camY, camZ, renderDist, parallaxFactor, matrix, texFade * visibility);
        renderPlanet(PLANET_TEXTURE_OBJ, camX, camY, camZ, renderDist, parallaxFactor, matrix, (1 - texFade) * visibility);
        
        poseStack.popPose();
    }

    /**
     * Renders the planet quad with Map, Clouds, and Halo layers.
     */
    private static void renderPlanet(PlanetRenderInfo planet, double camX, double camY, double camZ, float renderDist, float parallaxFactor, Matrix4f matrix, float visibility) {
        if (visibility <= 0) return;
        
        // Calculate relative position based on parallax
        float relX = (float) ((planet.getCenterX() - camX) * parallaxFactor);
        float relY = -renderDist; // Render "below" the player
        float relZ = (float) ((planet.getCenterZ() - camZ) * parallaxFactor);

        float prettyness = computePrettyness(planet, camY);
        
        relX = Mth.lerp(prettyness, relX, 0);
        relZ = Mth.lerp(prettyness, relZ, 0);
        
        // Determine quad size based on altitude and scale factor
        double trueSize = SkyDataHandler.toTrueSize(planet.getPowerSize());
        double optimalSize = camY * (2 << SkyDataHandler.SCALE_FACTOR);
        double result = Math.min(prettyness > 0 ? optimalSize : trueSize, SkyDataHandler.toTrueSize(SkyDataHandler.MAX_POWER_SIZE));
        float size = (float) (result * (renderDist / Math.max(100.0, camY)));

        // Setup rendering state
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        
        // --- Layer 1: Planet Surface Map ---
        if (planet.getTexID() != null) {
            RenderSystem.setShaderTexture(0, planet.getTexID());
        } else {
            RenderSystem.setShaderTexture(0, ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "textures/environment/planet_map.png"));
        }
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        float r = 1.0f, g = 1.0f, b = 1.0f;
        bufferbuilder.addVertex(matrix, relX - size, relY, relZ - size).setColor(r, g, b, visibility).setUv(0.0f, 0.0f);
        bufferbuilder.addVertex(matrix, relX - size, relY, relZ + size).setColor(r, g, b, visibility).setUv(0.0f, 1.0f);
        bufferbuilder.addVertex(matrix, relX + size, relY, relZ + size).setColor(r, g, b, visibility).setUv(1.0f, 1.0f);
        bufferbuilder.addVertex(matrix, relX + size, relY, relZ - size).setColor(r, g, b, visibility).setUv(1.0f, 0.0f);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());

        // --- Layer 2: Scrolling Clouds ---
        if (CLOUD_TEXTURE_ID != null) {
            RenderSystem.setShaderTexture(0, CLOUD_TEXTURE_ID);
            long factor = 1000L * SkyDataHandler.toTrueSize(planet.getPowerSize() / 2);
            float timeOffset = (System.currentTimeMillis() % (20L * factor)) / (float) factor;

            BufferBuilder cloudBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            cloudBuilder.addVertex(matrix, relX - size, relY, relZ - size).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(0.0f + timeOffset, 0.0f);
            cloudBuilder.addVertex(matrix, relX - size, relY, relZ + size).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(0.0f + timeOffset, 1.0f);
            cloudBuilder.addVertex(matrix, relX + size, relY, relZ + size).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(1.0f + timeOffset, 1.0f);
            cloudBuilder.addVertex(matrix, relX + size, relY, relZ - size).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(1.0f + timeOffset, 0.0f);
            BufferUploader.drawWithShader(cloudBuilder.buildOrThrow());
        }

        // --- Layer 3: Atmospheric Halo (Glow) ---
        if (HALO_TEXTURE_ID != null) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE); // Additive blend
            RenderSystem.setShaderTexture(0, HALO_TEXTURE_ID);

            float haloSize = size * 1.3f;
            BufferBuilder haloBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            haloBuilder.addVertex(matrix, relX - haloSize, relY, relZ - haloSize).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(0.0f, 0.0f);
            haloBuilder.addVertex(matrix, relX - haloSize, relY, relZ + haloSize).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(0.0f, 1.0f);
            haloBuilder.addVertex(matrix, relX + haloSize, relY, relZ + haloSize).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(1.0f, 1.0f);
            haloBuilder.addVertex(matrix, relX + haloSize, relY, relZ - haloSize).setColor(1.0f, 1.0f, 1.0f, visibility).setUv(1.0f, 0.0f);
            BufferUploader.drawWithShader(haloBuilder.buildOrThrow());

            RenderSystem.defaultBlendFunc();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        double y = mc.gameRenderer.getMainCamera().getPosition().y + SkyDataHandler.getHeightOffsetForLevel(mc.level.dimension());
        if (y > 1000.0) {
            float factor = (float) Mth.clamp((y - 1000.0) / 1000.0, 0.0, 1.0);
            
            float start = event.getNearPlaneDistance();
            float end = event.getFarPlaneDistance();
            
            event.setNearPlaneDistance(Mth.lerp(factor, start, 1000.0f));
            event.setFarPlaneDistance(Mth.lerp(factor, end, 2000.0f));
            event.setCanceled(true);
        }
    }

    private static PlanetRenderInfo PLANET_TEXTURE_OBJ_LAST = null;
    private static PlanetRenderInfo PLANET_TEXTURE_OBJ = null;
    private static float texFade = 0;
    private static boolean awaitUpdate = false;

    
    private static float computePrettyness(PlanetRenderInfo planet, double camY) {
        double continuousSize = SkyDataHandler.targetSizeForHeightContinuous(camY);
        if (continuousSize <= getMaximumScale() || continuousSize <= planet.getPowerSize()) {
            return 0;
        } else if (continuousSize >= SkyDataHandler.MAX_POWER_SIZE) {
            
            
            return 1;
        } else {
            return (float) (1 - 1 / (1 + continuousSize - planet.getPowerSize()));
        }
    }

    private static int getMaximumScale() {
        
        
        return 100;
    }

    private static void updatePlanetTex(double camX, double camY, double camZ) {
        if (awaitUpdate) return;
        
        boolean clamped = SkyDataHandler.targetSizeForHeightContinuous(camY) > getMaximumScale();
        double currentSize = clamped ? camY * (2 << SkyDataHandler.SCALE_FACTOR) : SkyDataHandler.toTrueSize(PLANET_TEXTURE_OBJ.getPowerSize());
        boolean violateX = Math.abs(camX - PLANET_TEXTURE_OBJ.getCenterX()) > currentSize * 3/5;
        boolean violateZ = Math.abs(camZ - PLANET_TEXTURE_OBJ.getCenterZ()) > currentSize * 3/5;
        boolean violateScale = PLANET_TEXTURE_OBJ.getPowerSize() != Math.min(SkyDataHandler.targetSizeForHeight(camY), getMaximumScale());
        if (violateX || violateZ || violateScale) {
            awaitUpdate = true;
            PacketDistributor.sendToServer(new PlanetMapRequestPayload(SkyDataHandler.targetSizeForHeight(camY)));
        }
    }

    private static void ensurePlanetTexObj() {
        if (PLANET_TEXTURE_OBJ == null) {
            Minecraft mc = Minecraft.getInstance();

            int size = 1024;
            NativeImage image = new NativeImage(size, size, false);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    int color = (255 << 24) | (80 << 16) | (40 << 8) | 10;
                    image.setPixelRGBA(x, y, color);
                }
            }

            DynamicTexture tex = new DynamicTexture(image);
            ResourceLocation id = mc.getTextureManager().register("rocketnautics_planet_main", tex);
            tex.setFilter(true, false);
            PLANET_TEXTURE_OBJ = new PlanetRenderInfo(id, tex);
            image.close();
        }
        if (PLANET_TEXTURE_OBJ_LAST == null) {
            Minecraft mc = Minecraft.getInstance();

            int size = 1024;
            NativeImage image = new NativeImage(size, size, false);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    int color = (255 << 24) | (80 << 16) | (40 << 8) | 10;
                    image.setPixelRGBA(x, y, color);
                }
            }

            DynamicTexture tex = new DynamicTexture(image);
            ResourceLocation id = mc.getTextureManager().register("rocketnautics_planet_last", tex);
            tex.setFilter(true, false);
            PLANET_TEXTURE_OBJ_LAST = new PlanetRenderInfo(id, tex);
            image.close();
        }
    }

    public static void updatePlanetTexture(int powerSize, int centerX, int centerZ, byte[] mapDataPosXPosZ, byte[] mapDataPosXNegZ, byte[] mapDataNegXPosZ, byte[] mapDataNegXNegZ) {
        PlanetRenderInfo updating = PLANET_TEXTURE_OBJ_LAST;
        PLANET_TEXTURE_OBJ_LAST = PLANET_TEXTURE_OBJ;
        PLANET_TEXTURE_OBJ = updating;
        texFade = 1;

        PLANET_TEXTURE_OBJ.setPowerSize(powerSize);
        PLANET_TEXTURE_OBJ.setCenterX(centerX);
        PLANET_TEXTURE_OBJ.setCenterZ(centerZ);
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (PLANET_TEXTURE_OBJ == null) return;
            
            int texSize = 1024;
            int virtualSize = 256; // virtual size for a gorgeous clean retro pixel art style
            int blockSize = texSize / virtualSize; // 4x4 blocks in the 1024x1024 texture
            
            NativeImage image = new NativeImage(texSize, texSize, false);
            byte[] virtualBiomes = new byte[virtualSize * virtualSize];
            
            // Pass 1: Compute smoothed metaball-like biomes on the virtual grid
            for (int vy = 0; vy < virtualSize; vy++) {
                for (int vx = 0; vx < virtualSize; vx++) {
                    double u = (vx / (double)virtualSize) * 512.0;
                    double v = (vy / (double)virtualSize) * 512.0;
                    
                    // Smooth wave distortion for organic borders
                    double nx = u * 0.1;
                    double ny = v * 0.1;
                    double warpX = (Math.sin(nx) + 0.5 * Math.sin(nx * 2.1)) * 1.5;
                    double warpY = (Math.cos(ny) + 0.5 * Math.cos(ny * 2.1)) * 1.5;
                    double wu = u + warpX;
                    double wv = v + warpY;
                    
                    int cx = (int) Math.round(wu);
                    int cy = (int) Math.round(wv);
                    
                    float[] influence = new float[11];
                    double radius = 3.2;
                    double radiusSq = radius * radius;
                    
                    for (int dx = -3; dx <= 3; dx++) {
                        for (int dy = -3; dy <= 3; dy++) {
                            int gx = cx + dx;
                            int gy = cy + dy;
                            byte biome = getBiomeAt(gx, gy, mapDataPosXPosZ, mapDataPosXNegZ, mapDataNegXPosZ, mapDataNegXNegZ);
                            
                            double distX = wu - (gx + 0.5);
                            double distY = wv - (gy + 0.5);
                            double dSq = distX * distX + distY * distY;
                            
                            if (dSq < radiusSq) {
                                // Smooth drop-off weight function: (1 - d^2 / R^2)^3
                                double ratio = dSq / radiusSq;
                                double term = 1.0 - ratio;
                                double weight = term * term * term;
                                
                                // Boost landmasses slightly for tighter shores
                                if (biome != 0 && biome != 1) {
                                    influence[biome] += (float) (weight * 1.1);
                                } else {
                                    influence[biome] += (float) weight;
                                }
                            }
                        }
                    }
                    
                    int bestBiome = 4;
                    float maxInfluence = -1;
                    for (int b = 0; b < 11; b++) {
                        if (influence[b] > maxInfluence) {
                            maxInfluence = influence[b];
                            bestBiome = b;
                        }
                    }
                    virtualBiomes[vx + vy * virtualSize] = (byte) bestBiome;
                }
            }
            
            // Pass 2: Color, Shade, and Draw the 3D-embossed pixel art planet
            for (int vy = 0; vy < virtualSize; vy++) {
                for (int vx = 0; vx < virtualSize; vx++) {
                    byte colorIdx = virtualBiomes[vx + vy * virtualSize];
                    
                    int r = 30, g = 120, b = 40;
                    switch (colorIdx) {
                        case 0: r = 15; g = 45; b = 135; break;  // Ocean (Curated deep royal blue)
                        case 1: r = 25; g = 95; b = 215; break;  // River (Vibrant blue)
                        case 2: r = 225; g = 205; b = 155; break; // Beach (Warm sand)
                        case 3: r = 215; g = 195; b = 115; break; // Desert (Golden sand)
                        case 4: r = 45; g = 145; b = 55; break;   // Plain (Emerald green)
                        case 5: r = 25; g = 105; b = 35; break;   // Forest (Lush dark forest green)
                        case 6: r = 15; g = 85; b = 25; break;    // Jungle (Deep jungle teal-green)
                        case 7: r = 30; g = 75; b = 55; break;    // Taiga (Cool pine green)
                        case 8: r = 240; g = 240; b = 245; break; // Snowy (Pristine snow white)
                        case 9: r = 195; g = 90; b = 40; break;   // Badlands (Terracotta orange)
                        case 10: r = 135; g = 135; b = 135; break;// Mountain (Slate grey)
                    }
                    
                    // Dynamic 3D pixel-art coastline shading
                    boolean isWater = (colorIdx == 0 || colorIdx == 1);
                    if (isWater) {
                        // Shadow cast ONTO water from top-left land
                        int tlx = vx - 1;
                        int tly = vy - 1;
                        if (tlx >= 0 && tly >= 0) {
                            byte neighborBiome = virtualBiomes[tlx + tly * virtualSize];
                            boolean neighborIsLand = (neighborBiome != 0 && neighborBiome != 1);
                            if (neighborIsLand) {
                                r = (int) (r * 0.75);
                                g = (int) (g * 0.75);
                                b = (int) (b * 0.75);
                            }
                        }
                    } else {
                        // Border depth cast ONTO neighboring bottom-right water
                        int brx = vx + 1;
                        int bry = vy + 1;
                        if (brx < virtualSize && bry < virtualSize) {
                            byte neighborBiome = virtualBiomes[brx + bry * virtualSize];
                            boolean neighborIsWater = (neighborBiome == 0 || neighborBiome == 1);
                            if (neighborIsWater) {
                                r = (int) (r * 0.85);
                                g = (int) (g * 0.85);
                                b = (int) (b * 0.85);
                            }
                        }
                    }
                    
                    int color = (255 << 24) | (b << 16) | (g << 8) | r;
                    for (int bx = 0; bx < blockSize; bx++) {
                        for (int by = 0; by < blockSize; by++) {
                            image.setPixelRGBA(vx * blockSize + bx, vy * blockSize + by, color);
                        }
                    }
                }
            }
            
            PLANET_TEXTURE_OBJ.getTexture().setPixels(image);
            PLANET_TEXTURE_OBJ.getTexture().upload();
            PLANET_TEXTURE_OBJ.getTexture().setFilter(false, false);
            image.close();
            awaitUpdate = false;
        });
    }        

    private static ResourceLocation CLOUD_TEXTURE_ID = null;
    private static DynamicTexture CLOUD_TEXTURE_OBJ = null;

    private static void ensureCloudTexture() {
        if (CLOUD_TEXTURE_ID != null) return;
        Minecraft mc = Minecraft.getInstance();
        
        int size = 256;
        NativeImage image = new NativeImage(size, size, false);
        
        java.util.Random rand = new java.util.Random(1337L);
        Noise2D noiseGen = new Noise2D(64, rand);
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                float u = (x / (float)size) * 8.0f;
                float v = (y / (float)size) * 8.0f;
                
                // 3 octaves of seamless fBm noise
                float noise = 0;
                float amplitude = 0.5f;
                float frequency = 1.0f;
                for (int o = 0; o < 3; o++) {
                    noise += noiseGen.sample(u * frequency, v * frequency) * amplitude;
                    amplitude *= 0.5f;
                    frequency *= 2.0f;
                }
                
                float threshold = 0.42f;
                float density = 0.0f;
                if (noise > threshold) {
                    density = (noise - threshold) / (1.0f - threshold);
                }
                
                int alpha = 0;
                if (density > 0) {
                    alpha = (int) (density * 190.0f);
                }
                
                int color = (alpha << 24) | (255 << 16) | (255 << 8) | 255;
                image.setPixelRGBA(x, y, color);
            }
        }
        
        CLOUD_TEXTURE_OBJ = new DynamicTexture(image);
        CLOUD_TEXTURE_ID = mc.getTextureManager().register("rocketnautics_clouds", CLOUD_TEXTURE_OBJ);
        CLOUD_TEXTURE_OBJ.setFilter(true, false); // Bilinear filtering for wispy and smooth clouds
        image.close();
    }

    private static byte getBiomeAt(int gx, int gy, byte[] posPos, byte[] posNeg, byte[] negPos, byte[] negNeg) {
        gx = Math.max(0, Math.min(gx, 511));
        gy = Math.max(0, Math.min(gy, 511));
        int dataSize = 256;
        if (gx >= dataSize) {
            int sx = gx - dataSize;
            if (gy >= dataSize) {
                return posPos[sx + (gy - dataSize) * dataSize];
            } else {
                return posNeg[sx + gy * dataSize];
            }
        } else {
            if (gy >= dataSize) {
                return negPos[gx + (gy - dataSize) * dataSize];
            } else {
                return negNeg[gx + gy * dataSize];
            }
        }
    }

    private static class Noise2D {
        private final float[] grid;
        private final int size;

        public Noise2D(int size, java.util.Random rand) {
            this.size = size;
            this.grid = new float[size * size];
            for (int i = 0; i < grid.length; i++) {
                grid[i] = rand.nextFloat();
            }
        }

        public float get(int x, int y) {
            x = (x % size + size) % size;
            y = (y % size + size) % size;
            return grid[x + y * size];
        }

        public float sample(float x, float y) {
            int x0 = (int) Math.floor(x);
            int y0 = (int) Math.floor(y);
            float tx = x - x0;
            float ty = y - y0;

            float v00 = get(x0, y0);
            float v10 = get(x0 + 1, y0);
            float v01 = get(x0, y0 + 1);
            float v11 = get(x0 + 1, y0 + 1);

            float sx = tx * tx * (3 - 2 * tx);
            float sy = ty * ty * (3 - 2 * ty);

            float nx0 = v00 + sx * (v10 - v00);
            float nx1 = v01 + sx * (v11 - v01);

            return nx0 + sy * (nx1 - nx0);
        }
    }

    private static ResourceLocation HALO_TEXTURE_ID = null;
    private static boolean haloV5 = false;

    private static void ensureHaloTexture() {
        if (HALO_TEXTURE_ID != null && haloV5) return;
        Minecraft mc = Minecraft.getInstance();
        int size = 256;
        NativeImage image = new NativeImage(size, size, false);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                double dx = (x - size / 2.0) / (size / 2.0);
                double dy = (y - size / 2.0) / (size / 2.0);
                
                double dist = Math.max(Math.abs(dx), Math.abs(dy));
                
                int r = 0, g = 0, b = 0, a = 0;
                double planetRadius = 1.0 / 1.3;
                
                if (dist <= planetRadius) {
                    double normalizedDist = dist / planetRadius; 
                    double opticalDepth = Math.pow(normalizedDist, 3.0);
                    
                    r = 40; g = 120; b = 255;
                    r += (int) (opticalDepth * 215); 
                    g += (int) (opticalDepth * 135); 
                    b += (int) (opticalDepth * 0);   
                    
                    r = Math.min(255, r); g = Math.min(255, g); b = Math.min(255, b);
                    a = 80 + (int) (opticalDepth * 175);
                } else if (dist <= 1.0) {
                    double gradient = (dist - planetRadius) / (1.0 - planetRadius);
                    double fade = Math.pow(1.0 - gradient, 1.0);
                    
                    r = (int) (140 * fade);
                    g = (int) (220 * fade);
                    b = (int) (255 * fade);
                    
                    a = (int) (255 * fade);
                }
                
                int color = (a << 24) | (b << 16) | (g << 8) | r;
                image.setPixelRGBA(x, y, color);
            }
        }
        DynamicTexture dynamicTexture = new DynamicTexture(image);
        HALO_TEXTURE_ID = mc.getTextureManager().register("rocketnautics_halo_v5", dynamicTexture);
        haloV5 = true;
        image.close();
    }
}
