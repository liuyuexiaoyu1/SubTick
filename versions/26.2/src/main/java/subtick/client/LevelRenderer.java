package subtick.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.dy.masa.malilib.util.data.Color4f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.stream.Collectors;

public class LevelRenderer
{
    private static final Minecraft mc = Minecraft.getInstance();
    private static final HashSet<Pos> hlPos = new HashSet<>();
    private static final HashSet<Text> texts = new HashSet<>();
    public static final HashMap<BlockPos,Integer> hlBe = new HashMap<>();
    private static final HashMap<BlockPos,Vec3> hlPistonOffsets = new HashMap<>();

    public static synchronized void render(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, boolean renderText) {
        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cpos = camera.position();
        if (!renderText) {
            LevelRenderer.hlBe.clear();
            hlPistonOffsets.clear();
            if (Configs.EXPERIMENTAL_RENDERING.getBooleanValue()) {
                Map<Integer, List<Outline>> groupedOutlines = hlPos.stream()
                        .filter(p -> p instanceof Outline)
                        .map(o -> (Outline) o)
                        .collect(Collectors.groupingBy(o -> o.color().intValue));

                for (Map.Entry<Integer, List<Outline>> entry : groupedOutlines.entrySet()) {
                    for (Outline o : entry.getValue()) {
                        o.render(poseStack, camera, submitNodeCollector, mc.level, true);
                    }
                }
            } else {
                for (Pos pos : hlPos) {
                    pos.render(poseStack, camera, submitNodeCollector, mc.level, false);
                }
            }
        } else {
            if (!texts.isEmpty()) {
                for (Text text : texts) {
                    text.render(submitNodeCollector, poseStack, camera.rotation(), cpos.x, cpos.y, cpos.z);
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
        void render(PoseStack poseStack, Camera camera, SubmitNodeCollector output, Level level, boolean NEW);
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

        @Override
        public void render(PoseStack poseStack, Camera camera, SubmitNodeCollector output, Level level, boolean NEW)
        {
            if (!NEW) {
                // Non-experimental: store piston offset for text positioning, render colored cube
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof PistonMovingBlockEntity movingBlock) {
                    hlPistonOffsets.put(pos.immutable(), new Vec3(movingBlock.getXOff(1.0f), movingBlock.getYOff(1.0f), movingBlock.getZOff(1.0f)));
                }
                Vec3 pistonOff = hlPistonOffsets.get(pos);
                double pox = pistonOff != null ? pistonOff.x : 0;
                double poy = pistonOff != null ? pistonOff.y : 0;
                double poz = pistonOff != null ? pistonOff.z : 0;
                if (output instanceof SubmitNodeStorage storage) {
                    SubmitNodeCollection collection = storage.order(0);
                    float r = color.r, g = color.g, b = color.b, a = color.a;
                    Vec3 cpos = camera.position();
                    float x = (float)(pos.getX() + pox - cpos.x), y = (float)(pos.getY() + poy - cpos.y), z = (float)(pos.getZ() + poz - cpos.z);
                    float X = x + 1, Y = y + 1, Z = z + 1;
                    var pose = poseStack.last().copy();
                    var submit = new CustomFeatureRenderer.Submit(pose, RenderTypes.debugQuads(), (p, buffer) -> {
                        buffer.addVertex(x, y, z).setColor(r, g, b, a).setNormal(0, 0, -1).setLineWidth(1);
                        buffer.addVertex(x, Y, z).setColor(r, g, b, a).setNormal(0, 0, -1).setLineWidth(1);
                        buffer.addVertex(x, Y, Z).setColor(r, g, b, a).setNormal(0, 0, -1).setLineWidth(1);
                        buffer.addVertex(x, y, Z).setColor(r, g, b, a).setNormal(0, 0, -1).setLineWidth(1);
                        buffer.addVertex(X, y, z).setColor(r, g, b, a).setNormal(0, 0, 1).setLineWidth(1);
                        buffer.addVertex(X, y, Z).setColor(r, g, b, a).setNormal(0, 0, 1).setLineWidth(1);
                        buffer.addVertex(X, Y, Z).setColor(r, g, b, a).setNormal(0, 0, 1).setLineWidth(1);
                        buffer.addVertex(X, Y, z).setColor(r, g, b, a).setNormal(0, 0, 1).setLineWidth(1);
                        buffer.addVertex(x, y, z).setColor(r, g, b, a).setNormal(-1, 0, 0).setLineWidth(1);
                        buffer.addVertex(x, y, Z).setColor(r, g, b, a).setNormal(-1, 0, 0).setLineWidth(1);
                        buffer.addVertex(X, y, Z).setColor(r, g, b, a).setNormal(-1, 0, 0).setLineWidth(1);
                        buffer.addVertex(X, y, z).setColor(r, g, b, a).setNormal(-1, 0, 0).setLineWidth(1);
                        buffer.addVertex(x, Y, z).setColor(r, g, b, a).setNormal(1, 0, 0).setLineWidth(1);
                        buffer.addVertex(X, Y, z).setColor(r, g, b, a).setNormal(1, 0, 0).setLineWidth(1);
                        buffer.addVertex(X, Y, Z).setColor(r, g, b, a).setNormal(1, 0, 0).setLineWidth(1);
                        buffer.addVertex(x, Y, Z).setColor(r, g, b, a).setNormal(1, 0, 0).setLineWidth(1);
                        buffer.addVertex(x, y, z).setColor(r, g, b, a).setNormal(0, -1, 0).setLineWidth(1);
                        buffer.addVertex(X, y, z).setColor(r, g, b, a).setNormal(0, -1, 0).setLineWidth(1);
                        buffer.addVertex(X, Y, z).setColor(r, g, b, a).setNormal(0, -1, 0).setLineWidth(1);
                        buffer.addVertex(x, Y, z).setColor(r, g, b, a).setNormal(0, -1, 0).setLineWidth(1);
                        buffer.addVertex(x, y, Z).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(1);
                        buffer.addVertex(x, Y, Z).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(1);
                        buffer.addVertex(X, Y, Z).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(1);
                        buffer.addVertex(X, y, Z).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(1);
                    });
                    collection.alwaysOnTop.submit(submit);
                }
            } else {
                poseStack.pushPose();
                Vec3 cpos = camera.position();
                poseStack.translate(pos.getX() - cpos.x, pos.getY() - cpos.y, pos.getZ() - cpos.z);
                BlockState state = level.getBlockState(pos);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    if (blockEntity instanceof PistonMovingBlockEntity movingBlock) {
                        float ox = movingBlock.getXOff(1.0f);
                        float oy = movingBlock.getYOff(1.0f);
                        float oz = movingBlock.getZOff(1.0f);
                        hlPistonOffsets.put(pos.immutable(), new Vec3(ox, oy, oz));
                        if (movingBlock.isExtending()) {
                            hlBe.put(pos.relative(movingBlock.getDirection().getOpposite()), color.intValue);
                        } else {
                            hlBe.put(pos.immutable(), color.intValue);
                        }
                    } else {
                        hlBe.put(pos.immutable(), color.intValue);
                    }
                }
                if (state.getRenderShape() != RenderShape.MODEL) {
                    poseStack.popPose();
                    return;
                }
                int lightCoords = LightCoordsUtil.getLightCoords(level, pos);
                RenderType outlineType = RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS);
                BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(state);
                List<BlockStateModelPart> parts = new ObjectArrayList<>();
                model.collectParts(mc.level.getRandom(), parts);
                output.submitBlockModel(
                        poseStack, outlineType, parts, new int[0],
                        lightCoords, OverlayTexture.NO_OVERLAY, color.intValue
                );
                poseStack.popPose();
            }
        }
    }

