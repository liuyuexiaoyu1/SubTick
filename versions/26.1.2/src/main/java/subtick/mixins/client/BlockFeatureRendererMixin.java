package subtick.mixins.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.client.Configs;
import subtick.client.LevelRenderer;

import java.lang.ScopedValue;

@Mixin(value = BlockFeatureRenderer.class, priority = 2000)
public class BlockFeatureRendererMixin {
    @Unique private final ScopedValue<OutlineBufferSource> outlineBufferSource = ScopedValue.newInstance();
    @Unique private BlockQuadOutput output;
    @Unique private BlockQuadOutput solidOutput;

    @WrapMethod(method = "renderTranslucent")
    private void renderTranslucent(SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource, BlockStateModelSet blockStateModelSet, OutlineBufferSource outlineBufferSource, MultiBufferSource.BufferSource crumblingBufferSource, OptionsRenderState optionsState, Operation<Void> original) {
        ScopedValue.where(this.outlineBufferSource, outlineBufferSource).run(() -> original.call(nodeCollection, bufferSource, blockStateModelSet, outlineBufferSource, crumblingBufferSource, optionsState));
    }

    @WrapMethod(method = "renderSolid")
    private void renderSolid(SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource, BlockStateModelSet blockStateModelSet, OutlineBufferSource outlineBufferSource, OptionsRenderState optionsState, Operation<Void> original) {
        ScopedValue.where(this.outlineBufferSource, outlineBufferSource).run(() -> original.call(nodeCollection, bufferSource, blockStateModelSet, outlineBufferSource, optionsState));
    }

    @Inject(method = "renderMovingBlockSubmits", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getInstance()Lnet/minecraft/client/Minecraft;"))
    private void renderMovingBlockSubmits(SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource, BlockStateModelSet blockStateModelSet, OptionsRenderState optionsState, boolean translucent, CallbackInfo ci, @Local(name = "poseStack") PoseStack poseStack) {
        output = (x, y, z, quad, instance) -> putBakedQuad(poseStack, this.outlineBufferSource.get(), x, y, z, quad, instance, quad.materialInfo().layer());
        solidOutput = (x, y, z, quad, instance) -> putBakedQuad(poseStack, this.outlineBufferSource.get(), x, y, z, quad, instance, ChunkSectionLayer.SOLID);
    }

    @WrapOperation(method = "renderMovingBlockSubmits", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"))
    private void tesselateBlock(ModelBlockRenderer instance, BlockQuadOutput blockQuadOutput, float x, float y, float z, BlockAndTintGetter blockAndTintGetter, BlockPos pos, BlockState blockState, BlockStateModel blockStateModel, long l, Operation<Void> original, @Local(argsOnly = true) OptionsRenderState optionsRenderState) {
        if (LevelRenderer.hlBe.containsKey(pos.immutable()) && Configs.EXPERIMENTAL_RENDERING.getBooleanValue()) {
            boolean cutoutLeaves = optionsRenderState.cutoutLeaves;
            BlockQuadOutput blockOutput = ModelBlockRenderer.forceOpaque(cutoutLeaves, blockState) ? solidOutput : output;
            original.call(instance, blockOutput, x, y, z, blockAndTintGetter, pos, blockState, blockStateModel, l);
            return;
        }
        original.call(instance, blockQuadOutput, x, y, z, blockAndTintGetter, pos, blockState, blockStateModel, l);
    }

    @Unique
    private static void putBakedQuad(
            final PoseStack poseStack,
            final OutlineBufferSource outlineBufferSource,
            final float x,
            final float y,
            final float z,
            final BakedQuad quad,
            final QuadInstance instance,
            final ChunkSectionLayer layer
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        VertexConsumer buffer = outlineBufferSource.getBuffer(switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        });
        buffer.putBakedQuad(poseStack.last(), quad, instance);
        poseStack.popPose();
    }
}
