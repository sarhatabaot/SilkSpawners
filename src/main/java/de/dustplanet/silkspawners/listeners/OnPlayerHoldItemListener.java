package de.dustplanet.silkspawners.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemHeldEvent;

/**
 * @author sarhatabaot
 */
public class OnPlayerHoldItemListener extends SilkListener {

    public OnPlayerHoldItemListener() {
        super();
        setName("OnPlayerHoldItem");
        verbose();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerHoldItem(PlayerItemHeldEvent event) {
        // Check if we should notify the player. The second condition is the
        // permission and that the slot isn't null and the item is a mob spawner
        if (event.getPlayer().getInventory().getItem(event.getNewSlot()) != null
                && event.getPlayer().getInventory().getItem(event.getNewSlot()).getType() == getSilkUtil().nmsProvider.getSpawnerMaterial()
                && getPlugin().config.getBoolean("notifyOnHold") && event.getPlayer().hasPermission("silkspawners.info")) {

            // Get ID
            String entityID = getSilkUtil().getStoredSpawnerItemEntityID(event.getPlayer().getInventory().getItem(event.getNewSlot()));
            // Check for unknown/invalid ID
            if (entityID == null) {
                entityID = getSilkUtil().getDefaultEntityID();
            }
            // Get the name from the entityID
            String spawnerName = getSilkUtil().getCreatureName(entityID);
            Player player = event.getPlayer();
            getSilkUtil().notify(player, spawnerName);
        }
    }
}
