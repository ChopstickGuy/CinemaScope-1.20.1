package cinemascope;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

    /** Advances the animation once per rendered frame. */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
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

    /** Draws the sliding letterbox bars before the HUD, so open screens stay on top. */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (!isEngaged() || Minecraft.getInstance().level == null) {
            return;
        }
        CinemaScopeConfig config = CinemaScopeConfig.get();
        GuiGraphics graphics = event.getGuiGraphics();

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
