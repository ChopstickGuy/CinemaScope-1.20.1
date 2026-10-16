package cinemascope.mixin;

import cinemascope.CinemaScopeClient;
import cinemascope.CinemaScopeConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the sliding letterbox bars and suppresses the HUD while cinematic mode is engaged. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cinemascope$render(DrawContext context, float tickDelta, CallbackInfo ci) {
        if (!CinemaScopeClient.isEngaged() || MinecraftClient.getInstance().world == null) {
            return;
        }
        CinemaScopeConfig config = CinemaScopeConfig.get();

        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        double visibleH = w / config.targetAspectRatio;
        float barH = (float) Math.max(h * config.minBarFraction, (h - visibleH) / 2.0D);

        if (barH > 0.0F) {
            // Bars start fully off-screen and slide in; the float translation keeps the motion smooth
            // even at large GUI scales.
            float offset = (float) (barH * CinemaScopeClient.progress());
            int size = (int) Math.ceil(barH) + 1;

            context.getMatrices().push();
            context.getMatrices().translate(0.0F, offset, 0.0F);
            context.fill(0, -size, w, 0, 0xFF000000);
            context.getMatrices().pop();

            context.getMatrices().push();
            context.getMatrices().translate(0.0F, -offset, 0.0F);
            context.fill(0, h, w, h + size, 0xFF000000);
            context.getMatrices().pop();
        }

        if (config.hideHud) {
            ci.cancel();
        }
    }
}
