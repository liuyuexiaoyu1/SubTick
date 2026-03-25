package subtick.mixins.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
//#if MC >= 26.1
//$$ import net.minecraft.client.renderer.state.level.CameraRenderState;
//#else
import net.minecraft.client.renderer.state.CameraRenderState;
//#endif
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import subtick.client.Configs;
import subtick.client.LevelRenderer;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRendererMixin {//1.21.10+
    @WrapMethod(method = "submit")
    public <S extends BlockEntityRenderState> void submit(S blockEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, Operation<Void> original) {
        BlockPos blockPos = blockEntityRenderState.blockPos;
        if (LevelRenderer.hlBe.containsKey(blockPos) && Configs.EXPERIMENTAL_RENDERING.getBooleanValue()){
            original.call(blockEntityRenderState, poseStack, new LevelRenderer.OutlineCollectorWrapper(submitNodeCollector, LevelRenderer.hlBe.get(blockPos)), cameraRenderState);
        } else {
            original.call(blockEntityRenderState, poseStack, submitNodeCollector, cameraRenderState);
        }
    }
}
