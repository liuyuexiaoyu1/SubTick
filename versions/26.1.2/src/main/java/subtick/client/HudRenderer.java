package subtick.client;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
//#if MC >= 26.3
//$$ import com.mojang.renderpearl.api.pipeline.RenderPipeline;
//#else
import com.mojang.blaze3d.pipeline.RenderPipeline;
//#endif
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.tuple.Pair;


import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import subtick.QueueElement;
import subtick.TickPhase;
import subtick.util.Translations;

public class HudRenderer
{
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Font font = mc.font;

    private static Color4f STEPPED_BG;
    private static int STEPPED_TEXT;
    private static Color4f STEPPING_BG;
    private static int STEPPING_TEXT;
    private static Color4f TO_STEP_BG;
    private static int TO_STEP_TEXT;
    private static Color4f NEW_BG;

    private static Pair<Integer, Integer> trimQueue(int max, int maxHighlights)
    {
        List<QueueElement> queue = ClientTickHandler.queue;
        if(queue.size() <= max)
            return Pair.of(0, queue.size());

        int size = ClientTickHandler.queueIndex2 - ClientTickHandler.queueIndex1 + 1;
        if(size > maxHighlights)
        {
            int start = Math.min(queue.size() - max, ClientTickHandler.queueIndex2 - maxHighlights);
            return Pair.of(start, start + max);
        }

        int start = Math.min(queue.size() - max, ClientTickHandler.queueIndex1);
        return Pair.of(start, start + max);
    }

    public static enum Align implements IConfigOptionListEntry
    {
        TOP_LEFT(0, 0, "top_left"), TOP(1, 0, "top"), TOP_RIGHT(2, 0, "top_right"),
        LEFT(0, 1, "left"), CENTER(1, 1, "center"), RIGHT(2, 1, "right"),
        BOTTOM_LEFT(0, 2, "bottom_left"), BOTTOM(1, 2, "bottom"), BOTTOM_RIGHT(2, 2, "bottom_right");

        private final int x, y;
        private final String translationKey;

        private static final Map<String, Align> byString = Map.of(
                "top_left", TOP_LEFT, "top", TOP, "top_right", TOP_RIGHT,
                "left", LEFT, "center", CENTER, "right", RIGHT,
                "bottom_left", BOTTOM_LEFT, "bottom", BOTTOM, "bottom_right", BOTTOM_RIGHT);

        Align(int x, int y, String key)
        {
            this.x = x; this.y = y;
            translationKey = key;
        }

        public int getX(int w)
        {
            return x * ((mc.getWindow().getGuiScaledWidth() - w)/2);
        }

        public int getY(int h)
        {
            return y * ((mc.getWindow().getGuiScaledHeight() - h)/2);
        }

        @Override
        public String getDisplayName()
        {
            return Translations.tr("subtick.client.align." + translationKey);
        }

        @Override
        public String getStringValue()
        {
            return translationKey;
        }

        @Override
        public Align fromString(String name)
        {
            return byString.get(name);
        }

        @Override
        public Align cycle(boolean forwards)
        {
            int id = ordinal();
            if(forwards)
                return values()[++id == values().length ? 0 : id];
            else
                return values()[--id == -1 ? values().length - 1 : id];
        }
    }

