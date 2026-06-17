package subtick.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.MovingBlockFeatureRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.client.Configs;
import subtick.client.LevelRenderer;

import java.util.List;

/**
 * In 26.2, BlockFeatureRenderer was removed and replaced by MovingBlockFeatureRenderer
 * which extends RenderTypeFeatureRenderer<Submit>.
 * MovingBlockFeatureRenderer already supports outlineColor via Submit.outlineColor().
 * This mixin intercepts buildGroup to inject outline colors from hlBe.
 */
@Mixin(value = MovingBlockFeatureRenderer.class, priority = 2000)
public class BlockFeatureRendererMixin
{
    @Inject(method = "buildGroup", at = @At("HEAD"))
    private void onBuildGroup(FeatureFrameContext context, List<MovingBlockFeatureRenderer.Submit> submits, CallbackInfo ci)
    {
        if (Configs.EXPERIMENTAL_RENDERING.getBooleanValue())
        {
            for (int i = 0; i < submits.size(); i++)
            {
                MovingBlockFeatureRenderer.Submit submit = submits.get(i);
                MovingBlockRenderState state = submit.movingBlockRenderState();
                int color = LevelRenderer.hlBe.getOrDefault(state.blockPos, submit.outlineColor());
                if (color != submit.outlineColor())
                {
                    submits.set(i, new MovingBlockFeatureRenderer.Submit(
                        submit.pose(), submit.movingBlockRenderState(), color
                    ));
                }
            }
        }
    }
}
