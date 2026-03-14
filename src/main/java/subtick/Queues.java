package subtick;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;

import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Triple;

import subtick.network.ServerNetworkHandler;
import subtick.queues.BlockEventQueue;
import subtick.queues.TickingQueue;
import subtick.util.Translations;
import subtick.util.deobfuscator.StackTraceDeobfuscator;
//#if MC > 11802
//$$ import net.minecraft.network.chat.Component;
//#endif

import java.util.Arrays;
import java.util.stream.Collectors;

public class Queues implements IQueues
{
  public static final DynamicCommandExceptionType INVALID_QUEUE_EXCEPTION = new DynamicCommandExceptionType(key -> new LiteralMessage("Invalid queue '" + key + "'"));

  private final TickHandler tickHandler;

  public Queues(TickHandler tickHandler)
  {
    this.tickHandler = tickHandler;
  }

  private TickingQueue queue;
  private TickingQueue prev_queue;
  private int count;
  private BlockPos pos;
  private int range;
  private CommandSourceStack actor;
  private ServerLevel level;

  public boolean scheduled;
  private boolean stepping;
  private boolean should_end;

  public void printDebugInfo(CommandSourceStack c)
  {
    Messenger.m(c, "w queue: " + queue);
    Messenger.m(c, "w prev_queue: " + prev_queue);
    Messenger.m(c, "w count: " + count);
    Messenger.m(c, "w pos: " + pos);
    Messenger.m(c, "w range: " + range);
  }

  private void step(TickingQueue newQueue, CommandSourceStack c, int newCount, BlockPos newPos, int newRange) {
    queue = newQueue;
    actor = c;
    count = newCount;
    pos = newPos;
    range = newRange;
    scheduled = true;
  }

  @Override
  public void schedule(CommandSourceStack c, TickingQueue newQueue, String modeKey, int count, BlockPos pos, int range, boolean force) throws CommandSyntaxException
  {
    level = c.getLevel();
    newQueue.setMode(modeKey);
    TickPhase phase = new TickPhase(level, newQueue.getPhase());
    if(force ? tickHandler.canStep(0, phase) && !newQueue.cantStep() : tickHandler.canStep(c, 0, phase))
    {
      step(newQueue, c, count, pos, range);
      tickHandler.step(c, 0, phase);
    }
    else if(force && tickHandler.canStep(c, 1, phase))
    {
      step(newQueue, c, count, pos, range);
      tickHandler.step(c, 1, phase);
    }
  }

  @Override
  public void scheduleEnd()
  {
    should_end = true;
  }

  @Override
  public void execute()
  {
    if(!scheduled) return;

    if(!stepping)
    {
      queue.start(level);
      stepping = true;
    }

    // Protects program state when stepping into an update suppressor
    try
    {
      Triple<Integer, Integer, Boolean> triple = queue.step(count, pos, range);
      queue.sendQueueStep(actor, triple.getLeft());
      sendFeedback(triple.getMiddle(), triple.getRight());
    }
    catch(Exception e)
    {
      sendErr(e);
    }

    prev_queue = queue;
    scheduled = false;
  }

  private void sendErr(Exception e) {
    String deobfuscatedStack = Arrays.stream(StackTraceDeobfuscator.deobfuscateStackTrace(e.getStackTrace()))
            .map(StackTraceElement::toString)
            .collect(Collectors.joining("\n"));
    SubTick.LOGGER.error("Crashed while stepping {} {}", queue.getName(), deobfuscatedStack);
    MutableComponent message = (MutableComponent) Messenger.c(Translations.tr("subtick.feedback.queueCommand.err.crash", queue, null)[0]);
    message.withStyle(style -> style
            .withHoverEvent(
                    //#if MC >= 12105
                    //$$ new HoverEvent.ShowText(
                    //#else
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    //#endif
                    Messenger.c("w " +Translations.tr("subtick.hovermessage.tips") +
                            "\n" +
                            deobfuscatedStack)
            ))
            .withClickEvent(
                    //#if MC >= 12105
                    //$$ new ClickEvent.CopyToClipboard(
                    //#else
                    new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,
                            //#endif
                            deobfuscatedStack
                    )
            ));
    actor.sendSuccess(
            //#if MC > 11904
            //$$ () ->
            //#endif
            message, actor.getServer().getLevel(Level.OVERWORLD) != null);
  }

  @Override
  public void end()
  {
    if(!should_end)
      return;

    should_end = false;
    if(!stepping)
      return;

    try {
      prev_queue.step(1, BlockPos.ZERO, -2);
      prev_queue.end();
      prev_queue.exhausted = false;
    } catch (Exception e) {
      sendErr(e);
    }
    tickHandler.advancePhase(level);
    // this clears block event highlights
    ServerNetworkHandler.sendTickStep(level, 0, tickHandler.targetPhase());
    stepping = false;
  }

  @Override
  public void onScheduleBlockEvent(ServerLevel level, BlockEventData be)
  {
    if(stepping && queue instanceof BlockEventQueue beq)
      beq.updateQueue(level, be);
  }

  private void sendFeedback(int steps, boolean exhausted)
  {
    if(steps == 0)
      Translations.m(actor, "queueCommand.err.exhausted", queue);
    else if(steps == 1)
      if(exhausted)
        Translations.m(actor, "queueCommand.success.single.exhausted", queue, steps);
      else
        Translations.m(actor, "queueCommand.success.single", queue, steps);
    else
      if(exhausted)
        Translations.m(actor, "queueCommand.success.multiple.exhausted", queue, steps);
      else
        Translations.m(actor, "queueCommand.success.multiple", queue, steps);
  }
}