    // i am too lazy to add 2 more of these comment thingys so i'm just gonna call it poseStack
    public static void render(GuiGraphicsExtractor poseStack)
    {
        synchronized(ClientTickHandler.class)
        {
            if(!ClientTickHandler.frozen || !Configs.SHOW_HUD.getBooleanValue())
                return;

            STEPPED_BG = Configs.STEPPED_BG.getColor();
            STEPPED_TEXT = Configs.STEPPED_TEXT.getColor().intValue;
            STEPPING_BG = Configs.STEPPING_BG.getColor();
            STEPPING_TEXT = Configs.STEPPING_TEXT.getColor().intValue;
            TO_STEP_BG = Configs.TO_STEP_BG.getColor();
            TO_STEP_TEXT = Configs.TO_STEP_TEXT.getColor().intValue;
            NEW_BG = Configs.NEW_BG.getColor();

            TickPhase tickPhase = ClientTickHandler.tickPhase;

            Align align = (Align)Configs.HUD_ALIGNMENT.getOptionListValue();
            int xOff = Configs.HUD_OFFSET_X.getIntegerValue();
            int yOff = Configs.HUD_OFFSET_Y.getIntegerValue();

            int wPhase = 0;
            int wDim = 0;
            for(int phase = 0; phase < TickPhase.totalPhases; phase ++)
                wPhase = Math.max(wPhase, font.width(TickPhase.getPhaseName(phase)));
            for(String dim : ClientTickHandler.dimensions)
                wDim = Math.max(wDim, font.width(dim));
            wPhase += 2;
            wDim += 2;
            int wDimPhase = wDim + wPhase + 10;

            int h = font.lineHeight + 1;

            if(ClientTickHandler.queue.isEmpty())
                renderHudA(poseStack, tickPhase, align.getX(wDimPhase + 10) + xOff, align.getY(h*TickPhase.totalPhases) + yOff, wDim, wPhase, h);
            else
            {
                Component[] queue = new Component[Math.min(ClientTickHandler.queue.size(), Configs.MAX_QUEUE_SIZE.getIntegerValue())];
                Pair<Integer, Integer> indices = trimQueue(Configs.MAX_QUEUE_SIZE.getIntegerValue(), Configs.MAX_HIGHLIGHT_SIZE.getIntegerValue());
                int wQueue = 0;
                int i = indices.getLeft();
                int j = 0;
                boolean depth = tickPhase.phase() == TickPhase.BLOCK_TICK || tickPhase.phase() == TickPhase.BLOCK_EVENT;
                while(i < indices.getRight())
                {
                    QueueElement element = ClientTickHandler.queue.get(i++);
                    Component s = queue[j++] = text(element, i, depth);
                    wQueue = Math.max(wQueue, font.width(s));
                }
                wQueue += 2;
                int wAll = wDimPhase + wQueue + 10;
                renderHudB(poseStack, queue, tickPhase, ClientTickHandler.queueIndex1 - indices.getLeft(), ClientTickHandler.queueIndex2 - indices.getLeft(), queue.length - ClientTickHandler.newQueueElementCount, align.getX(wAll + 10) + xOff, align.getY(h*Math.max(TickPhase.totalPhases, indices.getRight() - indices.getLeft())) + yOff, wDim, wPhase, wQueue, h);
            }
            poseStack.pose();
        }
    }

    private static String color(ConfigColor color)
    {
        return "#" + color.getStringValue().substring(3);
    }

    private static Component text(QueueElement element, int i, boolean depth)
    {
        assert Minecraft.getInstance().level != null;
        return depth ?
                fromJsonLenient(String.format("[\"#%d (\", {\"color\":\"%s\",\"text\":\"%d\"}, \"): %s\"]", i, color(i <= ClientTickHandler.queueIndex1 ? Configs.STEPPED_DEPTH : i <= ClientTickHandler.queueIndex2 ? Configs.STEPPING_DEPTH : i >= ClientTickHandler.queue.size() - ClientTickHandler.newQueueElementCount ? Configs.NEW_DEPTH : Configs.TO_STEP_DEPTH), element.depth(), element.label())
                        , Minecraft.getInstance().level.registryAccess()
                ) :
                Component.literal(String.format("#%d: %s", i, element.label()));
    }
    static MutableComponent deserialize(JsonElement jsonElement, HolderLookup.Provider provider) {
        return (MutableComponent) ComponentSerialization.CODEC
                .parse(provider.createSerializationContext(JsonOps.INSTANCE), jsonElement)
                .getOrThrow(JsonParseException::new);
    }
    @Nullable
    public static MutableComponent fromJsonLenient(String string, HolderLookup.Provider provider) {
        JsonReader jsonReader = new JsonReader(new StringReader(string));
        jsonReader.setStrictness(Strictness.LENIENT);
        JsonElement jsonElement = JsonParser.parseReader(jsonReader);
        return jsonElement == null ? null : deserialize(jsonElement, provider);
    }

