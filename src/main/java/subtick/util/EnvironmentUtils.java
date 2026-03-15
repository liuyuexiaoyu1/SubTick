package subtick.util;

import com.mojang.bridge.game.GameVersion;
import net.minecraft.SharedConstants;

/**
 * Copy from Carpet-TIS-Addition
 * 此文件以LGPL-3.0协议开源
 */

public class EnvironmentUtils {
    public static GameVersion getMinecraftVersion()
    {
        return SharedConstants.getCurrentVersion();
    }

    public static String getMinecraftVersionId()
    {
        return getMinecraftVersion().getId();
    }

    @SuppressWarnings("unused")
    public static String getMinecraftVersionName()
    {
        return getMinecraftVersion().getName();
    }

    @SuppressWarnings("unused")
    public static boolean isMinecraftUnobfuscated()
    {
        //#if MC >= 26.1
        //$$ return true;
        //#else
        return getMinecraftVersionId().endsWith("_unobfuscated");
        //#endif
    }

    @SuppressWarnings("unused")
    public static boolean isMinecraftObfuscated()
    {
        return !isMinecraftUnobfuscated();
    }
}
