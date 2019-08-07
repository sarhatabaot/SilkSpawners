package de.dustplanet.silkspawners.configs;

import de.dustplanet.silkspawners.SilkSpawners;

import java.util.List;

/**
 * @author sarhatabaot
 */
public class Config {
    private Config() {
        throw new IllegalStateException("Config class");
    }

    public static int getInt(String path){
        return SilkSpawners.getInstance().getConfig().getInt(path);
    }

    public static boolean getBoolean(String path){
        return SilkSpawners.getInstance().getConfig().getBoolean(path);
    }

    public static String getString(String path){
        return SilkSpawners.getInstance().getConfig().getString(path);
    }

    public static List<String> getStringList(String path){
        return SilkSpawners.getInstance().getConfig().getStringList(path);
    }
}
