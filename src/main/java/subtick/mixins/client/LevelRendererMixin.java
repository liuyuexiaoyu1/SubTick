package subtick.mixins.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import subtick.client.ClientTickHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//#if MC >= 12002
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//$$ import org.spongepowered.asm.mixin.Unique;
//$$ import org.spongepowered.asm.mixin.injection.ModifyArgs;
//$$ import net.minecraft.world.entity.player.Player;
//#if MC < 12111
//$$ import org.apache.http.util.Args;
//#endif
//$$ import net.minecraft.world.entity.Entity;
//#endif
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
//#if MC >= 11900
//$$ import org.joml.Matrix4f;
//#else
import com.mojang.math.Matrix4f;
//#endif
import subtick.client.Configs;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
//#if MC >= 12103
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$ import java.util.List;
//$$ import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
//$$ import net.minecraft.client.renderer.culling.Frustum;
//$$ import net.minecraft.util.profiling.ProfilerFiller;
//$$ import com.mojang.blaze3d.resource.ResourceHandle;
//$$ import org.spongepowered.asm.mixin.injection.ModifyArg;
//$$ import net.minecraft.world.phys.Vec3;
//#endif
//#if MC >= 12101
//$$ import net.minecraft.client.DeltaTracker;
//#endif
//#if MC < 12002
import carpet.fakes.MinecraftClientInferface;
//#endif
//#if MC >= 12003
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//#endif
//#if MC >= 12109
//#if MC >= 26.1
//$$ import org.joml.Matrix4fc;
//$$ import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
//$$ import net.minecraft.client.renderer.state.level.LevelRenderState;
//$$ import net.minecraft.client.renderer.state.level.CameraRenderState;
//#else
//$$ import net.minecraft.client.renderer.state.LevelRenderState;
//$$ import net.minecraft.client.renderer.state.CameraRenderState;
//#endif
//$$ import net.minecraft.client.renderer.entity.state.EntityRenderState;
//$$ import org.joml.Vector4f;
//#endif
//#if MC >= 12108
//$$ import com.mojang.blaze3d.buffers.GpuBufferSlice;
//#endif

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
  @Shadow @Final private RenderBuffers renderBuffers;
  //#if MC < 12103
  @Shadow @Nullable private PostChain entityEffect;
  //#endif
  @Shadow @Final private Minecraft minecraft;
  @Unique private final ThreadLocal<Boolean> precessed = ThreadLocal.withInitial(() -> false);

  //#if MC >= 12109
  //$$ @Shadow @Final private SubmitNodeStorage submitNodeStorage;
  //$$ @Shadow @Final private LevelRenderState levelRenderState;
  //#endif
  @Inject(
          //#if MC >= 26.1
          //$$ method = "lambda$addMainPass$0",
          //#elseif MC >= 12103
          //$$ method = "method_62214",
          //#else
          method = "renderLevel",
          //#endif
          //#if MC >= 26.1
          //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;submitBlockEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeStorage;)V")
          //#elseif MC >= 12110
          //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures()V")
          //#elseif MC >= 12108
          //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/Camera;Lnet/minecraft/client/DeltaTracker;Ljava/util/List;)V")
          //#else
          at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0)
          //#endif
  )
  private void onRenderWorldLastNormal(
          //#if MC >= 26.1
          //$$ GpuBufferSlice terrainFog, LevelRenderState levelRenderState, ProfilerFiller profiler, ChunkSectionsToRender chunkSectionsToRender, ResourceHandle<?> entityOutlineTarget, ResourceHandle<?> translucentTarget, ResourceHandle<?> mainTarget, ResourceHandle<?> itemEntityTarget, ResourceHandle<?> particleTarget, boolean renderOutline, Matrix4fc modelViewMatrix, CallbackInfo ci, @Local(name = "poseStack") PoseStack poseStack
          //#elseif MC >= 12111
          //$$ GpuBufferSlice gpuBufferSlice, LevelRenderState levelRenderState, ProfilerFiller profilerFiller, Matrix4f matrix4f, ResourceHandle<?> resourceHandle, ResourceHandle<?> resourceHandle2, boolean bl, ResourceHandle<?> resourceHandle3, ResourceHandle<?> resourceHandle4, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12109
          //$$ GpuBufferSlice gpuBufferSlice, LevelRenderState levelRenderState, ProfilerFiller profilerFiller, Matrix4f matrix4f, ResourceHandle<?> resourceHandle, ResourceHandle<?> resourceHandle2, boolean bl, Frustum frustum, ResourceHandle<?> resourceHandle3, ResourceHandle<?> resourceHandle4, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12108
          //$$ GpuBufferSlice gpuBufferSlice, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profilerFiller, Matrix4f matrix4f, ResourceHandle<?> resourceHandle, ResourceHandle<?> resourceHandle2, boolean bl, Frustum frustum, ResourceHandle<?> resourceHandle3, ResourceHandle<?> resourceHandle4, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12105
          //$$ FogParameters fogParameters, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profilerFiller, Matrix4f matrix4f, Matrix4f matrix4f2, ResourceHandle<?> resourceHandle, ResourceHandle<?> resourceHandle2, boolean bl, Frustum frustum, ResourceHandle<?> resourceHandle3, ResourceHandle<?> resourceHandle4, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12103
          //$$ FogParameters fogParameters, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profilerFiller, Matrix4f matrix4f, Matrix4f matrix4f2, ResourceHandle<?> resourceHandle, ResourceHandle<?> resourceHandle2, ResourceHandle<?> resourceHandle3, ResourceHandle<?> resourceHandle4, boolean bl, Frustum frustum, ResourceHandle<?> resourceHandle5, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12101
          //$$ DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12006
          //$$ float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci, @Local PoseStack poseStack
          //#else
          PoseStack poseStack, float delta, long time, boolean renderBlockOutline, Camera camera, GameRenderer renderer, LightTexture lightTexture, Matrix4f projMatrix, CallbackInfo ci
          //#endif
  )
  {
    OutlineBufferSource outlineBufferSource = this.renderBuffers.outlineBufferSource();
    subtick.client.LevelRenderer.render(poseStack, outlineBufferSource, false);
  }

  //#if MC >= 26.1
  //$$ @Inject(method = "lambda$addLateDebugPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
  //#elseif MC >= 12111
  //$$ @Inject(method = "method_75413", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
  //#elseif MC >= 12109
  //$$ @Inject(method = "method_72915", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
  //#elseif MC >= 12103
  //$$ @Inject(method = "method_62212", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
  //#else
  @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V", ordinal = 1))
  //#endif
  private void onRenderWorldLastSnow(
          //#if MC >= 26.1
          //$$ GpuBufferSlice fog, ResourceHandle<?> mainTarget, CameraRenderState camera, Matrix4fc modelViewMatrix, CallbackInfo ci, @Local(name = "poseStack") PoseStack poseStack
          //#elseif MC >= 12111
          //$$ GpuBufferSlice gpuBufferSlice, ResourceHandle<?> resourceHandle, CameraRenderState cameraRenderState, Matrix4f matrix4f, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12109
          //$$ GpuBufferSlice gpuBufferSlice, Vec3 vec3, ResourceHandle<?> resourceHandle, Frustum frustum, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12108
          //$$ GpuBufferSlice gpuBufferSlice, Vec3 vec3, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12105
          //$$ FogParameters fogParameters, Vec3 vec3, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12103
          //$$ FogParameters fogParameters, ResourceHandle<?> resourceHandle, Vec3 vec3, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12101
          //$$ DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12006
          //$$ float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci, @Local PoseStack poseStack
          //#else
          PoseStack poseStack, float delta, long time, boolean renderBlockOutline, Camera camera, GameRenderer renderer, LightTexture lightTexture, Matrix4f projMatrix, CallbackInfo ci
          //#endif
  ) {
    subtick.client.LevelRenderer.render(poseStack, null, true);
  }

  @Inject(method = "shouldShowEntityOutlines()Z", at = @At("HEAD"), cancellable = true)
  private void forceEntityOutline(CallbackInfoReturnable<Boolean> cir) {
    if (subtick.client.LevelRenderer.hasOutline() && Configs.EXPERIMENTAL_RENDERING.getBooleanValue()) cir.setReturnValue(true);
  }
  
  //#if MC >= 12109
  //#if MC >= 26.1
  //$$ @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/culling/Frustum;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;ZLnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V"))
  //$$ private void renderLevel(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
  //#else
  //$$ @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/culling/Frustum;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;ZLnet/minecraft/client/renderer/state/LevelRenderState;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/util/profiling/ProfilerFiller;)V"))
  //$$ private void renderLevel(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean bl, Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, Matrix4f matrix4f3, GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl2, CallbackInfo ci) {
  //#endif
  //$$   if (!this.levelRenderState.haveGlowingEntities) this.levelRenderState.haveGlowingEntities = subtick.client.LevelRenderer.hasOutline();
  //$$ }
  //#endif

  //#if MC < 12109
  //#if MC >= 12103
  //$$ @Inject(method = "collectVisibleEntities", at = @At("RETURN"), cancellable = true)
  //$$ private void collectVisibleEntities(Camera camera, Frustum frustum, List<Entity> list, CallbackInfoReturnable<Boolean> cir) {
  //$$   if (subtick.client.LevelRenderer.hasOutline()) cir.setReturnValue(true);
  //$$ }
  //#endif

  //#if MC >= 12108
  //$$ @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;ZZLnet/minecraft/client/DeltaTracker;Lnet/minecraft/util/profiling/ProfilerFiller;)V"), index = 6)
  //#elseif MC >= 12103
  //$$ @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/FogParameters;ZZLnet/minecraft/client/DeltaTracker;Lnet/minecraft/util/profiling/ProfilerFiller;)V"), index = 6)
  //#else
  @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;process(F)V"))
  private void precess(PostChain instance, float f, Operation<Void> original) {
    original.call(instance, f);
    precessed.set(true);
  }
  @Inject(
          method = "renderLevel",
          at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;long2ObjectEntrySet()Lit/unimi/dsi/fastutil/objects/ObjectSet;")
  )
  //#endif
  //#if MC >= 12103
  //$$ private boolean forceEntityOutline2(boolean bl4) {
  //$$   return subtick.client.LevelRenderer.hasOutline() || bl4;
  //$$ }
  //#else
  private void forceEntityOutline2(
          //#if MC >= 12101
          //$$ DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci, @Local PoseStack poseStack
          //#elseif MC >= 12006
          //$$ float delta, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci, @Local PoseStack poseStack
          //#else
          PoseStack poseStack, float delta, long time, boolean renderBlockOutline, Camera camera, GameRenderer renderer, LightTexture lightTexture, Matrix4f projMatrix, CallbackInfo ci
          //#endif
  ) {
       if (subtick.client.LevelRenderer.hasOutline() || !precessed.get()) {
         //#if MC >= 12101
         //$$ this.entityEffect.process(deltaTracker.getGameTimeDeltaTicks());
         //#else
         this.entityEffect.process(delta);
         //#endif
         this.minecraft.getMainRenderTarget().bindWrite(false);
         precessed.set(false);
       }
  }
  //#endif
  //#endif


  // Everything below this point is yoinked from carpet

  //#if MC < 12103
  @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
  private void modifyBlockEntityDelta(BlockEntityRenderDispatcher instance, BlockEntity blockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, Operation<Void> original) {
    if (ClientTickHandler.frozen) f = 1.0f;
    original.call(instance, blockEntity, f, poseStack, multiBufferSource);
  }
  //#elseif MC < 12109
  //$$ @WrapOperation(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderBlockEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/Camera;F)V"))
  //$$ private void modifyBlockEntityDelta(LevelRenderer instance, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, MultiBufferSource.BufferSource bufferSource2, Camera camera, float v, Operation<Void> original) {
  //$$   if (ClientTickHandler.frozen) v = 1.0f;
  //$$   original.call(instance, poseStack, bufferSource, bufferSource2, camera, v);
  //$$ }
  //#endif

  //#if MC < 12002
  @Unique float initial = -1234.0f;

  @ModifyVariable(method = "renderLevel", argsOnly = true, require = 0, ordinal = 0, at = @At(
          value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"
  ))
  private float changeTickPhase(float previous)
  {
    initial = previous;
    if(ClientTickHandler.frozen)
      return ((MinecraftClientInferface)minecraft).getPausedTickDelta();
    return previous;
  }

  @ModifyVariable(method = "renderLevel", argsOnly = true, require = 0, ordinal = 0 ,at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
          shift = At.Shift.BEFORE
  ))
  private float changeTickPhaseBack(float previous)
  {
    return initial == -1234.0f ? previous : initial;
  }
  //#elseif MC < 12103
  //$$ @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE",
  //$$    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
  //$$ ))
  //$$ private void modifyDelta(LevelRenderer instance, Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, Operation<Void> original) {
  //$$     tickDelta = shouldUsePausedDelta(entity) ? 1.0F : tickDelta;
  //$$     original.call(instance, entity, cameraX, cameraY, cameraZ, tickDelta, matrices, vertexConsumers);
  //$$}
  //#elseif MC < 12109
  //$$ @WrapOperation(method = "renderEntities", at = @At(value = "INVOKE",
  //$$    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
  //$$ ))
  //$$ private void modifyDelta(LevelRenderer instance, Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, Operation<Void> original) {
  //$$     tickDelta = shouldUsePausedDelta(entity) ? 1.0F : tickDelta;
  //$$     original.call(instance, entity, cameraX, cameraY, cameraZ, tickDelta, matrices, vertexConsumers);
  //$$ }
  //$$ @WrapOperation(method = "renderBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
  //$$ private void modifyBlockEntityDelta(BlockEntityRenderDispatcher instance, BlockEntity blockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, Operation<Void> original) {
  //$$   if (ClientTickHandler.frozen) f = 1.0f;
  //$$   original.call(instance, blockEntity, f, poseStack, multiBufferSource);
  //$$ }
  //#elseif MC <= 12111
  //$$ @WrapOperation(method = "extractVisibleEntities", at = @At(value = "INVOKE",
  //$$    target = "Lnet/minecraft/client/renderer/LevelRenderer;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
  //$$ ))
  //$$ private EntityRenderState modifyDelta(LevelRenderer instance, Entity entity, float v, Operation<EntityRenderState> original) {
  //$$   if (shouldUsePausedDelta(entity)) v = 1.0f;
  //$$   return original.call(instance, entity, v);
  //$$ }
  //$$ @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;extractVisibleBlockEntities(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/state/LevelRenderState;)V"))
  //$$ private void modifyBlockEntityDelta(LevelRenderer instance, Camera camera, float v, LevelRenderState levelRenderState, Operation<Void> original) {
  //$$   if (ClientTickHandler.frozen) v = 1.0f;
  //$$   original.call(instance, camera, v, levelRenderState);
  //$$ }
  //#endif
  //#if MC >= 12002
  //$$ @Unique
  //$$ private boolean shouldUsePausedDelta(Entity entity) {
////$$   if (Minecraft.getInstance().getSingleplayerServer() != null) if (isReplayEnvironment(Minecraft.getInstance().getSingleplayerServer().getClass())) return false;
  //$$   return ClientTickHandler.frozen && !canTick(entity);
  //$$ }
  //$$ @Unique
  //$$ private boolean canTick(Entity entity) {
  //$$  if (entity instanceof Player) return true;
  //$$  return entity.getPassengers().stream().flatMap(Entity::getSelfAndPassengers).anyMatch(entity1 -> entity1 instanceof Player);
  //$$ }
  //#endif
}
