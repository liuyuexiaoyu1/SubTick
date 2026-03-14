package subtick;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.CarpetSettings;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import subtick.commands.TickCommand;
import subtick.util.Translations;
import subtick.commands.PhaseCommand;
import subtick.commands.QueueCommand;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//#if MC >= 12005
//$$ import subtick.network.PacketRegister;
//$$ import subtick.network.packet.SubTickPayload;
//#endif

//#if MC >= 11903
//$$ import net.minecraft.commands.CommandBuildContext;
//#endif
import com.mojang.brigadier.CommandDispatcher;
import subtick.util.deobfuscator.StackTraceDeobfuscator;

public class SubTick implements CarpetExtension, ModInitializer
{
  public static final Logger LOGGER = LogManager.getLogger();

  public static final String MOD_ID = "subtick";
  //#if MC < 12005
  public static final ResourceLocation SUBTICK_PACKET_ID = new ResourceLocation(MOD_ID, "subtick_payload");
  //#endif
  public static String MOD_NAME = "";
  public static String MOD_VERSION = "";
  public static final boolean hasLithium = FabricLoader.getInstance().isModLoaded("lithium");

  @Override
  public void onInitialize()
  {
    StackTraceDeobfuscator.fetchMapping();
    CarpetServer.manageExtension(new SubTick());
    //#if MC >= 12005
    //$$ PacketRegister.s2c();
    //#endif
  }

  @Override
  public void onGameStarted()
  {
    ModMetadata metadata = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(RuntimeException::new).getMetadata();
    MOD_NAME = metadata.getName();
    MOD_VERSION = metadata.getVersion().getFriendlyString();
    CarpetServer.settingsManager.parseSettingsClass(Settings.class);
    //#if MC < 11901
    Translations.update(CarpetSettings.language.equalsIgnoreCase("none") ? "en_us" : CarpetSettings.language);
    //#endif
  }

  @Override
  //#if MC >= 11903
  //$$ public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
  //#else
  public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
  //#endif
    TickCommand.register(dispatcher);
    PhaseCommand.register(dispatcher);
    QueueCommand.register(dispatcher);
  }

  @Override
  public Map<String, String> canHasTranslations(String lang)
  {
    return Translations.getTranslationFromResourcePath(lang);
  }

  // The order can be different in different versions of the game...
  // public static TickPhase[] getTickPhaseOrder()
  // {
  //   return new TickPhase[]
  //   {
  //     TickPhase.WORLD_BORDER,
  //     TickPhase.WEATHER,
  //     TickPhase.TIME,
  //     TickPhase.BLOCK_TICK,
  //     TickPhase.FLUID_TICK,
  //     TickPhase.RAID,
  //     TickPhase.CHUNK,
  //     TickPhase.BLOCK_EVENT,
  //     TickPhase.ENTITY,
  //     TickPhase.BLOCK_ENTITY,
  //     TickPhase.ENTITY_MANAGEMENT
  //   };
  // }

  @Override
  public void onPlayerLoggedIn(ServerPlayer player) {
    //#if MC >= 12005
    //$$ if (!ServerPlayNetworking.canSend(player, SubTickPayload.TYPE)) return;
    //#else
    if (!ServerPlayNetworking.canSend(player, SubTick.SUBTICK_PACKET_ID)) return;
    //#endif
    ITickHandler tickHandler = ((ITickHandleable)player.getLevel().getServer()).tickHandler();
    subtick.network.ServerNetworkHandler.sendFrozen(player, tickHandler.frozen(), tickHandler.currentPhase());
  }
}