    public static void renderHudA(
            GuiGraphicsExtractor guiGraphics,
            TickPhase phase, int x, int y, int wDim, int wPhase, int h)
    {
        drawTableB(guiGraphics, x, y, h, wDim, ClientTickHandler.dimensions.size(), phase.dim());
        drawTableA(guiGraphics, x+wDim+10, y, h, wPhase, TickPhase.totalPhases, phase.phase());

        x += 2;
        for(int y1 = y+2, i = 0; i < ClientTickHandler.dimensions.size(); i++, y1 += h)
            guiGraphics.text(font, ClientTickHandler.dimensions.get(i), x, y1, i < phase.dim() ? STEPPED_TEXT : TO_STEP_TEXT, false);

        x += wDim + 10;
        for(int y1 = y+2, i = 0; i < TickPhase.totalPhases; i++, y1 += h)
            guiGraphics.text(font, TickPhase.getPhaseName(i), x, y1, i < phase.phase() ? STEPPED_TEXT : TO_STEP_TEXT, false);
    }

    public static void renderHudB(
            GuiGraphicsExtractor guiGraphics,
            Component[] queue, TickPhase phase, int iqueue1, int iqueue2, int iqueue3, int x, int y, int wDim, int wPhase, int wQueue, int h)
    {
        drawTableB(guiGraphics, x, y, h, wDim, ClientTickHandler.dimensions.size(), phase.dim());
        drawTableB(guiGraphics, x+wDim+10, y, h, wPhase, TickPhase.totalPhases, phase.phase());
        drawTableC(guiGraphics, x+wDim+wPhase+20, y, h, wQueue, queue.length, iqueue1, iqueue2, iqueue3);

        int sx = x + wDim + 10 + wPhase + 1;
        int sy = y + phase.phase() * h + 1;
        drawQuad(guiGraphics,
                sx, sy, sx, sy + h - 1,
                sx + 9, y + queue.length * h, sx + 9, y, Configs.POSITION.getColor());

        x += 2;
        for(int y1 = y+2, i = 0; i < ClientTickHandler.dimensions.size(); i++, y1 += h)
            guiGraphics.text(font, ClientTickHandler.dimensions.get(i), x, y1, i < phase.dim() ? STEPPED_TEXT : TO_STEP_TEXT, false);

        x += wDim + 10;
        for(int y1 = y+2, i = 0; i < TickPhase.totalPhases; i++, y1 += h)
            guiGraphics.text(font, TickPhase.getPhaseName(i), x, y1, i < phase.phase() ? STEPPED_TEXT : TO_STEP_TEXT, false);

        x += wPhase + 10;
        for(int y1 = y+2, i = 0; i < queue.length; i++, y1 += h)
            guiGraphics.text(font, queue[i], x, y1, i < iqueue1 ? STEPPED_TEXT : i < iqueue2 ? STEPPING_TEXT : TO_STEP_TEXT, false);
    }

    private static void drawTableA(GuiGraphicsExtractor guiGraphics, int x, int y, int h, int w, int count, int index)
    {
        // Left & right borders
        int Y = y + count * h + 1;
        int X = x + w;
        drawRect(guiGraphics, x, y, x + 1, Y, Configs.SEPARATOR.getColor());
        drawRect(guiGraphics, X, y, X + 1, Y, Configs.SEPARATOR.getColor());
        x += 1;
        w -= 1;
        h -= 1;

        // Fill
        for(int i = 0; i < count; i++)
        {
            if(i == index)
            {
                drawQuad(guiGraphics, X, y, X, y+1, X+5, y+3, X+5, y-3, Configs.POSITION.getColor());
                drawRect(guiGraphics, x, y, X, y += 1, Configs.POSITION.getColor());
            }
            else
                drawRect(guiGraphics, x, y, X, y += 1, Configs.SEPARATOR.getColor());
            drawRect(guiGraphics, x, y, X, y += h, i < index ? STEPPED_BG : TO_STEP_BG);
        }
        drawRect(guiGraphics, x, y, X, y + 1, Configs.SEPARATOR.getColor());
    }

