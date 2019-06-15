package de.dustplanet.silkspawners;

import de.dustplanet.silkspawners.commands.SilkSpawnersTabCompleter;
import de.dustplanet.silkspawners.commands.SpawnerCommand;
import de.dustplanet.silkspawners.configs.Config;
import de.dustplanet.silkspawners.configs.Localization;
import de.dustplanet.silkspawners.configs.Mobs;
import de.dustplanet.silkspawners.listeners.*;
import de.dustplanet.silkspawners.configs.CommentedConfiguration;
import de.dustplanet.silkspawners.util.Common;
import de.dustplanet.silkspawners.util.SilkUtil;
import lombok.Getter;
import lombok.Setter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * General stuff.
 *
 * @author (former) mushroomhostage
 * @author xGhOsTkiLLeRx
 */

public class SilkSpawners extends JavaPlugin {
    @Getter
    private SilkUtil silkUtil;
    @Getter
    @Setter
    private String nmsVersion;
    public CommentedConfiguration config;
    @Getter
    @Setter
    private CommentedConfiguration localization;

    @Getter
    private CommentedConfiguration mobs;

    @Getter
    @Setter
    private static SilkSpawners instance;

    @Override
    public void onDisable() {
        if (silkUtil != null) {
            silkUtil.clearAll();
        }
        setInstance(null);
    }

    @Override
    public void onEnable() {
        setInstance(this);
        // Make files and copy defaults
        initializeConfigs();

        // Get full package string of CraftServer
        String packageName = getServer().getClass().getPackage().getName();
        // org.bukkit.craftbukkit.version
        // Get the last element of the package
        setNmsVersion(packageName.substring(packageName.lastIndexOf('.') + 1));

        // Heart of SilkSpawners is the SilkUtil class which holds all of our
        // important methods
        silkUtil = new SilkUtil(this);

        loadPermissions();
        loadConfig();

        // Commands
        Common.registerCommand(new SpawnerCommand());
        //getCommand("silkspawners").setTabCompleter(new SilkSpawnersTabCompleter()); TODO

        // Listeners
        registerListeners();

        new Metrics(this);

        // BarAPI check
        hookBarApi();

    }

