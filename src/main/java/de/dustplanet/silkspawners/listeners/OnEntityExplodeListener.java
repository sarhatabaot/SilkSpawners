package de.dustplanet.silkspawners.listeners;

import de.dustplanet.silkspawners.SilkSpawners;
import de.dustplanet.silkspawners.util.SilkUtil;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Random;

/**
 * Handle the explosion of a spawner.
 *
 * @author (former) mushroomhostage
 * @author xGhOsTkiLLeRx
 */

public class OnEntityExplodeListener extends SilkListener {
    private Random rnd;

    public OnEntityExplodeListener() {
        super();
        this.rnd = new Random();
        setName("OnEntityExplodeListener");
        verbose();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        /*
         * Skip if event is cancelled entity is not known or null EnderDragon calls this event explosionChance is 0
         */
        Entity entity = event.getEntity();
        if (event.isCancelled() || entity instanceof EnderDragon
                || getPlugin().config.getInt("explosionDropChance", 30) == 0) {
            return;
        }

        // Check if a spawner block is on the list
        if (!isDrop(entity)) {
            return;
        }

        for (Block block : event.blockList()) {
            // We have a spawner
            if (block.getType() == getSilkUtil().nmsProvider.getSpawnerMaterial()) {
                // Roll the dice
                int randomNumber = rnd.nextInt(100);
                String entityID = getSilkUtil().getSpawnerEntityID(block);
                // Check if we should drop a block
                if (randomNumber < getExplosionDropChance(entityID)) {
                    World world = block.getWorld();
                    world.dropItemNaturally(block.getLocation(),
                            getSilkUtil().newSpawnerItem(entityID, getSilkUtil().getCustomSpawnerName(entityID), 1, false));
                }
            }
        }

    }

    private int getExplosionDropChance(String entityID){
        if (getPlugin().getMobs().contains("creatures." + entityID + ".explosionDropChance")) {
            return getPlugin().getMobs().getInt("creatures." + entityID + ".explosionDropChance", 100);
        }
        return getPlugin().config.getInt("explosionDropChance", 100);
    }

    private boolean isDrop(Entity entity) {
        if (getPlugin().config.getBoolean("permissionExplode", false) && entity instanceof TNTPrimed) {
            Entity igniter = ((TNTPrimed) entity).getSource();
            if (igniter instanceof Player) {
                Player sourcePlayer = (Player) igniter;
                return sourcePlayer.hasPermission("silkspawners.explodedrop");
            }
        }
        return false;
    }
}
