package subtick.mixins;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerTickRateManager.class)
public interface ServerTickRateManagerAccessor {
    @Accessor("server")
    MinecraftServer getServer();
}
