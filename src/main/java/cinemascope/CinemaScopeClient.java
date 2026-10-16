package cinemascope;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class CinemaScopeClient implements ClientModInitializer {

    private static KeyBinding toggleKey;

    /** Whether cinematic mode is switched on (the animation target). */
    private static boolean active;
    /** Linear animation progress 0..1, eased before use. */
    private static double linearProgress;
    private static long lastFrameNanos = -1L;
    private static boolean previousSmoothCamera;

    @Override
    public void onInitializeClient() {
        CinemaScopeConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cinema_scope.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.cinema_scope"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) {
                if (active) {
                    setActive(client, false);
                }
                linearProgress = 0.0D;
                return;
            }
            while (toggleKey.wasPressed()) {
                setActive(client, !active);
            }
        });
    }

    private static void setActive(MinecraftClient client, boolean value) {
        active = value;
        if (CinemaScopeConfig.get().smoothCamera) {
            if (value) {
                previousSmoothCamera = client.options.smoothCameraEnabled;
                client.options.smoothCameraEnabled = true;
            } else {
                client.options.smoothCameraEnabled = previousSmoothCamera;
            }
        }
    }

    /** Advances the animation; called once per rendered frame. */
    public static void onFrame() {
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
    public static double progress() {
        double t = linearProgress;
        return t < 0.5D ? 4.0D * t * t * t : 1.0D - Math.pow(-2.0D * t + 2.0D, 3.0D) / 2.0D;
    }

    /** True while the mode is on or still animating out. */
    public static boolean isEngaged() {
        return active || linearProgress > 0.0D;
    }
}
