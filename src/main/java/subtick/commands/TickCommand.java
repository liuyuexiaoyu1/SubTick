package subtick.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import subtick.ITickHandler;
import subtick.TickPhase;
//#if MC >= 12111
//$$ import net.minecraft.commands.Commands;
//#endif

public class TickCommand
{
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
  {
    dispatcher.register(
      literal("tick")
              //#if MC >= 12111
              //$$ .requires(c -> Commands.LEVEL_GAMEMASTERS.check(c.permissions()))
              //#else
              .requires(c -> c.hasPermission(2))
              //#endif
      //.then(literal("debug")
      //  .executes((c) -> ITickHandler.get(c).printDebugInfo(c.getSource()))
      //)
      .then(literal("when")
        .executes((c) -> ITickHandler.get(c).when(c.getSource()))
      )
      .then(literal("freeze")
        .then(argument("phase", word())
          .suggests((c, b) -> suggest(TickPhase.commandSuggestions, b))
          .executes((c) -> ITickHandler.get(c).toggleFreeze(c.getSource(), TickPhase.byCommandKey(getString(c, "phase"))))
        )
        // Carpet parity
        .then(literal("on")
          .then(argument("phase", word())
            .suggests((c, b) -> suggest(TickPhase.commandSuggestions, b))
            .executes((c) -> ITickHandler.get(c).freeze(c.getSource(), TickPhase.byCommandKey(getString(c, "phase"))))
          )
          // .executes((c) -> freeze(c.getSource(), TickPhase.byCommandKey(Settings.subtickDefaultPhase)))
        )
        // Carpet parity
        // .then(literal("off")
        //   .executes((c) -> unfreeze(c.getSource()))
        // )
        // Carpet parity
        // .then(literal("status")
        //   .executes((c) -> when(c.getSource()))
        // )
        // .executes((c) -> toggleFreeze(c.getSource(), TickPhase.byCommandKey(Settings.subtickDefaultPhase)))
      )
              //#if MC < 12003
      .then(literal("step")
        .then(argument("ticks", integer(0))
          .then(argument("phase", word())
            .suggests((c, b) -> suggest(TickPhase.commandSuggestions, b))
            .executes((c) -> ITickHandler.get(c).step(c.getSource(), getInteger(c, "ticks"), TickPhase.byCommandKey(getString(c, "phase"))))
          )
          // .executes((c) -> step(c.getSource(), getInteger(c, "ticks"), TickPhase.byCommandKey(Settings.subtickDefaultPhase)))
        )
        // .executes((c) -> step(c.getSource(), 1, TickPhase.byCommandKey(Settings.subtickDefaultPhase)))
      )
              //#endif
    );
  }
}
