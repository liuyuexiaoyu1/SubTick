package subtick.client;

import com.mojang.blaze3d.vertex.*;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
//#if MC >= 12111
//$$ import net.minecraft.client.renderer.rendertype.RenderType;
//$$ import net.minecraft.client.renderer.rendertype.RenderTypes;
//#else
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
//#endif

import java.util.*;
import java.util.stream.Collectors;

public class LevelRenderer
{
    private static final com.mojang.blaze3d.pipeline.RenderPipeline WORLD_QUAD_PIPELINE = com.mojang.blaze3d.pipeline.RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("subtick", "world_quads"))
            .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .build();
    public static final net.minecraft.client.renderer.RenderType WORLD_QUADS = net.minecraft.client.renderer.RenderType.create(
            "subtick_world_quads",
            //#if MC >= 12111
            //$$ net.minecraft.client.renderer.rendertype.RenderSetup.builder(WORLD_QUAD_PIPELINE)
            //$$                .affectsCrumbling()
            //$$                .sortOnUpload()
            //$$                .bufferSize(256)
            //$$                .createRenderSetup()
            //#else
            256, false, true, WORLD_QUAD_PIPELINE,
            net.minecraft.client.renderer.RenderType.CompositeState.builder()
                    .createCompositeState(false)
            //#endif
    );
    private static final Minecraft mc = Minecraft.getInstance();
    private static final HashSet<Pos> hlPos = new HashSet<>();
    private static final HashSet<Text> texts = new HashSet<>();
    public static final HashMap<BlockPos,Integer> hlBe = new HashMap<>();

    public static synchronized void render(PoseStack poseStack, OutlineBufferSource outlineBufferSource, boolean renderText) {
        Camera camera = mc.gameRenderer.getMainCamera();
        LevelRenderState levelRenderState = mc.gameRenderer.getLevelRenderState();
        SubmitNodeCollector output = mc.gameRenderer.getSubmitNodeStorage();
        Vec3 cpos = camera.position();
        if (!renderText) {
            LevelRenderer.hlBe.clear();
            if (Configs.EXPERIMENTAL_RENDERING.getBooleanValue()) {
                Map<Integer, List<Outline>> groupedOutlines = hlPos.stream()
                        .filter(p -> p instanceof Outline)
                        .map(o -> (Outline) o)
                        .collect(Collectors.groupingBy(o -> o.color().intValue));

                for (Map.Entry<Integer, List<Outline>> entry : groupedOutlines.entrySet()) {
                    int color = entry.getKey();
                    outlineBufferSource.setColor(color);
                    for (Outline o : entry.getValue()) {
                        o.render(null, poseStack, camera, levelRenderState, output, outlineBufferSource, mc.level, true);
                    }
                    outlineBufferSource.setColor(-1);
                }
            }
        } else {
            if (!hlPos.isEmpty() && !Configs.EXPERIMENTAL_RENDERING.getBooleanValue()) {
                BufferBuilder quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                for (Pos pos : hlPos) {
                    pos.render(quadBuffer, poseStack, camera, levelRenderState, output, outlineBufferSource, mc.level, false);
                }
                WORLD_QUADS.draw(quadBuffer.buildOrThrow());
            }
            if (!texts.isEmpty()) {
                for (Text text : texts) {
                    text.render(null, poseStack, camera.rotation(), cpos.x, cpos.y, cpos.z);
                }
            }
        }
    }

    public static synchronized void clear()
    {
        hlPos.clear();
        texts.clear();
    }


    public static synchronized boolean hasOutline(){
        return !hlPos.isEmpty();
    }

    public static synchronized void addOutline(BlockPos pos, Color4f color) {
        Outline o = new Outline(pos, color);
        if(!hlPos.add(o)) {
            hlPos.remove(o);
            hlPos.add(o);
        }
    }

    public static synchronized void addText(String text, int x, int y, int z, Color4f color)
    {
        TextBasic o = new TextBasic(text, x + 0.5, y + 0.5, z + 0.5, color);
        if(!texts.add(o))
        {
            texts.remove(o);
            texts.add(o);
        }
    }

    public static synchronized void addLabel(int index, int depth, int x, int y, int z, Color4f color1, Color4f color2)
    {
        DepthLabel o = new DepthLabel(String.valueOf(index), String.valueOf(depth), x + 0.5, y + 0.5, z + 0.5, color1.intValue, color2.intValue);
        if(!texts.add(o))
        {
            texts.remove(o);
            texts.add(o);
        }
    }

    private interface Pos
    {
        void render(BufferBuilder buffer, PoseStack poseStack, Camera camera, LevelRenderState levelRenderState, SubmitNodeCollector output, OutlineBufferSource outlineBufferSource, Level level, boolean NEW);
    }

    private record Outline(BlockPos pos, Color4f color) implements Pos
    {
        @Override
        public boolean equals(Object b)
        {
            return b instanceof Outline o && o.pos.getX() == pos.getX() && o.pos.getY() == pos.getY() && o.pos.getZ() == pos.getZ();
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(pos);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void render(BufferBuilder buffer, PoseStack poseStack, Camera camera, LevelRenderState levelRenderState, SubmitNodeCollector output, OutlineBufferSource outlineBufferSource, Level level, boolean NEW)
        {
            if (!NEW) {
                //#if MC >= 12111
                //$$ double cx = camera.position().x;
                //$$ double cy = camera.position().y;
                //$$ double cz = camera.position().z;
                //#else
                double cx = camera.getPosition().x;
                double cy = camera.getPosition().y;
                double cz = camera.getPosition().z;
                //#endif
                double x1 = pos.getX();
                double y1 = pos.getY();
                double z1 = pos.getZ();
                double x2 = x1 + 1;
                double y2 = y1 + 1;
                double z2 = z1 + 1;
                double x = x1 - cx, y = y1 - cy, z = z1 - cz;
                double X = x2 - cx, Y = y2 - cy, Z = z2 - cz;
                buffer.addVertex((float) x, (float) y, (float) z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) x, (float) Y, (float) z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) x, (float) Y, (float) Z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) x, (float) y, (float) Z).setColor(color.r, color.g, color.b, color.a);

                buffer.addVertex((float) X,(float) y,(float) z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) X,(float) y,(float) Z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) X,(float) Y,(float) Z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) X,(float) Y,(float) z).setColor(color.r, color.g, color.b, color.a);

                buffer.addVertex((float) x,(float)  y,(float)  z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) x,(float)  y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) X,(float)  y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) X,(float)  y,(float)  z).setColor(color.r, color.g, color.b, color.a);

                buffer.addVertex((float) x,(float)  Y,(float)  z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) X,(float)  Y,(float)  z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) X,(float)  Y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) x,(float)  Y,(float)  Z).setColor(color.r, color.g, color.b, color.a);

                buffer.addVertex((float) x,(float)  y,(float)  z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) X,(float)  y,(float)  z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) X,(float)  Y,(float)  z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) x,(float)  Y,(float)  z).setColor(color.r, color.g, color.b, color.a);

                buffer.addVertex((float) x,(float)  y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) x,(float)  Y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) X,(float)  Y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex((float) X,(float)  y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
                return;
            }
            //#if MC >= 12111
            //$$ RenderType outlineType = RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS);
            //#else
            RenderType outlineType = RenderType.outline(TextureAtlas.LOCATION_BLOCKS);
            //#endif
            BlockState state = level.getBlockState(pos);
            BlockRenderDispatcher blockRenderManager = mc.getBlockRenderer();
            BlockEntity blockEntity = level.getBlockEntity(pos);
            poseStack.pushPose();
            Vec3 cpos = camera.position();
            poseStack.translate(pos.getX() - cpos.x, pos.getY() - cpos.y, pos.getZ() - cpos.z);

            if (blockEntity != null) {//See BlockEntityRendererMixin
                if (blockEntity instanceof PistonMovingBlockEntity movingBlock && movingBlock.isExtending()) {
                    hlBe.put(pos.relative(movingBlock.getDirection().getOpposite()), color.intValue);
                } else {
                    hlBe.put(pos.immutable(), color.intValue);
                }
            }
            if (state.getRenderShape() != RenderShape.MODEL) {
                poseStack.popPose();
                return;
            }
            BlockStateModel model = blockRenderManager.getBlockModel(state);
            VertexConsumer vertexConsumer = outlineBufferSource.getBuffer(outlineType);
            InvisibleVertexConsumer invisibleConsumer = new InvisibleVertexConsumer(vertexConsumer);
            ModelBlockRenderer.renderModel(
                    poseStack.last(),
                    invisibleConsumer,
                    model,
                    color.r, color.g, color.b,
                    net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos),
                    OverlayTexture.NO_OVERLAY
            );

            poseStack.popPose();
        }
    }

    @SuppressWarnings("all")
    public static class InvisibleOutlineBufferSource implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            OutlineBufferSource outlineBufferSource = Minecraft.getInstance().renderBuffers().outlineBufferSource();
            VertexConsumer vertexConsumer = outlineBufferSource.getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS));
            return vertexConsumer;
        }
    }
    public static class OutlineCollectorWrapper implements SubmitNodeCollector {
        private final SubmitNodeCollector delegate;
        private final int outlineColor;

        public OutlineCollectorWrapper(SubmitNodeCollector delegate, int outlineColor) {
            this.delegate = delegate;
            this.outlineColor = outlineColor;
        }
        //#if MC < 12111
        @Override
        public void submitHitbox(PoseStack poseStack, EntityRenderState entityRenderState, HitboxesRenderState hitboxesRenderState) {
            this.delegate.submitHitbox(poseStack, entityRenderState, hitboxesRenderState);
        }
        //#endif

        @Override
        public void submitShadow(PoseStack poseStack, float f, List<EntityRenderState.ShadowPiece> list) {
            delegate.submitShadow(poseStack, f, list);
        }

        @Override
        public void submitNameTag(PoseStack poseStack, @Nullable Vec3 vec3, int i, Component component, boolean bl, int j, double d, CameraRenderState cameraRenderState) {
            delegate.submitNameTag(poseStack, vec3, i, component, bl, j, d, cameraRenderState);
        }

        @Override
        public void submitText(PoseStack poseStack, float f, float g, FormattedCharSequence formattedCharSequence, boolean bl, Font.DisplayMode displayMode, int i, int j, int k, int l) {
            delegate.submitText(poseStack, f, g, formattedCharSequence, bl, displayMode, i, j, k, this.outlineColor);
        }

        @Override
        public void submitFlame(PoseStack poseStack, EntityRenderState entityRenderState, Quaternionf quaternionf) {
            delegate.submitFlame(poseStack, entityRenderState, quaternionf);
        }

        @Override
        public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
            delegate.submitLeash(poseStack, leashState);
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                    int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite,
                                    int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
            delegate.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords,
                    tintedColor, sprite, this.outlineColor, crumblingOverlay);
        }

        @Override
        public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int i, int j, @Nullable TextureAtlasSprite textureAtlasSprite, boolean bl, boolean bl2, int k, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, int l) {
            delegate.submitModelPart(modelPart, poseStack, renderType, i, j, textureAtlasSprite, bl, bl2, k, crumblingOverlay, this.outlineColor);
        }

        @Override
        public void submitBlock(PoseStack poseStack, BlockState blockState, int i, int j, int k) {
            delegate.submitBlock(poseStack, blockState, i, j, this.outlineColor);
        }

        @Override
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
            delegate.submitMovingBlock(poseStack, movingBlockRenderState);
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, BlockStateModel blockStateModel, float f, float g, float h, int i, int j, int k) {
            delegate.submitBlockModel(poseStack, renderType, blockStateModel, f, g, h, i, j, this.outlineColor);
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext itemDisplayContext, int i, int j, int k, int[] is, List<BakedQuad> list, RenderType renderType, ItemStackRenderState.FoilType foilType) {
            delegate.submitItem(poseStack, itemDisplayContext, i, j, this.outlineColor, is, list, renderType, foilType);
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
            delegate.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
        }

        @Override
        public void submitParticleGroup(ParticleGroupRenderer particleGroupRenderer) {
            delegate.submitParticleGroup(particleGroupRenderer);
        }

        @Override
        public OrderedSubmitNodeCollector order(int i) {
            return delegate.order(i);
        }
    }

    private static class InvisibleVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;

        public InvisibleVertexConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
        }

        @Override
        public @NotNull VertexConsumer addVertex(float d, float e, float f) {
            return this.delegate.addVertex(d, e, f);
        }

        @Override
        public @NotNull VertexConsumer setColor(int i, int j, int k, int l) {
            return delegate.setColor(i, j, k, 0);
        }

        @Override
        public @NotNull VertexConsumer setColor(int color) {
            int colorWithZeroAlpha = color & 0x00FFFFFF;
            return this.delegate.setColor(colorWithZeroAlpha);
        }

        @Override
        public @NotNull VertexConsumer setUv(float u, float v) {
            return this.delegate.setUv(u, v);
        }

        @Override
        public @NotNull VertexConsumer setUv1(int u, int v) {
            return this.delegate.setUv1(u, v);
        }

        @Override
        public @NotNull VertexConsumer setUv2(int u, int v) {
            return this.delegate.setUv2(u, v);
        }

        @Override
        public @NotNull VertexConsumer setNormal(float x, float y, float z) {
            return this.delegate.setNormal(x, y, z);
        }

        //#if MC >= 12111
        //$$ @Override
        //$$ public @NotNull VertexConsumer setLineWidth(float f) {
        //$$     return delegate.setLineWidth(f);
        //$$ }
        //#endif
    }

    private interface Text
    {
        void render(BufferBuilder builder, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz);
    }

    private record TextBasic(String text, double x, double y, double z, Color4f color) implements Text
    {
        @Override
        public boolean equals(Object b)
        {
            return b instanceof TextBasic o && o.x == x && o.y == y && o.z == z;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(x, y, z);
        }

        @Override
        public void render(BufferBuilder buffer, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz)
        {
            Font font = Minecraft.getInstance().font;
            poseStack.pushPose();
            poseStack.translate((float)(x - cx), (float)(y - cy), (float)(z - cz));
            poseStack.mulPose(rotation);
            poseStack.scale(0.07F, -0.07F, 0.07F);
            MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
            font.drawInBatch(text, -font.width(text)/2F, -font.lineHeight * 0.5F, color.intValue, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
            immediate.endBatch();
            poseStack.popPose();
        }
    }

    private record DepthLabel(String index, String depth, double x, double y, double z, int color1, int color2) implements Text
    {
        @Override
        public boolean equals(Object b)
        {
            return b instanceof DepthLabel o && o.x == x && o.y == y && o.z == z;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(x, y, z);
        }

        @Override
        public void render(BufferBuilder buffer, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz)
        {
            Font font = Minecraft.getInstance().font;
            poseStack.pushPose();
            poseStack.translate((float)(x - cx), (float)(y - cy), (float)(z - cz));
            poseStack.mulPose(rotation);
            poseStack.scale(0.07F, -0.07F, 0.08F);
            MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
            font.drawInBatch(index, -font.width(index)/2F, -font.lineHeight * 0.5F, color1, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
            immediate.endBatch();

            poseStack.translate(font.width(index)/2F, 0, 0);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            immediate = Minecraft.getInstance().renderBuffers().bufferSource();
            font.drawInBatch(depth, -font.width(depth)/2F, font.lineHeight + 1, color2, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
            immediate.endBatch();
            poseStack.popPose();
        }
    }
}