package cinemascope.mixin;

import cinemascope.CinemaScopeClient;
import cinemascope.CinemaScopeConfig;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void cinemascope$tickAnimation(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        CinemaScopeClient.onFrame();
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void cinemascope$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (!changingFov || !CinemaScopeClient.isEngaged()) {
            return;
        }
        double zoom = CinemaScopeConfig.get().zoomAmount * CinemaScopeClient.progress();
        cir.setReturnValue(cir.getReturnValue() * (1.0D - zoom));
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void cinemascope$hideHand(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        if (CinemaScopeConfig.get().hideHand && CinemaScopeClient.isEngaged()) {
            ci.cancel();
        }
    }
}
