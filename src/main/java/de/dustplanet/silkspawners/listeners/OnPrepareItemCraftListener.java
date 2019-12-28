package de.dustplanet.silkspawners.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author sarhatabaot
 */
public class OnPrepareItemCraftListener extends SilkListener {
    public OnPrepareItemCraftListener() {
        super();
        setName("OnPrepareItemCraftListener");
        verbose();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareItemCraftEvent(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) {
            return;
        }

        if (event.getRecipe().getResult().getType() != getSilkUtil().nmsProvider.getSpawnerMaterial()) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();

        for (ItemStack itemStack : event.getInventory().getContents()) {
            if (itemStack.getType() == getSilkUtil().nmsProvider.getSpawnEggMaterial() && itemStack.getDurability() == 0) {
                String entityID = getSilkUtil().getStoredEggEntityID(itemStack);
                result = getSilkUtil().newSpawnerItem(entityID, getSilkUtil().getCustomSpawnerName(entityID), result.getAmount(), true);
                event.getInventory().setResult(result);
            }
        }
    }
}
