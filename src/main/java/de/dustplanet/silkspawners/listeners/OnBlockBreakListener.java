package de.dustplanet.silkspawners.listeners;

import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerBreakEvent;
import de.dustplanet.silkspawners.util.Common;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Random;


/**
 * Handle the placement and breaking of a spawner.
 *
 * @author (former) mushroomhostage
 * @author xGhOsTkiLLeRx
 * @author sarhatabaot
 */
public class OnBlockBreakListener extends SilkListener {
    private Random rnd;

    public OnBlockBreakListener() {
        super();
        setName("OnBlockBreakListener");
        verbose();
        this.rnd = new Random();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        boolean isFakeEvent = !BlockBreakEvent.class.equals(event.getClass());
        if (isFakeEvent) {
            return;
        }

        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (block.getType() != getSilkUtil().nmsProvider.getSpawnerMaterial()) {
            return;
        }
        if (!getSilkUtil().canBuildHere(player, block.getLocation())) {
            return;
        }
        String entityID = getSilkUtil().getSpawnerEntityID(block);

        SilkSpawnersSpawnerBreakEvent breakEvent = new SilkSpawnersSpawnerBreakEvent(player, block, entityID);
        plugin.getServer().getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        entityID = getSilkUtil().getDisplayNameToMobID().get(breakEvent.getEntityID());

        plugin.informPlayer(player, Common.colorize(plugin.getLocalization().getString("spawnerBroken"))
                .replace("%creature%", getSilkUtil().getCreatureName(entityID)));

        ItemStack tool = getSilkUtil().nmsProvider.getItemInHand(player);
        boolean validToolAndSilkTouch = getSilkUtil().isValidItemAndHasSilkTouch(tool);

        World world = player.getWorld();

        String mobName = getSilkUtil().getCreatureName(entityID).toLowerCase().replace(" ", "");

        if (plugin.config.getBoolean("noDropsCreative", true) && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        event.setExpToDrop(0);
        boolean mined = false;
        boolean dropXPOnlyOnDestroy = plugin.config.getBoolean("dropXPOnlyOnDestroy", false);

        if (plugin.config.getBoolean("preventXPFarming", true) && block.hasMetadata("mined")) {
            mined = block.getMetadata("mined").get(0).asBoolean();
        }

        if (player.hasPermission("silkspawners.silkdrop." + mobName) || player.hasPermission("silkspawners.destroydrop." + mobName)) {
            int addXP = plugin.config.getInt("destroyDropXP");
            // If we have more than 0 XP, drop them
            // either we drop XP for destroy and silktouch or only when
            // destroyed and we have no silktouch
            if (!mined && addXP != 0 && (!dropXPOnlyOnDestroy || !validToolAndSilkTouch)) {
                event.setExpToDrop(addXP);
                // check if we should flag spawners
                if (plugin.config.getBoolean("preventXPFarming", true)) {
                    block.setMetadata("mined", new FixedMetadataValue(plugin, true));
                }
            }
        }

        int randomNumber = rnd.nextInt(100);

        if (validToolAndSilkTouch && player.hasPermission("silkspawners.silkdrop." + mobName)) {
            if (randomNumber < getSilkDropChance(entityID)) {
                ItemStack breakEventDrop = breakEvent.getDrop();
                ItemStack spawnerItemStack = null;
                if (breakEventDrop != null) {
                    spawnerItemStack = breakEventDrop;
                } else {
                    spawnerItemStack = getSilkUtil().newSpawnerItem(entityID, getSilkUtil().getCustomSpawnerName(entityID), getDropAmount(entityID), false);
                }
                if (spawnerItemStack == null) {
                    plugin.getLogger().warning("Skipping dropping of spawner, since item is null");
                    return;
                }
                if (plugin.getConfig().getBoolean("dropSpawnerToInventory", false)) {
                    HashMap<Integer, ItemStack> additionalItems = player.getInventory().addItem(spawnerItemStack);
                    if (!additionalItems.isEmpty()) {
                        for (ItemStack itemStack : additionalItems.values()) {
                            world.dropItemNaturally(block.getLocation(), itemStack);
                        }
                    }
                } else {
                    world.dropItemNaturally(block.getLocation(), spawnerItemStack);
                }
            }
            return;
        }

        if (player.hasPermission("silkspawners.destroydrop." + mobName)) {
            if (plugin.config.getBoolean("destroyDropEgg", false)) {
                randomNumber = rnd.nextInt(100);
                if (randomNumber < getEggDropChance(entityID)) {
                    world.dropItemNaturally(block.getLocation(), getSilkUtil().newEggItem(entityID, 1));
                }
            }

            int dropBars = plugin.config.getInt("destroyDropBars", 0);
            if (dropBars != 0) {
                randomNumber = rnd.nextInt(100);
                if (randomNumber < getDestroyDropChance(entityID)) {
                    world.dropItem(block.getLocation(), new ItemStack(getSilkUtil().nmsProvider.getIronFenceMaterial(), dropBars));
                }
            }
        }
    }

    private int getDropAmount(String entityID){
        if (plugin.getMobs().contains("creatures." + entityID + ".dropAmount")) {
            return plugin.getMobs().getInt("creatures." + entityID + ".dropAmount", 1);
        }
        return plugin.config.getInt("dropAmount", 1);

    }

    private int getSilkDropChance(String entityID){
        if (plugin.getMobs().contains("creatures." + entityID + ".silkDropChance")) {
            return plugin.getMobs().getInt("creatures." + entityID + ".silkDropChance", 100);
        }
        return plugin.config.getInt("silkDropChance", 100);
    }

    private int getEggDropChance(String entityID){
        if (plugin.getMobs().contains("creatures." + entityID + ".eggDropChance")) {
            return plugin.getMobs().getInt("creatures." + entityID + ".eggDropChance", 100);
        }
        return plugin.config.getInt("eggDropChance", 100);

    }

    private int getDestroyDropChance(String entityID){
        if (plugin.getMobs().contains("creatures." + entityID + ".destroyDropChance")) {
            return plugin.getMobs().getInt("creatures." + entityID + ".destroyDropChance", 100);
        }
        return plugin.config.getInt("destroyDropChance", 100);

    }
}
