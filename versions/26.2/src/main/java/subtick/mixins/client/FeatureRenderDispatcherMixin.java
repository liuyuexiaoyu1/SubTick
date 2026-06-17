package subtick.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects custom outlines and text into all FeatureRenderDispatcher rendering paths.
 */
@Mixin(value = FeatureRenderDispatcher.class, priority = 2000)
public class FeatureRenderDispatcherMixin
{
    @Inject(method = "renderAllFeatures", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;prepareFrame(Lnet/minecraft/client/renderer/SubmitNodeStorage;)Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;", shift = At.Shift.BEFORE))
    private void onBeforePrepareFrame(SubmitNodeStorage submitNodeStorage, CallbackInfo ci)
    {
        if (subtick.client.LevelRenderer.hasOutline())
        {
            subtick.client.LevelRenderer.render(new PoseStack(), submitNodeStorage, false);
            subtick.client.LevelRenderer.render(new PoseStack(), submitNodeStorage, true);
        }
    }
}
