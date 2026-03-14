package subtick.mixins;

import org.spongepowered.asm.mixin.Mixin;
import subtick.DummyClass;

@Mixin(DummyClass.class)
public interface ServerTickRateManagerAccessor {
}
