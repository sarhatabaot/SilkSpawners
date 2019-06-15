package de.dustplanet.silkspawners.commands;

import de.dustplanet.silkspawners.SilkSpawners;
import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerChangeEvent;
import de.dustplanet.silkspawners.util.Common;
import de.dustplanet.silkspawners.util.SilkUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Handles the commands.
 *
 * @author xGhOsTkiLLeRx
 */

public class SpawnerCommand extends PlayerCommand {
    private SilkUtil su;
    private SilkSpawners plugin;

    public SpawnerCommand() {
        super("silkspawners");

        setPrefix("[&2Silk&aSpawners&f]");
        setDescription("Command for changing and getting spawners or spawn eggs");
        setUsage("/<command> help");
        setPermission("silkspawners.help");
        setAliases(Arrays.asList("ss", "spawner", "silk", "spawnersilk", "egg", "eg", "eggs"));

        plugin = SilkSpawners.getInstance();
        su = plugin.getSilkUtil();
    }

    @Override
    public void run(final @NotNull Player player, @NotNull final String[] args) {
        switch (args.length) {
            case 1:
                switch (args[0].toLowerCase()) {
                    case "help":
                        handleHelp();
                        break;
                    case "all":
                    case "list":
                        handleList(player);
                        break;
                    case "reload":
                    case "rl":
                        handleReload(player);
                        break;
                    case "view":
                        handleView(player);
                        break;
                    default:
                        handleUnknownArgument(player);
                        break;
                }
                break;
            case 2:
                switch (args[0].toLowerCase()) {
                    case "change":
                    case "set":
                        handleChange(player, args[1]);
                        break;
                    default:
                        handleUnknownArgument(player);
                        break;
                }
                break;
            case 3:
                switch (args[0].toLowerCase()) {
                    case "give":
                    case "add":
                        handleGive(player, args[1], args[2].toLowerCase(), null);
                        break;
                    default:
                        handleUnknownArgument(player);
                        break;
                }
                break;
            case 4:
                switch (args[0].toLowerCase()) {
                    case "give":
                    case "add":
                        handleGive(player, args[1], args[2].toLowerCase(), args[3]);
                        break;
                    default:
                        handleUnknownArgument(player);
                        break;
                }
                break;
            default:
                handleUnknownArgument(player);
                break;
        }
    }


    private void handleGive(CommandSender sender, String receiver, String mob, String amountString) {
        int amount = plugin.config.getInt("defaultAmountGive", 1);

        // Check given amount
        if (amountString != null && !amountString.isEmpty()) {
            amount = su.getNumber(amountString);
            if (amount == -1) {
                tell(plugin.getLocalization().getString("useNumbers"));
                return;
            }
        }

        // Check player
        Player player = su.nmsProvider.getPlayer(receiver);
        // Online check
        if (player == null) {
            Common.tell(sender,plugin.getLocalization().getString("playerOffline"));
            return;
        }

        // Check if it's an egg or not
        boolean isEgg = su.isEgg(mob);
        String egg = mob;
        if (isEgg) {
            egg = egg.replaceFirst("egg$", "");
        }

        if (isEgg) {
            handleGiveEgg(sender, player, egg, amount);
        } else {
            handleGiveSpawner(sender, player, mob, amount);
        }
    }

