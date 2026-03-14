package subtick.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.client.Configs;
import subtick.client.LevelRenderer;

import java.util.List;

@Mixin(value = BlockFeatureRenderer.class, priority = 2000)
public class BlockFeatureRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void render(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource, BlockRenderDispatcher blockRenderDispatcher, OutlineBufferSource outlineBufferSource, CallbackInfo ci
    ) {
        for(SubmitNodeStorage.MovingBlockSubmit movingBlockSubmit : submitNodeCollection.getMovingBlockSubmits()) {
            BlockPos pos = movingBlockSubmit.movingBlockRenderState().blockPos;
            if (LevelRenderer.hlBe.containsKey(pos.immutable()) && Configs.EXPERIMENTAL_RENDERING.getBooleanValue()) {
                outlineBufferSource.setColor(LevelRenderer.hlBe.get(pos));
                MovingBlockRenderState movingBlockRenderState = movingBlockSubmit.movingBlockRenderState();
                BlockState blockState = movingBlockRenderState.blockState;
                List<BlockModelPart> list = blockRenderDispatcher.getBlockModel(blockState).collectParts(RandomSource.create(blockState.getSeed(movingBlockRenderState.randomSeedPos)));
                PoseStack poseStack = new PoseStack();
                poseStack.mulPose(movingBlockSubmit.pose());
                blockRenderDispatcher.getModelRenderer().tesselateBlock(movingBlockRenderState, list, blockState, movingBlockRenderState.blockPos, poseStack, new LevelRenderer.InvisibleOutlineBufferSource().getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(blockState)), false, OverlayTexture.NO_OVERLAY);
                outlineBufferSource.setColor(-1);
            }
        }
    }
}
