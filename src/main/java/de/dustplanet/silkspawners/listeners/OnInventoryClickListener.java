package de.dustplanet.silkspawners.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * @author sarhatabaot
 */
public class OnInventoryClickListener extends SilkListener {
    public OnInventoryClickListener() {
        super();
        setName("OnInventoryClickListener");
        verbose();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event == null || event.getCurrentItem() == null || event.getWhoClicked() == null) {
            return;
        }

        if (event.getCurrentItem().getType() != getSilkUtil().nmsProvider.getSpawnerMaterial()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        String entityID = getSilkUtil().getStoredSpawnerItemEntityID(event.getCurrentItem());

        if (entityID == null) {
            entityID = getSilkUtil().getDefaultEntityID();
        }
        String creatureName = getSilkUtil().getCreatureName(entityID);

        if (getPlugin().config.getBoolean("notifyOnClick") && player.hasPermission("silkspawners.info")) {
            getSilkUtil().notify(player, creatureName);
        }
    }
}
