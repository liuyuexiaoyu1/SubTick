package subtick.mixins.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.client.Configs;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
  @Shadow @Final private SubmitNodeStorage submitNodeStorage;
  @Shadow @Final private LevelRenderState levelRenderState;

  @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;prepareFrame(Lnet/minecraft/client/renderer/SubmitNodeStorage;)Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;", shift = At.Shift.BEFORE))
  private void onBeforePrepareFrame(GraphicsResourceAllocator alloc, DeltaTracker delta, boolean renderOutline, CameraRenderState camera, Matrix4fc view, GpuBufferSlice fog, Vector4f fogColor, boolean sky, CallbackInfo ci)
  {
    if (subtick.client.LevelRenderer.hasOutline())
    {
      this.levelRenderState.shouldShowEntityOutlines = true;
    }
    subtick.client.LevelRenderer.render(new PoseStack(), this.submitNodeStorage, false);
    subtick.client.LevelRenderer.render(new PoseStack(), this.submitNodeStorage, true);
  }
}
