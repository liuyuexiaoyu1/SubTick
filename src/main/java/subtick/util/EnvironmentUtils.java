package subtick.util;


//#if MC >= 11802
//$$ import net.minecraft.WorldVersion;
//#else
import com.mojang.bridge.game.GameVersion;
//#endif
import net.minecraft.SharedConstants;

/**
 * Copy from Carpet-TIS-Addition
 * 此文件以LGPL-3.0协议开源
 */

public class EnvironmentUtils {
    //#if MC >= 11802
    //$$ public static WorldVersion getMinecraftVersion()
    //#else
    public static GameVersion getMinecraftVersion()
    //#endif
    {
        return SharedConstants.getCurrentVersion();
    }

    public static String getMinecraftVersionId()
    {
        //#if MC >= 12108
        //$$ return getMinecraftVersion().id();
        //#else
        return getMinecraftVersion().getId();
        //#endif
    }

    @SuppressWarnings("unused")
    public static String getMinecraftVersionName()
    {
        //#if MC >= 12108
        //$$ return getMinecraftVersion().name();
        //#else
        return getMinecraftVersion().getName();
        //#endif
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