    private void hookBarApi() {
        if (config.getBoolean("barAPI.enable", false)) {
            Plugin barAPI = getServer().getPluginManager().getPlugin("BarAPI");
            if (barAPI != null) {
                // If BarAPI is enabled, load the economy
                getLogger().info("Loaded BarAPI successfully!");
                silkUtil.setBarAPI(true);
            } else {
                // Else tell the admin about the missing of BarAPI
                getLogger().info("BarAPI was not found and remains disabled!");
            }
        } else {
            getLogger().info("BarAPI is disabled due to config setting.");
        }
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new OnBlockBreakListener(),this);
        pm.registerEvents(new OnBlockPlaceListener(),this);
        pm.registerEvents(new OnInventoryClickListener(),this);
        pm.registerEvents(new OnPlayerInteractListener(), this);
        pm.registerEvents(new OnPlayerHoldItemListener(),this);
        pm.registerEvents(new OnItemCraftListener(), this);
        pm.registerEvents(new OnEntityExplodeListener(), this);
        pm.registerEvents(new OnPrepareItemCraftListener(), this);
        getLogger().info("Registered listeners.");
    }

    // If no config is found, copy the default one(s)!

    private void copy(String yml, File file) {
        try (OutputStream out = new FileOutputStream(file); InputStream in = getResource(yml)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            getLogger().warning("Failed to copy the default config! (I/O)");
            e.printStackTrace();
        }
    }

    private void loadPermissions() {
        loadPermissions("craft", "Allows you to craft the specific spawner", PermissionDefault.FALSE);
        loadPermissions("place", "Allows you to place the specific spawner", PermissionDefault.FALSE);
        loadPermissions("silkdrop", "Allows you to use silk touch to acquire mob spawner items", PermissionDefault.FALSE);
        loadPermissions("destroydrop", "Allows you to destroy mob spawners to acquire mob spawn eggs / iron bars / XP (as configured)",
                PermissionDefault.FALSE);
        loadPermissions("changetype", "Allows you to change the spawner type using /spawner [creature]", PermissionDefault.FALSE);
        loadPermissions("changetypewithegg", "Allows you to change the spawner type by left-clicking with a spawn egg",
                PermissionDefault.FALSE);
        loadPermissions("freeitem", "Allows you to get spawner items in your hand for free using /spawner [creature]",
                PermissionDefault.FALSE);
        loadPermissions("freeitemegg", "Allows you to get spawn eggs in your hand for free using /spawner [creature]egg",
                PermissionDefault.FALSE);
    }

    private void loadPermissions(String permissionPart, String description, PermissionDefault permDefault) {
        HashMap<String, Boolean> childPermissions = new HashMap<>();
        for (String mobAlias : silkUtil.getDisplayNameToMobID().keySet()) {
            mobAlias = mobAlias.toLowerCase().replace(" ", "");
            childPermissions.put("silkspawners." + permissionPart + "." + mobAlias, true);
        }
        Permission perm = new Permission("silkspawners." + permissionPart + ".*", description, permDefault, childPermissions);
        try {
            getServer().getPluginManager().addPermission(perm);
        } catch (IllegalArgumentException e) {
            getLogger().info("Permission " + perm.getName() + " is already registered. Skipping...");
        }
    }

    private void initializeConfigs() {
        // Config
        File configFile = new File(getDataFolder(), "config.yml");
        // One file and the folder not existent
        if (!configFile.exists() && !getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("The config folder could NOT be created, make sure it's writable!");
            getLogger().severe("Disabling now!");
            shutdown();
            return;
        }
        // Copy default is necessary
        if (!configFile.exists()) {
            copy("config.yml", configFile);
        }

        // Localization
        File localizationFile = new File(getDataFolder(), "localization.yml");
        if (!localizationFile.exists()) {
            copy("localization.yml", localizationFile);
        }

        // Mobs
        File mobsFile = new File(getDataFolder(), "mobs.yml");
        if (!mobsFile.exists()) {
            copy("mobs.yml", mobsFile);
        }

        // Load configs
        config = new CommentedConfiguration(configFile);
        new Config(config).loadConfig();

        setLocalization(new CommentedConfiguration(localizationFile));
        new Localization(getLocalization()).loadConfig();

        mobs = new CommentedConfiguration(mobsFile);
        new Mobs(mobs).loadConfig();

        migrateConfig();
    }

    private void migrateConfig() {
        if (config.contains("creatures")) {
            getLogger().info("Found entries of creatures in the config.yml, will migrate them into the mobs.yml!");
            ConfigurationSection creatures = config.getConfigurationSection("creatures");
            // Remove from config and save
            config.set("creatures", null);
            config.save();
            // Set updated list
            mobs.set("creatures", creatures);
            mobs.save();
            getLogger().info("Successfully migrated the creatures into the mobs.yml!");
        }
    }

    private void loadConfig() {
        // Enable craftable spawners?
        if (config.getBoolean("craftableSpawners", false)) {
            loadRecipes();
        }
    }

    public static void verbose(String message){
        if(SilkSpawners.getInstance().config.getBoolean("verboseConfig",false)){
            getInstance().getLogger().info("VERBOSE "+message);
        }
    }

    public static void verbose(String... messages){
        if(SilkSpawners.getInstance().config.getBoolean("verboseConfig",false)){
            for(String message: messages)
                getInstance().getLogger().info("VERBOSE "+ message);
        }
    }

    // Add the recipes
    private void loadRecipes() {
        verbose("Loading custom recipes");

        // Add "base" recipe for eggs containing no durability (not from SilkSpawners)
        // 1.9 deprecated the durability and uses NBT tags
        String baseSpawnerEntityID = silkUtil.getDefaultEntityID();
        int baseSpawnerAmount = config.getInt("recipeAmount", 1);
        ItemStack baseSpawnerItem = silkUtil.newSpawnerItem(baseSpawnerEntityID, "&e&o??? &r&fSpawner", baseSpawnerAmount, false);
        ShapedRecipe baseSpawnerRecipe = null;
        try {
            baseSpawnerRecipe = new ShapedRecipe(new NamespacedKey(this, "baseSpawner"), baseSpawnerItem);
        } catch (Exception | Error e) {
            // Legacy
            baseSpawnerRecipe = new ShapedRecipe(baseSpawnerItem);
        }

        String baseSpawnerTop = config.getString("recipeTop", "AAA");
        String baseSpawnerMiddle = config.getString("recipeMiddle", "AXA");
        String baseSpawnerBottom = config.getString("recipeBottom", "AAA");

        // Set the shape
        baseSpawnerRecipe.shape(baseSpawnerTop, baseSpawnerMiddle, baseSpawnerBottom);

        List<String> baseSpawnerIngredientsList = config.getStringList("ingredients");

        // Security first
        if (baseSpawnerIngredientsList != null && !baseSpawnerIngredientsList.isEmpty()) {
            try {
                List<String> baseSpawnerShape = Arrays.asList(baseSpawnerRecipe.getShape());
                // We have an ingredient that is not in our shape. Ignore it then
                if (shapeContainsIngredient(baseSpawnerShape, 'X')) {
                    // Use the right egg!
                    baseSpawnerRecipe.setIngredient('X', silkUtil.nmsProvider.getSpawnEggMaterial());
                }

                for (String ingredient : baseSpawnerIngredientsList) {
                    // They are added like this A,DIRT
                    // Lets split the "," then
                    String[] ingredients = ingredient.split(",");
                    // if our array is not exactly of the size 2, something is wrong
                    if (ingredients.length != 2) {
                        getLogger().info("ingredient length of default invalid: " + ingredients.length);
                        continue;
                    }
                    // Maybe they put a string in here, so first position and uppercase
                    char character = ingredients[0].toUpperCase().charAt(0);
                    // We have an ingredient that is not in our shape. Ignore it then
                    if (!shapeContainsIngredient(baseSpawnerShape, character)) {
                        getLogger().info("shape of default does not contain " + character);
                        continue;
                    }
                    // We try to get the material (ID or name)
                    Material material = Material.matchMaterial(ingredients[1]);
                    // Failed!
                    if (material == null) {
                        getLogger().info("shape material " + ingredients[1] + " of default spawner matched null");
                        material = silkUtil.nmsProvider.getIronFenceMaterial();
                    }
                    baseSpawnerRecipe.setIngredient(character, material);
                }
            } catch (IllegalArgumentException e) {
                // If the custom recipe fails, we have a fallback
                getLogger().warning("Could not add the default recipe!");
                e.printStackTrace();
                baseSpawnerRecipe.shape("AAA", "ABA", "AAA");
                baseSpawnerRecipe.setIngredient('A', silkUtil.nmsProvider.getIronFenceMaterial());
                // Use the right egg!
                baseSpawnerRecipe.setIngredient('B', silkUtil.nmsProvider.getSpawnEggMaterial());
            } finally {
                // Add it
                getServer().addRecipe(baseSpawnerRecipe);
            }
        }

        // For all our entities
        for (String entityID : silkUtil.getMobIDToDisplayName().keySet()) {

            // If the mob is disabled, skip it
            if (!mobs.getBoolean("creatures." + entityID + ".enableCraftingSpawner", true)) {
                verbose("Skipping crafting recipe for " + entityID + " per config");
                continue;
            }

            // Debug output
            verbose("Amount of " + entityID + ": " + getPerMobAmount(entityID));

            // Output is a spawner of this type with a custom amount
            ItemStack spawnerItem = silkUtil.newSpawnerItem(entityID, silkUtil.getCustomSpawnerName(entityID), getPerMobAmount(entityID), true);
            ShapedRecipe recipe = null;
            try {
                recipe = new ShapedRecipe(new NamespacedKey(this, entityID), spawnerItem);
            } catch (Exception e) {
                recipe = new ShapedRecipe(spawnerItem);
            }

            /*
             * Default is
             * A A A
             * A B A
             * A A A
             * where A is IRON_FENCE and B is MONSTER_EGG
             */

            // We try to use the custom recipe, but we don't know if the user
            // changed it right ;)
            try {
                // Per type recipe?
                String top = getTopRecipe(entityID);
                String middle = getMiddleRecipe(entityID);
                String bottom = getBottomRecipe(entityID);

                // Debug output
                verbose("Shape of " + entityID + ":",top,middle,bottom);

                // Set the shape
                recipe.shape(top, middle, bottom);

                // Per type ingredients?
                List<String> ingredientsList;
                if (mobs.contains("creatures." + entityID + ".recipe.ingredients")) {
                    ingredientsList = mobs.getStringList("creatures." + entityID + ".recipe.ingredients");
                } else {
                    // No list what we should use -> not adding
                    if (!config.contains("ingredients")) {
                        continue;
                    }
                    ingredientsList = config.getStringList("ingredients");
                }

                // Security first
                if (ingredientsList.isEmpty()) {
                    continue;
                }

                // Debug output
                verbose(String.format("Ingredients of %s:",entityID), ingredientsList.toString());

                List<String> shape = Arrays.asList(recipe.getShape());
                // We have an ingredient that is not in our shape. Ignore it then
                if (shapeContainsIngredient(shape, 'X')) {
                    verbose("shape of " + entityID + " contains X");

                    // Use the right egg!
                    // TODO
                    recipe.setIngredient('X', silkUtil.nmsProvider.getSpawnEggMaterial(), 0);
                }
                for (String ingredient : ingredientsList) {
                    // They are added like this A,DIRT
                    // Lets split the "," then
                    String[] ingredients = ingredient.split(",");
                    // if our array is not exactly of the size 2, something is wrong
                    if (ingredients.length != 2) {
                        getLogger().info("ingredient length of " + entityID + " invalid: " + ingredients.length);
                        continue;
                    }
                    // Maybe they put a string in here, so first position and uppercase
                    char character = ingredients[0].toUpperCase().charAt(0);
                    // We have an ingredient that is not in our shape. Ignore it then
                    if (!shapeContainsIngredient(shape, character)) {
                        getLogger().info("shape of " + entityID + " does not contain " + character);
                        continue;
                    }
                    // We try to get the material (ID or name)
                    Material material = Material.matchMaterial(ingredients[1]);
                    // Failed!
                    if (material == null) {
                        getLogger().info("shape material " + ingredients[1] + " of " + entityID + " matched null");
                        material = silkUtil.nmsProvider.getIronFenceMaterial();
                    }
                    recipe.setIngredient(character, material);
                }
            } catch (IllegalArgumentException e) {
                // If the custom recipe fails, we have a fallback
                getLogger().warning("Could not add the recipe of " + entityID + "!");
                e.printStackTrace();
                recipe.shape("AAA", "ABA", "AAA");
                recipe.setIngredient('A', silkUtil.nmsProvider.getIronFenceMaterial());
                // Use the right egg!
                // TODO
                recipe.setIngredient('B', silkUtil.nmsProvider.getSpawnEggMaterial(), 0);
            } finally {
                // Add it
                getServer().addRecipe(recipe);
            }
        }
    }

    private boolean shapeContainsIngredient(List<String> shape, char c) {
        boolean match = false;
        for (String recipePart : shape) {
            for (char recipeIngredient : recipePart.toCharArray()) {
                if (recipeIngredient == c) {
                    match = true;
                    break;
                }
            }
            if (match) {
                break;
            }
        }
        return match;
    }

    // If the user has the permission, message
    public void informPlayer(Player player, String message) {
        // Ignore empty messages
        if (message == null || message.isEmpty()) {
            return;
        }
        if (player.hasPermission("silkspawners.info")) {
            Common.tell(player, message);
        }
    }

    private String getTopRecipe(String entityID) {
        if (mobs.contains("creatures." + entityID + ".recipe.top")) {
            return mobs.getString("creatures." + entityID + ".recipe.top", "AAA");
        }
        return config.getString("recipeTop", "AAA");
    }

    private String getMiddleRecipe(String entityID){
        if (mobs.contains("creatures." + entityID + ".recipe.middle")) {
            return  mobs.getString("creatures." + entityID + ".recipe.middle", "AXA");
        }
        return config.getString("recipeMiddle", "AXA");
    }

    private String getBottomRecipe(String entityID){
        if (mobs.contains("creatures." + entityID + ".recipe.bottom")) {
            return mobs.getString("creatures." + entityID + ".recipe.bottom", "AAA");
        }
        return config.getString("recipeBottom", "AAA");

    }

    private int getPerMobAmount(String entityID){
        if (mobs.contains("creatures." + entityID + ".recipe.amount")) {
            return mobs.getInt("creatures." + entityID + ".recipe.amount", 1);
        }
        return config.getInt("recipeAmount", 1);
    }

    public void reloadConfigs() {
        config.load();
        config.save();
        loadConfig();
        silkUtil.load();
        mobs.load();
        mobs.save();
        getLocalization().load();
        getLocalization().save();
    }

    public void shutdown() {
        setEnabled(false);
    }


}
