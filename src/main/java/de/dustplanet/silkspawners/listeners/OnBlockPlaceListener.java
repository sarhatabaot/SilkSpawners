package de.dustplanet.silkspawners.listeners;

import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerPlaceEvent;
import de.dustplanet.silkspawners.util.Common;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handle the placement and breaking of a spawner.
 *
 * @author (former) mushroomhostage
 * @author xGhOsTkiLLeRx
 * @author sarhatabaot
 */
public class OnBlockPlaceListener extends SilkListener {

    public OnBlockPlaceListener() {
        super();
        setName("OnBlockPlaceListener");
        verbose();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        boolean isFakeEvent = !BlockPlaceEvent.class.equals(event.getClass());
        if (isFakeEvent) {
            return;
        }

        Block blockPlaced = event.getBlockPlaced();
        if (blockPlaced.getType() != getSilkUtil().nmsProvider.getSpawnerMaterial()) {
            return;
        }
        Player player = event.getPlayer();
        if (!getSilkUtil().canBuildHere(player, blockPlaced.getLocation())) {
            return;
        }
        ItemStack item = event.getItemInHand();
        String entityID = getSilkUtil().getStoredSpawnerItemEntityID(item);
        boolean defaultID = false;
        if (entityID == null) {
            defaultID = true;
            entityID = getSilkUtil().getDefaultEntityID();
        }

        SilkSpawnersSpawnerPlaceEvent placeEvent = new SilkSpawnersSpawnerPlaceEvent(player, blockPlaced, entityID);
        plugin.getServer().getPluginManager().callEvent(placeEvent);

        if (placeEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        entityID = placeEvent.getEntityID();

        String creatureName = getSilkUtil().getCreatureName(entityID);
        String spawnerName = creatureName.toLowerCase().replace(" ", "");

        if (!player.hasPermission("silkspawners.place." + spawnerName)) {
            event.setCancelled(true);
            Common.tell(player,plugin.getLocalization().getString("noPermissionPlace").replace("%ID%", entityID)
                    .replace("%creature%", creatureName));
            return;
        }

        if (defaultID) {
            plugin.informPlayer(player, Common.colorize(plugin.getLocalization().getString("placingDefault")));
        } else {
            plugin.informPlayer(player, Common.colorize(plugin.getLocalization().getString("spawnerPlaced"))
                    .replace("%creature%", getSilkUtil().getCreatureName(entityID)));
        }

        getSilkUtil().setSpawnerEntityID(blockPlaced, entityID); //TODO: issue #3 here
    }
}
