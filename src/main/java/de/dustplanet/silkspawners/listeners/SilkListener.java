package de.dustplanet.silkspawners.listeners;

import de.dustplanet.silkspawners.SilkSpawners;
import de.dustplanet.silkspawners.util.SilkUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Listener;


/**
 * @author sarhatabaot
 */
@Getter
public abstract class SilkListener implements Listener {
    protected SilkSpawners plugin;
    protected SilkUtil silkUtil;
    @Setter (AccessLevel.PROTECTED)
    private String name;

    public SilkListener() {
        plugin = SilkSpawners.getInstance();
        silkUtil = plugin.getSilkUtil();
    }

    protected void verbose(){
        SilkSpawners.verbose(getName()+" registered.");
    }

    protected void verbose(String message){
        SilkSpawners.verbose(getName()+" "+message);
    }

    //TODO: Add prefix
    protected void verbose(String... messages){
        SilkSpawners.verbose(messages);
    }
}
