package de.dustplanet.silkspawners.listeners;

import de.dustplanet.silkspawners.util.Common;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;

/**
 * @author sarhatabaot
 */
public class OnItemCraftListener extends SilkListener {
    public OnItemCraftListener() {
        super();
        setName("OnItemCraftListener");
        verbose();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemCraft(CraftItemEvent event) {
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

        String spawnerName = creatureName.toLowerCase().replace(" ", "");
        if (!player.hasPermission("silkspawners.craft." + spawnerName)) {
            event.setCancelled(true);
            Common.tell(player,getPlugin().getLocalization().getString("noPermissionCraft").replace("%ID%", entityID)
                    .replace("%creature%", spawnerName));
        }
    }

}
