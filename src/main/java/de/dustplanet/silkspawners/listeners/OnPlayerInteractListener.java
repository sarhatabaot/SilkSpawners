package de.dustplanet.silkspawners.listeners;

import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerChangeEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author sarhatabaot
 */
public class OnPlayerInteractListener extends SilkListener {

    public OnPlayerInteractListener() {
        super();
        setName("OnPlayerInteract");
        verbose();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasItem() || !event.hasBlock()) {
            return;
        }
        ItemStack item = event.getItem();
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        // If we use a spawn egg
        if (item != null && item.getType() == getSilkUtil().nmsProvider.getSpawnEggMaterial()) {
            // Get the entityID
            String entityID = getSilkUtil().getStoredEggEntityID(item);
            // Clicked spawner with monster egg to change type
            if (block != null && block.getType() == getSilkUtil().nmsProvider.getSpawnerMaterial()) {
                Action action = event.getAction();
                if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
                    return;
                }

                if (action != Action.RIGHT_CLICK_BLOCK && getPlugin().config.getBoolean("disableChangeTypeWithEgg", false)) {
                    return;
                }

                if (!getSilkUtil().canBuildHere(player, block.getLocation())) {
                    return;
                }

                // Mob
                String mobName = getSilkUtil().getCreatureName(entityID).toLowerCase().replace(" ", "");

                if (!player.hasPermission("silkspawners.changetypewithegg." + mobName)) {
                    getSilkUtil().sendMessage(player, ChatColor.translateAlternateColorCodes('\u0026',
                            getPlugin().getLocalization().getString("noPermissionChangingWithEggs")));
                    event.setCancelled(true);
                    return;
                }


                // Call the event and maybe change things!
                SilkSpawnersSpawnerChangeEvent changeEvent = new SilkSpawnersSpawnerChangeEvent(player, block, entityID,
                        getSilkUtil().getSpawnerEntityID(block), 1);
                getPlugin().getServer().getPluginManager().callEvent(changeEvent);
                // See if we need to stop
                if (changeEvent.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }
                // Get the new ID (might be changed)
                entityID = changeEvent.getEntityID();

                getSilkUtil().setSpawnerType(block, entityID, player,
                        ChatColor.translateAlternateColorCodes('\u0026', getPlugin().getLocalization().getString("changingDeniedWorldGuard")));
                getSilkUtil().sendMessage(player, ChatColor.translateAlternateColorCodes('\u0026', getPlugin().getLocalization().getString("changedSpawner"))
                        .replace("%creature%", getSilkUtil().getCreatureName(entityID)));

                // Consume egg
                if (getPlugin().config.getBoolean("consumeEgg", true)) {
                    getSilkUtil().nmsProvider.reduceEggs(player);
                    // Prevent normal eggs reducing
                    event.setCancelled(true);
                }
                // Normal spawning
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                if (getPlugin().config.getBoolean("spawnEggToSpawner", false)) {
                    Block targetBlock = block.getRelative(BlockFace.UP);
                    // Check if block above is air
                    if (targetBlock.getType() == Material.AIR) {
                        targetBlock.setType(getSilkUtil().nmsProvider.getSpawnerMaterial());
                        getSilkUtil().setSpawnerEntityID(targetBlock, entityID);
                        // Prevent mob spawning
                        // Should we consume the egg?
                        if (getPlugin().config.getBoolean("consumeEgg", true)) {
                            getSilkUtil().nmsProvider.reduceEggs(player);
                        }
                    } else {
                        getSilkUtil().sendMessage(player,
                                ChatColor.translateAlternateColorCodes('\u0026', getPlugin().getLocalization().getString("noSpawnerHere")));
                    }
                    event.setCancelled(true);
                } else if (getPlugin().config.getBoolean("spawnEggOverride", false)) {
                    boolean allowed = getPlugin().config.getBoolean("spawnEggOverrideSpawnDefault", true);
                    if (entityID != null) {
                        allowed = getPlugin().getMobs().getBoolean("creatures." + entityID + ".enableSpawnEggOverrideAllowSpawn", allowed);
                    }
                    // Deny spawning
                    if (!allowed) {
                        getSilkUtil().sendMessage(player,
                                ChatColor
                                        .translateAlternateColorCodes('\u0026',
                                                getPlugin().getLocalization().getString("spawningDenied").replace("%ID%", entityID))
                                        .replace("%creature%", getSilkUtil().getCreatureName(entityID)));
                        event.setCancelled(true);
                        return;
                    }
                    // Bukkit doesn't allow us to spawn wither or dragons and so
                    // on. NMS here we go!
                    // https://github.com/Bukkit/CraftBukkit/blob/master/src/main/java/net/minecraft/server/ItemMonsterEgg.java#L22

                    // Notify
                    getPlugin().informPlayer(player, getPlugin().getLocalization().getString("spawning").replace("%ID%", entityID)
                            .replace("%creature%", getSilkUtil().getCreatureName(entityID)));

                    // Spawn on top of targeted block
                    Location location = block.getLocation().add(0, 1, 0);
                    double x = location.getX();
                    double y = location.getY();
                    double z = location.getZ();

                    // We can spawn using the direct method from EntityTypes
                    getSilkUtil().nmsProvider.spawnEntity(player.getWorld(), entityID, x, y, z);

                    getSilkUtil().nmsProvider.reduceEggs(player);

                    // Prevent normal spawning
                    event.setCancelled(true);
                }
            }
        }
    }
}