    private void handleGiveEgg(@NotNull CommandSender sender, Player receiver, String mob, int amount) {
        String entityID = su.getDisplayNameToMobID().get(mob);
        String creature = su.getCreatureName(entityID);
        String mobName = creature.toLowerCase().replace(" ", "");
        if (!sender.hasPermission("silkspawners.freeitemegg." + mobName)) {
            tell(plugin.getLocalization().getString("noPermissionFreeEgg"));
            return;
        }
        // Add egg

        // Have space in inventory
        if (receiver.getInventory().firstEmpty() == -1) {
            tell(plugin.getLocalization().getString("noFreeSlot"));
            return;
        }
        receiver.getInventory().addItem(su.newEggItem(entityID, amount));
        //TODO: Force sender to be a player, classic player command.
        if (sender instanceof Player) {
            Player pSender = (Player) sender;
            if (pSender.getUniqueId() == receiver.getUniqueId()) {
                su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.getLocalization().getString("addedEgg"))
                        .replace("%creature%", creature).replace("%amount%", Integer.toString(amount)));
            } else {
                su.sendMessage(sender,
                        ChatColor
                                .translateAlternateColorCodes('\u0026',
                                        plugin.getLocalization().getString("addedEggOtherPlayer").replace("%player%", receiver.getName()))
                                .replace("%creature%", creature).replace("%amount%", Integer.toString(amount)));
            }
        } else {
            su.sendMessage(sender,
                    ChatColor
                            .translateAlternateColorCodes('\u0026',
                                    plugin.getLocalization().getString("addedEggOtherPlayer").replace("%player%", receiver.getName()))
                            .replace("%creature%", creature).replace("%amount%", Integer.toString(amount)));
        }

    }

    private void handleGiveSpawner(CommandSender sender, Player receiver, String mob, int amount) {
        if (su.isUnkown(mob)) {
            su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.getLocalization().getString("unknownCreature"))
                    .replace("%creature%", mob));
            return;
        }

        String entityID = su.getDisplayNameToMobID().get(mob);
        String creature = su.getCreatureName(entityID);
        // Filter spaces (like Zombie Pigman)
        String mobName = creature.toLowerCase().replace(" ", "");

        // Add spawner
        if (sender.hasPermission("silkspawners.freeitem." + mobName)) {
            // Have space in inventory
            if (receiver.getInventory().firstEmpty() == -1) {
                su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.getLocalization().getString("noFreeSlot")));
                return;
            }
            receiver.getInventory().addItem(su.newSpawnerItem(entityID, su.getCustomSpawnerName(entityID), amount, false));
            if (sender instanceof Player) {
                Player pSender = (Player) sender;
                if (pSender.getUniqueId() == receiver.getUniqueId()) {
                    tell(plugin.getLocalization().getString("addedSpawner").replace("%creature%", creature).replace("%amount%", Integer.toString(amount)));
                } else {
                    su.sendMessage(sender,
                            ChatColor
                                    .translateAlternateColorCodes('\u0026',
                                            plugin.getLocalization().getString("addedSpawnerOtherPlayer").replace("%player%",
                                                    receiver.getName()))
                                    .replace("%creature%", creature).replace("%amount%", Integer.toString(amount)));
                }
            } else {
                su.sendMessage(sender,
                        ChatColor
                                .translateAlternateColorCodes('\u0026',
                                        plugin.getLocalization().getString("addedSpawnerOtherPlayer").replace("%player%", receiver.getName()))
                                .replace("%creature%", creature).replace("%amount%", Integer.toString(amount)));
            }
            return;
        }
        su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.getLocalization().getString("noPermissionFreeSpawner")));

    }

    private void handleChange(CommandSender sender, String newMob) {

        if (sender instanceof Player) {
            if (su.isUnkown(newMob)) {
                su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.getLocalization().getString("unknownCreature"))
                        .replace("%creature%", newMob));
                return;
            }

            String entityID = su.getDisplayNameToMobID().get(newMob);
            String creature = su.getCreatureName(entityID);
            // Filter spaces (like Zombie Pigman)
            String mobName = creature.toLowerCase().replace(" ", "");

            Player player = (Player) sender;

            int distance = plugin.config.getInt("spawnerCommandReachDistance", 6);
            // If the distance is -1, return
            if (distance != -1) {
                // Get the block
                Block block = su.nmsProvider.getSpawnerFacing(player, distance);
                if (block != null) {
                    handleBlockChange(player, block, entityID, mobName);
                    return;
                }
            }

            ItemStack itemInHand = su.nmsProvider.getSpawnerItemInHand(player);
            Material itemMaterial;
            try {
                itemMaterial = itemInHand.getType();
            } catch (NullPointerException e) {
                itemMaterial = null;
            }

            if (itemMaterial != null && itemMaterial == su.nmsProvider.getSpawnerMaterial()) {
                handleChangeSpawner(player, entityID, mobName, itemInHand);
            } else if (itemMaterial != null && itemMaterial == su.nmsProvider.getSpawnEggMaterial()) {
                handleChangeEgg(player, entityID, mobName, itemInHand);
            } else {
                su.sendMessage(player,
                        ChatColor.translateAlternateColorCodes('\u0026', plugin.getLocalization().getString("spawnerNotDeterminable")));
            }
        } else {
            su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.getLocalization().getString("noConsole")));
        }

    }

    private void handleBlockChange(Player player, Block block, String entityID, String mobName) {

        if (!player.hasPermission("silkspawners.changetype." + mobName)) {
            su.sendMessage(player,
                    ChatColor.translateAlternateColorCodes('\u0026', plugin.getLocalization().getString("noPermissionChangingSpawner")));
            return;
        }
        // Call the event and maybe change things!
        SilkSpawnersSpawnerChangeEvent changeEvent = new SilkSpawnersSpawnerChangeEvent(player, block, entityID,
                su.getSpawnerEntityID(block), 1);
        plugin.getServer().getPluginManager().callEvent(changeEvent);
        // See if we need to stop
        if (changeEvent.isCancelled()) {
            return;
        }
        // Get the new ID (might be changed)
        String newEntityID = changeEvent.getEntityID();
        String newMob = su.getCreatureName(entityID);
        if (su.setSpawnerType(block, newEntityID, player,
                Common.colorize(plugin.getLocalization().getString("changingDeniedWorldGuard")))) {
            Common.tell(player, plugin.getLocalization().getString("changedSpawner")
                    .replace("%creature%", newMob));

        }

    }

    private void handleChangeSpawner(Player player, String entityID, String mobName, ItemStack itemInHand) {

        if (!player.hasPermission("silkspawners.changetype." + mobName)) {
            Common.tell(player, plugin.getLocalization().getString("noPermissionChangingSpawner"));
            return;
        }

        // Call the event and maybe change things!
        SilkSpawnersSpawnerChangeEvent changeEvent = new SilkSpawnersSpawnerChangeEvent(player, null, entityID,
                su.getStoredSpawnerItemEntityID(itemInHand), itemInHand.getAmount());
        plugin.getServer().getPluginManager().callEvent(changeEvent);
        // See if we need to stop
        if (changeEvent.isCancelled()) {
            return;
        }

        // Get the new ID (might be changed)
        String newEntityID = changeEvent.getEntityID();
        String newMob = su.getCreatureName(entityID);
        ItemStack newItem = su.setSpawnerType(itemInHand, newEntityID, plugin.getLocalization().getString("spawnerName"));
        su.nmsProvider.setSpawnerItemInHand(player, newItem);
        su.sendMessage(player, ChatColor.translateAlternateColorCodes('\u0026', plugin.getLocalization().getString("changedSpawner"))
                .replace("%creature%", newMob));

    }

    private void handleChangeEgg(Player player, String entityID, String mobName, ItemStack itemInHand) {

        if (!player.hasPermission("silkspawners.changetype." + mobName)) {
            su.sendMessage(player,
                    ChatColor.translateAlternateColorCodes('\u0026', plugin.getLocalization().getString("noPermissionChangingEgg")));
            return;
        }

        // Call the event and maybe change things!
        SilkSpawnersSpawnerChangeEvent changeEvent = new SilkSpawnersSpawnerChangeEvent(player, null, entityID,
                su.getStoredSpawnerItemEntityID(itemInHand), itemInHand.getAmount());
        plugin.getServer().getPluginManager().callEvent(changeEvent);
        // See if we need to stop
        if (changeEvent.isCancelled()) {
            return;
        }

        // Get the new ID (might be changed)
        String newEntityID = changeEvent.getEntityID();
        String newMob = su.getCreatureName(entityID);
        ItemStack newItem = su.setSpawnerType(itemInHand, newEntityID, plugin.getLocalization().getString("spawnerName"));
        su.nmsProvider.setSpawnerItemInHand(player, newItem);
        Common.tell(player,plugin.getLocalization().getString("changedEgg")
                .replace("%creature%", newMob));

    }

    private void handleUnknownArgument(CommandSender sender) {
        Common.tell(sender, plugin.getLocalization().getString("unknownArgument"));
    }

    private void handleHelp() {
        if (getPlayer().hasPermission("silkspawners.help")) {
            String message = plugin.getLocalization().getString("help").replace("%version%", plugin.getDescription().getVersion());
            tellNoPrefix(message);
        } else {
            tell(plugin.getLocalization().getString("noPermission"));
        }
    }

    private void handleReload(CommandSender sender) {
        if (sender.hasPermission("silkspawners.reload")) {
            plugin.reloadConfigs();
            Common.tell(sender, plugin.getLocalization().getString("configsReloaded"));
        } else {
            Common.tell(sender, plugin.getLocalization().getString("noPermission"));
        }
    }

    private void handleList(CommandSender sender) {
        su.showAllCreatures(sender);
    }

    private void handleView(CommandSender sender) {
        //TODO has to be a player
        if (sender instanceof Player) {
            // If the distance is -1, return
            int distance = plugin.config.getInt("spawnerCommandReachDistance", 6);
            if (distance == -1) {
                return;
            }
            // Get the block, returns null for non spawner blocks
            Player player = (Player) sender;
            Block block = su.nmsProvider.getSpawnerFacing(player, distance);
            if (block == null) {
                tell(plugin.getLocalization().getString("lookAtSpawner"));
                return;
            }
            String entityID = su.getSpawnerEntityID(block);
            if (player.hasPermission("silkspawners.viewtype")) {
                tell(plugin.getLocalization().getString("getSpawnerType").replace("%creature%", su.getCreatureName(entityID)));
            } else {
                tell(plugin.getLocalization().getString("noPermissionViewType"));
            }
        } else {
            Common.tell(sender,plugin.getLocalization().getString("noConsole"));
        }
    }
}
