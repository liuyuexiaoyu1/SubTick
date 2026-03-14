package subtick.mixins.carpet;

import carpet.logging.HUDController;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.ServerTickRateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import subtick.ITickHandleable;
import subtick.mixins.ServerTickRateManagerAccessor;

@Mixin(HUDController.class)
public class HUDControllerMixin {
    @WrapOperation(method = "send_tps_display", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickRateManager;isFrozen()Z"))
    private static boolean isFrozen(ServerTickRateManager instance, Operation<Boolean> original) {
        return original.call(instance) || ((ITickHandleable)(((ServerTickRateManagerAccessor)instance)).getServer()).tickHandler().frozen();
    }
}
