package subtick.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
//#if MC < 11900
import com.mojang.math.Quaternion;
//#endif
import com.mojang.math.Transformation;

import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
//#if MC >= 11900
//$$ import org.joml.Matrix4fStack;
//$$ import org.joml.Quaternionf;
//#endif

public class LevelRenderer
{
  private static final Minecraft mc = Minecraft.getInstance();
  private static final Font font = mc.font;
  private static final HashSet<Pos> hlPos = new HashSet<>();
  private static final HashSet<Text> texts = new HashSet<>();
  private static final HashMap<BlockPos,Vec3> hlPistonOffsets = new HashMap<>();

  public static synchronized void render(PoseStack poseStack, OutlineBufferSource outlineBufferSource, boolean renderText)
  {
    hlPistonOffsets.clear();
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.disableDepthTest();
    Camera camera = mc.gameRenderer.getMainCamera();
    Vec3 cpos = camera.getPosition();

    Tesselator tesselator = Tesselator.getInstance();
    BufferBuilder buffer = tesselator.getBuilder();
    if (!renderText && Configs.EXPERIMENTAL_RENDERING.getBooleanValue()) {
      Map<Integer, List<Outline>> groupedOutlines = hlPos.stream()
              .filter(p -> p instanceof Outline)
              .map(o -> (Outline) o)
              .collect(Collectors.groupingBy(o -> o.color().intValue));

      for (Map.Entry<Integer, List<Outline>> entry : groupedOutlines.entrySet()) {
        int color = entry.getKey();
        setOutlineColor(outlineBufferSource, color);
        for (Pos pos : entry.getValue()) {
          pos.render(null, poseStack, camera, outlineBufferSource, mc.level, true);
        }
        setOutlineColor(outlineBufferSource, -1);
      }
    } else {
      if (!hlPos.isEmpty() && !Configs.EXPERIMENTAL_RENDERING.getBooleanValue()) {
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Pos pos : hlPos) {
          pos.render(buffer, poseStack, camera, outlineBufferSource, mc.level, false);
        }
        tesselator.end();
      }
      //#if MC < 12006
      poseStack = RenderSystem.getModelViewStack();
      //#endif
      //#if MC >= 11900
      //$$ Quaternionf rot = camera.rotation();
      //#else
      Quaternion rot = camera.rotation();
      //#endif
      for (Text text : texts)
        text.render(buffer, poseStack, rot, cpos.x, cpos.y, cpos.z);
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
    void render(BufferBuilder buffer, PoseStack poseStack, Camera camera, OutlineBufferSource outlineBufferSource, Level level, boolean NEW);
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
    public void render(BufferBuilder buffer, PoseStack poseStack, Camera camera, OutlineBufferSource outlineBufferSource, Level level, boolean NEW)
    {
      if (!NEW) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PistonMovingBlockEntity movingBlock) {
          hlPistonOffsets.put(pos.immutable(), new Vec3(movingBlock.getXOff(1.0f), movingBlock.getYOff(1.0f), movingBlock.getZOff(1.0f)));
        }
        Vec3 po = hlPistonOffsets.get(pos);
        double pox = po != null ? po.x : 0, poy = po != null ? po.y : 0, poz = po != null ? po.z : 0;
        double cx = camera.getPosition().x;
        double cy = camera.getPosition().y;
        double cz = camera.getPosition().z;
        double x1 = pos.getX() + pox;
        double y1 = pos.getY() + poy;
        double z1 = pos.getZ() + poz;
        double x2 = x1 + 1;
        double y2 = y1 + 1;
        double z2 = z1 + 1;
        double x = x1 - cx, y = y1 - cy, z = z1 - cz;
        double X = x2 - cx, Y = y2 - cy, Z = z2 - cz;
        buffer.vertex(x, y, z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(x, Y, z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(x, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(x, y, Z).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.vertex(X, y, z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(X, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(X, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(X, Y, z).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.vertex(x, y, z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(x, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(X, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(X, y, z).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.vertex(x, Y, z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(X, Y, z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(X, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(x, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.vertex(x, y, z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(X, y, z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(X, Y, z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(x, Y, z).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.vertex(x, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(x, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(X, Y, Z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(X, y, Z).color(color.r, color.g, color.b, color.a).endVertex();
        return;
      }
      BlockState state = level.getBlockState(pos);
      BlockRenderDispatcher blockRenderManager = mc.getBlockRenderer();
      BlockEntity blockEntity = level.getBlockEntity(pos);
      poseStack.pushPose();
      Vec3 cpos = camera.getPosition();
      poseStack.translate(pos.getX() - cpos.x, pos.getY() - cpos.y, pos.getZ() - cpos.z);

      if (blockEntity != null) {
        if (blockEntity instanceof PistonMovingBlockEntity movingBlock) {
          hlPistonOffsets.put(pos.immutable(), new Vec3(movingBlock.getXOff(1.0f), movingBlock.getYOff(1.0f), movingBlock.getZOff(1.0f)));
        }
        BlockEntityRenderDispatcher blockEntityRenderDispatcher = mc.getBlockEntityRenderDispatcher();
        blockEntityRenderDispatcher.render(blockEntity, 1.0f, poseStack, new InvisibleOutlineBufferSource(outlineBufferSource));
      }
      if (state.getRenderShape() != RenderShape.MODEL) {
        poseStack.popPose();
        return;
      }
      BakedModel model = blockRenderManager.getBlockModel(state);
      VertexConsumer vertexConsumer = outlineBufferSource.getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS));
      InvisibleVertexConsumer invisibleConsumer = new InvisibleVertexConsumer(vertexConsumer);
      blockRenderManager.getModelRenderer().renderModel(
              poseStack.last(),
              invisibleConsumer,
              state,
              model,
              color.r, color.g, color.b,
              net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos),
              OverlayTexture.NO_OVERLAY
      );
      poseStack.popPose();
    }
  }

  public static void setOutlineColor(OutlineBufferSource outlineProvider, int color) {
    int red = (color >> 16) & 0xFF;
    int green = (color >> 8) & 0xFF;
    int blue = color & 0xFF;
    int alpha = (color >> 24) & 0xFF;

    if (alpha == 0) {
      alpha = 255;
    }

    outlineProvider.setColor(red, green, blue, alpha);
  }

  public static class InvisibleOutlineBufferSource implements MultiBufferSource {
    private final OutlineBufferSource outlineBufferSource;

    public InvisibleOutlineBufferSource(OutlineBufferSource outlineBufferSource) {
      this.outlineBufferSource = outlineBufferSource;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull VertexConsumer getBuffer(RenderType renderType) {
      VertexConsumer vertexConsumer = this.outlineBufferSource.getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS));
      return new InvisibleVertexConsumer(vertexConsumer);
    }
  }

  private static class InvisibleVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;

    public InvisibleVertexConsumer(VertexConsumer delegate) {
      this.delegate = delegate;
    }

    @Override
    public @NotNull VertexConsumer vertex(double d, double e, double f) {
      this.delegate.vertex(d, e, f);
      return this;
    }

    @Override
    public @NotNull VertexConsumer color(int red, int green, int blue, int alpha) {
      this.delegate.color(red, green, blue, 0);
      return this;
    }

    @Override
    public @NotNull VertexConsumer uv(float u, float v) {
      this.delegate.uv(u, v);
      return this;
    }

    @Override
    public @NotNull VertexConsumer overlayCoords(int u, int v) {
      this.delegate.overlayCoords(u, v);
      return this;
    }

    @Override
    public @NotNull VertexConsumer uv2(int u, int v) {
      this.delegate.uv2(u, v);
      return this;
    }

    @Override
    public @NotNull VertexConsumer normal(float x, float y, float z) {
     this.delegate.normal(x, y, z);
      return this;
    }

    @Override
    public void endVertex() {
      this.delegate.endVertex();
    }

    @Override
    public void defaultColor(int i, int j, int k, int l) {
      this.delegate.defaultColor(i, j, k, l);
    }

    @Override
    public void unsetDefaultColor() {
      this.delegate.unsetDefaultColor();
    }
  }

  private interface Text
  {
    //#if MC >= 11900
    //$$ public void render(BufferBuilder builder, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz);
    //#else
    void render(BufferBuilder buffer, PoseStack poseStack, Quaternion rotation, double cx, double cy, double cz);
    //#endif
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
    //#if MC >= 11900
    //$$ public void render(BufferBuilder buffer, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz)
    //#else
    public void render(BufferBuilder buffer, PoseStack poseStack, Quaternion rotation, double cx, double cy, double cz)
    //#endif
    {
      BlockPos bpos = new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
      Vec3 offset = hlPistonOffsets.get(bpos);
      double ox = offset != null ? offset.x : 0, oy = offset != null ? offset.y : 0, oz = offset != null ? offset.z : 0;
      poseStack.pushPose();
      poseStack.translate((float)(x + ox - cx), (float)(y + oy - cy), (float)(z + oz - cz));
      poseStack.mulPose(rotation);
      poseStack.scale(-0.07F, -0.07F, 0.07F);
      RenderSystem.applyModelViewMatrix();
      MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(buffer);
      //#if MC >= 12006
      //$$ font.drawInBatch(text, -font.width(text)/2F, -font.lineHeight * 0.5F, color.intValue, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      //#if MC >= 11904
      //$$ font.drawInBatch(text, -font.width(text)/2F, -font.lineHeight * 0.5F, color.intValue, false, Transformation.identity().getMatrix(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      font.drawInBatch(text, -font.width(text)/2F, -font.lineHeight * 0.5F, color.intValue, false, Transformation.identity().getMatrix(), immediate, true, 0x00000000, 0x00000000);
      //#endif
      //#endif
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
    //#if MC >= 11900
    //$$ public void render(BufferBuilder buffer, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz)
    //#else
    public void render(BufferBuilder buffer, PoseStack poseStack, Quaternion rotation, double cx, double cy, double cz)
    //#endif
    {
      BlockPos bpos = new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
      Vec3 offset = hlPistonOffsets.get(bpos);
      double ox = offset != null ? offset.x : 0, oy = offset != null ? offset.y : 0, oz = offset != null ? offset.z : 0;
      poseStack.pushPose();
      poseStack.translate((float)(x + ox - cx), (float)(y + oy - cy), (float)(z + oz - cz));
      poseStack.mulPose(rotation);
      poseStack.scale(-0.07F, -0.07F, 0.08F);
      RenderSystem.applyModelViewMatrix();
      MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(buffer);
      //#if MC >= 12006
      //$$ font.drawInBatch(index, -font.width(index)/2F, -font.lineHeight * 0.5F, color1, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      //#if MC >= 11904
      //$$ font.drawInBatch(index, -font.width(index)/2F, -font.lineHeight * 0.5F, color1, false, Transformation.identity().getMatrix(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      font.drawInBatch(index, -font.width(index)/2F, -font.lineHeight * 0.5F, color1, false, Transformation.identity().getMatrix(), immediate, true, 0x00000000, 0x00000000);
      //#endif
      //#endif
      immediate.endBatch();

      poseStack.translate(font.width(index)/2F, 0, 0);
      poseStack.scale(0.5F, 0.5F, 0.5F);
      RenderSystem.applyModelViewMatrix();
      immediate = MultiBufferSource.immediate(buffer);
      //#if MC >= 12006
      //$$ font.drawInBatch(depth, -font.width(depth)/2F, font.lineHeight + 1, color2, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      //#if MC >= 11904
      //$$ font.drawInBatch(depth, -font.width(depth)/2F, font.lineHeight + 1, color2, false, Transformation.identity().getMatrix(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      font.drawInBatch(depth, -font.width(depth)/2F, font.lineHeight + 1, color2, false, Transformation.identity().getMatrix(), immediate, true, 0x00000000, 0x00000000);
      //#endif
      //#endif
      immediate.endBatch();

      poseStack.popPose();
    }
  }
}