    @SuppressWarnings("all")
    public static class OutlineCollectorWrapper implements SubmitNodeCollector {
        private final SubmitNodeCollector delegate;
        private final int outlineColor;

        public OutlineCollectorWrapper(SubmitNodeCollector delegate, int outlineColor) {
            this.delegate = delegate;
            this.outlineColor = outlineColor;
        }

        @Override
        public OrderedSubmitNodeCollector order(int i) {
            return delegate.order(i);
        }

        @Override
        public void submitShadow(PoseStack poseStack, float f, List<EntityRenderState.ShadowPiece> list) {
            delegate.submitShadow(poseStack, f, list);
        }

        @Override
        public void submitNameTag(@NonNull PoseStack poseStack, @Nullable Vec3 vec3, int i, @NonNull Component component, boolean bl, int j, CameraRenderState cameraRenderState) {
            delegate.submitNameTag(poseStack, vec3, i, component, bl, j, cameraRenderState);
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
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
            delegate.submitMovingBlock(poseStack, movingBlockRenderState, this.outlineColor);
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
            delegate.submitBlockModel(poseStack, renderType, parts, tintLayers, lightCoords, overlayCoords, this.outlineColor);
        }

        @Override
        public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress) {
            delegate.submitBreakingBlockModel(poseStack, parts, progress);
        }