    private static void drawTableB(GuiGraphicsExtractor guiGraphics, int x, int y, int h, int w, int count, int index)
    {
        // Left & right borders
        int Y = y + count * h + 1;
        int X = x + w;
        drawRect(guiGraphics, x, y, x + 1, Y, Configs.SEPARATOR.getColor());
        drawRect(guiGraphics, X, y, X + 1, Y, Configs.SEPARATOR.getColor());
        x += 1;
        w -= 1;
        h -= 1;

        // Middle
        for(int i = 0; i < count; i++)
        {
            drawRect(guiGraphics, x, y, X, y += 1, Configs.SEPARATOR.getColor()); // Border
            drawRect(guiGraphics, x, y, X, y += h, i < index ? STEPPED_BG : i == index ? Configs.POSITION.getColor() : TO_STEP_BG); // Fill
        }
        drawRect(guiGraphics, x, y, X, y + 1, Configs.SEPARATOR.getColor());
    }

    private static void drawTableC(GuiGraphicsExtractor guiGraphics, int x, int y, int h, int w, int count, int index1, int index2, int index3)
    {
        // Left & right borders
        int Y = y + count * h + 1;
        int X = x + w;
        drawRect(guiGraphics, x, y, x + 1, Y, Configs.SEPARATOR.getColor());
        drawRect(guiGraphics, X, y, X + 1, Y, Configs.SEPARATOR.getColor());
        x += 1;
        w -= 1;
        h -= 1;

        // Middle
        for(int i = 0; i < count; i++)
        {
            if(i == index2)
            {
                drawQuad(guiGraphics, X, y, X, y+1, X+5, y+3, X+5, y-3, Configs.POSITION.getColor());
                drawRect(guiGraphics, x, y, X, y += 1, Configs.POSITION.getColor());
                drawRect(guiGraphics, x, y, X, y += h, TO_STEP_BG);
            }
            else
            {
                drawRect(guiGraphics, x, y, X, y += 1, Configs.SEPARATOR.getColor());
                drawRect(guiGraphics, x, y, X, y += h, i < index1 ? STEPPED_BG : i < index2 ? STEPPING_BG : i >= index3 ? NEW_BG : TO_STEP_BG);
            }
        }
        drawRect(guiGraphics, x, y, X, y + 1, Configs.SEPARATOR.getColor());
    }

    private static void drawRect(GuiGraphicsExtractor guiGraphics, int x, int y, int X, int Y, Color4f color)
    {
        guiGraphics.fill(x, y, X, Y, color.intValue);
    }

    private static void drawQuad(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4, Color4f color)
    {
        Vector2f[] vertices = {
                new Vector2f((float)x1, (float)y1),
                new Vector2f((float)x2, (float)y2),
                new Vector2f((float)x3, (float)y3),
                new Vector2f((float)x4, (float)y4)
        };

        guiGraphics.guiRenderState.addGuiElement(new CustomMeshRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                guiGraphics.pose(),
                vertices,
                color.intValue,
                guiGraphics.scissorStack.peek()
        ));
    }

    public record CustomMeshRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            Vector2f[] vertices,
            int color,
            @Nullable ScreenRectangle scissorArea
    ) implements GuiElementRenderState {

        @Override
        //#if MC >= 12110
        public void buildVertices(VertexConsumer vertexConsumer) {
        //#else
        //$$ public void buildVertices(VertexConsumer vertexConsumer, float z) {
            //#endif
            for (int i = 0; i < 4; i++) {
                Vector2f v = (i < vertices.length) ? vertices[i] : vertices[vertices.length - 1];
                //#if MC >= 12110
                vertexConsumer.addVertexWith2DPose(this.pose, v.x, v.y)
                //#else
                //$$ vertexConsumer.addVertexWith2DPose(this.pose, v.x, v.y, z)
                        //#endif
                        .setColor(this.color);
            }
        }

        @Override
        public ScreenRectangle bounds() {
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;

            for (Vector2f v : vertices) {
                minX = Math.min(minX, v.x);
                minY = Math.min(minY, v.y);
                maxX = Math.max(maxX, v.x);
                maxY = Math.max(maxY, v.y);
            }

            return new ScreenRectangle((int)minX, (int)minY, (int)(maxX - minX), (int)(maxY - minY));
        }

        @Override
        public @NotNull RenderPipeline pipeline() { return pipeline; }

        @Override
        public @NotNull TextureSetup textureSetup() { return textureSetup; }

        @Override
        @Nullable
        public ScreenRectangle scissorArea() { return scissorArea; }
    }
}
