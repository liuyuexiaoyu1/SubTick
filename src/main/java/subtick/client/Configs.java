package subtick.client;

import java.io.File;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import subtick.client.HudRenderer.Align;

public class Configs implements IConfigHandler
{
  public static final ConfigBoolean
          SHOW_HUD = new ConfigBoolean("showHUD", true, "Controls whether the hud is shown"),
          EXPERIMENTAL_RENDERING = new ConfigBoolean("experimentalRendering", false, "Render block highlights using the new rendering pipeline (with some rendering bugs)");
  public static final ConfigColor
    STEPPED_BG = new ConfigColor("steppedBG", "#80000000", "Background color for things already stepped through"),
    STEPPED_RENDER = new ConfigColor("stepperRender", "#80FFFFFF", "The highlight render color for things already stepped through"),
    STEPPED_TEXT = new ConfigColor("steppedText", "#FFAAAAAA", "Text color for things already stepped through"),
    STEPPED_DEPTH = new ConfigColor("steppedDepth", "#FF004040", "Text color for depth of things already stepped through"),
    STEPPING_BG = new ConfigColor("steppingBG", "#808000FF", "Background color for things being stepped through"),
    STEPPING_RENDER = new ConfigColor("steppingRender", "#808000FF", "The highlight render color for things being stepped through"),
    STEPPING_TEXT = new ConfigColor("steppingText", "#FFFFFFFF", "Text color for things being stepped through"),
    STEPPING_DEPTH = new ConfigColor("steppingDepth", "#FF00FFFF", "Text color for depth of things being stepped through"),
    TO_STEP_BG = new ConfigColor("toStepBG", "#80000000", "Background color for things not stepped through"),
    TO_STEP_RENDER = new ConfigColor("toStepRender", "#80000000", "The highlight render color for things not stepped through"),
    TO_STEP_TEXT = new ConfigColor("toStepText", "#FFFFFFFF", "Text color for things not stepped through"),
    TO_STEP_DEPTH = new ConfigColor("toStepDepth", "#FF00FFFF", "Text color for depth of things not stepped through"),
    NEW_BG = new ConfigColor("newBG", "#80808080", "Background color for newly scheduled things"),
    NEW_RENDER = new ConfigColor("newRender", "#80808080", "The highlight render color for newly scheduled things"),
    NEW_TEXT = new ConfigColor("newText", "#FFFFFFFF", "Text color for newly scheduled things"),
    NEW_DEPTH = new ConfigColor("newDepth", "#FF00FFFF", "Text color for depth of newly scheduled things"),
    SEPARATOR = new ConfigColor("separator", "#80FFFFFF", "Color for separating elements in the HUD"),
    POSITION = new ConfigColor("position", "#80FF0000", "Color for indicating the position in the HUD");
  public static final ConfigOptionList HUD_ALIGNMENT = new ConfigOptionList("hudAlignment", Align.TOP, "Alignment for the HUD");
  public static final ConfigInteger
    HUD_OFFSET_X = new ConfigInteger("hudOffsetX", 0, "X offset for the HUD"),
    HUD_OFFSET_Y = new ConfigInteger("hudOffsetY", 0, "Y offset for the HUD"),
    MAX_QUEUE_SIZE = new ConfigInteger("maxQueueSize", 15, "Maximum number of elements in the queue HUD"),
    MAX_HIGHLIGHT_SIZE = new ConfigInteger("maxHighlightSize", 10, "Maximum number of highlighted elements in the queue HUD\nUseful to control the number of highlights when the queue is bigger than maxQueueSize");

  public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
    EXPERIMENTAL_RENDERING,
    SHOW_HUD,
    STEPPED_BG,
    STEPPED_RENDER,
    STEPPED_TEXT,
    STEPPED_DEPTH,
    STEPPING_BG,
    STEPPING_RENDER,
    STEPPING_TEXT,
    STEPPING_DEPTH,
    TO_STEP_BG,
    TO_STEP_RENDER,
    TO_STEP_TEXT,
    TO_STEP_DEPTH,
    SEPARATOR,
    POSITION,
    HUD_ALIGNMENT,
    HUD_OFFSET_X,
    HUD_OFFSET_Y,
    MAX_QUEUE_SIZE,
    MAX_HIGHLIGHT_SIZE
  );

  @Override
  public void save()
  {
    File dir =
            //#if MC >= 12110
            //$$ FileUtils.getConfigDirectoryAsPath().toFile();
            //#else
            FileUtils.getConfigDirectory();
    //#endif
    if(!(dir.exists() && dir.isDirectory()) && !dir.mkdirs())
      return;

    JsonObject root = new JsonObject();
    ConfigUtils.writeConfigBase(root, "Config", OPTIONS);
    //#if MC >= 26.1
    //$$ JsonUtils.writeJsonToFile(root, new File(dir, "subtick.json").toPath());
    //#else
    JsonUtils.writeJsonToFile(root, new File(dir, "subtick.json"));
    //#endif
  }

  @Override
  public void load()
  {
    File configFile = new File(
            //#if MC >= 12110
            //$$ FileUtils.getConfigDirectoryAsPath().toFile()
            //#else
            FileUtils.getConfigDirectory()
            //#endif
            , "subtick.json");
    if(!configFile.exists() || !configFile.isFile() || !configFile.canRead())
      return;

    //#if MC >= 26.1
    //$$ JsonElement element = JsonUtils.parseJsonFile(configFile.toPath());
    //#else
    JsonElement element = JsonUtils.parseJsonFile(configFile);
    //#endif
    if(element == null || !element.isJsonObject())
      return;

    JsonObject root = element.getAsJsonObject();
    ConfigUtils.readConfigBase(root, "Config", OPTIONS);
  }
}