        @Override
        public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {
            delegate.submitShapeOutline(poseStack, shape, renderType, this.outlineColor, width, afterTerrain);
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
            delegate.submitItem(poseStack, displayContext, lightCoords, overlayCoords, this.outlineColor, tintLayers, quads, foilType);
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
            delegate.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
        }

        @Override
        public void submitQuadParticleGroup(QuadParticleRenderState quadParticleRenderState) {
            delegate.submitQuadParticleGroup(quadParticleRenderState);
        }

        @Override
        public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {
            delegate.submitGizmoPrimitives(group, camera, onTop);
        }
    }

    private interface Text
    {
        FormattedCharSequence getSequence();
        void render(SubmitNodeCollector collector, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz);
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
        public FormattedCharSequence getSequence() {
            return FormattedCharSequence.forward(text, Style.EMPTY);
        }

        @Override
        public void render(SubmitNodeCollector collector, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz)
        {
            Font font = Minecraft.getInstance().font;
            FormattedCharSequence seq = getSequence();
            BlockPos pos = BlockPos.containing(x, y, z);
            Vec3 offset = hlPistonOffsets.get(pos);
            double offX = offset != null ? offset.x : 0;
            double offY = offset != null ? offset.y : 0;
            double offZ = offset != null ? offset.z : 0;
            poseStack.pushPose();
            poseStack.translate((float)(x + offX - cx), (float)(y + offY - cy), (float)(z + offZ - cz));
            poseStack.mulPose(rotation);
            poseStack.scale(0.07F, -0.07F, 0.07F);
            collector.submitText(poseStack, -font.width(seq)/2F, -font.lineHeight * 0.5F, seq, false, Font.DisplayMode.SEE_THROUGH, 0xF000F0, color.intValue, 0x00000000, 0x00000000);
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
        public FormattedCharSequence getSequence() {
            return FormattedCharSequence.forward(index, Style.EMPTY);
        }

        @Override
        public void render(SubmitNodeCollector collector, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz)
        {
            Font font = Minecraft.getInstance().font;
            FormattedCharSequence seqIndex = FormattedCharSequence.forward(index, Style.EMPTY);
            FormattedCharSequence seqDepth = FormattedCharSequence.forward(depth, Style.EMPTY);
            BlockPos pos = BlockPos.containing(x, y, z);
            Vec3 offset = hlPistonOffsets.get(pos);
            double offX = offset != null ? offset.x : 0;
            double offY = offset != null ? offset.y : 0;
            double offZ = offset != null ? offset.z : 0;

            poseStack.pushPose();
            poseStack.translate((float)(x + offX - cx), (float)(y + offY - cy), (float)(z + offZ - cz));
            poseStack.mulPose(rotation);
            poseStack.scale(0.07F, -0.07F, 0.08F);
            collector.submitText(poseStack, -font.width(seqIndex)/2F, -font.lineHeight * 0.5F, seqIndex, false, Font.DisplayMode.SEE_THROUGH, 0xF000F0, color1, 0x00000000, 0x00000000);

            poseStack.translate(font.width(seqIndex)/2F, 0, 0);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            collector.submitText(poseStack, -font.width(seqDepth)/2F, font.lineHeight + 1, seqDepth, false, Font.DisplayMode.SEE_THROUGH, 0xF000F0, color2, 0x00000000, 0x00000000);
            poseStack.popPose();
        }
    }
}
