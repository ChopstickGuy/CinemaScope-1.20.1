package cinemascope;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public final class CinemaScopeClient {

    private static final KeyMapping TOGGLE_KEY = new KeyMapping(
        "key.cinema_scope.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.cinema_scope");

    /** Whether cinematic mode is switched on (the animation target). */
    private static boolean active;
    /** Linear animation progress 0..1, eased before use. */
    private static double linearProgress;
    private static long lastFrameNanos = -1L;
    private static boolean previousSmoothCamera;
    /** Set while we have switched on vanilla's F1 "hide GUI" to hide the HUD. */
    private static boolean hidingGui;
    private static boolean previousHideGui;

    private CinemaScopeClient() {
    }

    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (active) {
                setActive(mc, false);
            }
            linearProgress = 0.0D;
            updateHideGui(mc);
            return;
        }
        while (TOGGLE_KEY.consumeClick()) {
            setActive(mc, !active);
        }
    }

    private static void setActive(Minecraft mc, boolean value) {
        active = value;
        if (CinemaScopeConfig.get().smoothCamera) {
            if (value) {
                previousSmoothCamera = mc.options.smoothCamera;
                mc.options.smoothCamera = true;
            } else {
                mc.options.smoothCamera = previousSmoothCamera;
            }
        }
    }

    /**
     * Hides the HUD through vanilla's F1 switch rather than cancelling individual overlays, so HUD
     * elements that mods draw outside Forge's overlay system disappear too.
     */
    private static void updateHideGui(Minecraft mc) {
        boolean shouldHide = CinemaScopeConfig.get().hideHud && isEngaged();
        if (shouldHide && !hidingGui) {
            previousHideGui = mc.options.hideGui;
            mc.options.hideGui = true;
            hidingGui = true;
        } else if (!shouldHide && hidingGui) {
            mc.options.hideGui = previousHideGui;
            hidingGui = false;
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START) {
            advanceAnimation();
            updateHideGui(mc);
        } else if (mc.options.hideGui && mc.screen == null && mc.level != null && isEngaged()) {
            // With F1 active and no screen open the GUI pass is skipped entirely,
            // so draw the bars on top of the finished frame instead.
            drawBarsStandalone(mc);
        }
    }

    private static void advanceAnimation() {
        long now = System.nanoTime();
        double dt = lastFrameNanos < 0 ? 0.0D : (now - lastFrameNanos) / 1.0E9D;
        lastFrameNanos = now;
        // Cap dt so a lag spike doesn't make the bars jump.
        double step = Math.min(dt, 0.1D) / CinemaScopeConfig.get().animationSeconds;
        linearProgress = active
            ? Math.min(1.0D, linearProgress + step)
            : Math.max(0.0D, linearProgress - step);
    }

    /** Eased progress 0..1 (ease-in-out cubic). */
    private static double progress() {
        double t = linearProgress;
        return t < 0.5D ? 4.0D * t * t * t : 1.0D - Math.pow(-2.0D * t + 2.0D, 3.0D) / 2.0D;
    }

    /** True while the mode is on or still animating out. */
    private static boolean isEngaged() {
        return active || linearProgress > 0.0D;
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov() || !isEngaged()) {
            return;
        }
        double zoom = CinemaScopeConfig.get().zoomAmount * progress();
        event.setFOV(event.getFOV() * (1.0D - zoom));
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (CinemaScopeConfig.get().hideHand && isEngaged()) {
            event.setCanceled(true);
        }
    }

    /** Hides every HUD element (vanilla and modded overlays). */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (CinemaScopeConfig.get().hideHud && isEngaged()) {
            event.setCanceled(true);
        }
    }

    /** Draws the bars in the GUI pass (runs when a screen is open or the HUD isn't hidden), below the screen. */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (isEngaged() && Minecraft.getInstance().level != null) {
            drawBars(event.getGuiGraphics());
        }
    }

    /** Sets up the same GUI projection GameRenderer uses, then draws the bars. */
    private static void drawBarsStandalone(Minecraft mc) {
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        modelView.translate(0.0D, 0.0D, 1000.0D - ForgeHooksClient.getGuiFarPlane());
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(256, Minecraft.ON_OSX);

        GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        drawBars(graphics);
        graphics.flush();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private static void drawBars(GuiGraphics graphics) {
        CinemaScopeConfig config = CinemaScopeConfig.get();

        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        double visibleH = w / config.targetAspectRatio;
        float barH = (float) Math.max(h * config.minBarFraction, (h - visibleH) / 2.0D);
        if (barH <= 0.0F) {
            return;
        }

        // Bars start fully off-screen and slide in; the float translation keeps the motion smooth
        // even at large GUI scales.
        float offset = (float) (barH * progress());
        int size = (int) Math.ceil(barH) + 1;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, offset, 0.0F);
        graphics.fill(0, -size, w, 0, 0xFF000000);
        graphics.pose().popPose();

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, -offset, 0.0F);
        graphics.fill(0, h, w, h + size, 0xFF000000);
        graphics.pose().popPose();
    }
}
