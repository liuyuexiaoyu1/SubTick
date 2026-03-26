package subtick.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import subtick.client.ClientTickHandler;

import java.util.function.Consumer;

@Mixin(ClientLevel.class)
public class ClientLevelMixin
{
  @Inject(method = "tick", at = @At("TAIL"))
  private void onTick(CallbackInfo ci)
  {
    ClientTickHandler.onTick((ClientLevel)(Object)this);
  }

  //#if MC < 12110
  @WrapWithCondition(method = "tickEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;tickBlockEntities()V"))
  private boolean tickBlockEntities(ClientLevel level)
  {
    return !ClientTickHandler.skip_block_entities && ClientTickHandler.shouldTick();
  }
  //#endif

  //#if MC >= 26.1
  //$$ @WrapWithCondition(method = "lambda$tickEntities$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;guardEntityTick(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/Entity;)V"))
  //#else
  @WrapWithCondition(method = "method_32124", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;guardEntityTick(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/Entity;)V"))
  //#endif
  private boolean tickNonPassenger(ClientLevel instance, Consumer<?> consumer, Entity entity)
  {
    return ClientTickHandler.shouldTick() || canTick(entity);
  }

  @Unique
  private boolean canTick(Entity entity) {
    if (entity instanceof Player) return true;
    return entity.getPassengers().stream().flatMap(Entity::getSelfAndPassengers).anyMatch(entity1 -> entity1 instanceof Player);
  }
}
