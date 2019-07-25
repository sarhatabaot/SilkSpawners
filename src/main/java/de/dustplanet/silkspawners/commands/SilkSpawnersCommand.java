package de.dustplanet.silkspawners.commands;

import de.dustplanet.silkspawners.util.Common;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;


/**
 * @author sarhatabaot
 */
public abstract class SilkSpawnersCommand extends Command {
    @Setter(value= AccessLevel.PROTECTED)
    private String prefix;

    @Getter
    private CommandSender sender;

    public SilkSpawnersCommand(@NotNull final String name) {
        super(name);
    }

    @Override
    public boolean execute(@NotNull final CommandSender commandSender, @NotNull final String label, @NotNull final String[] args) {
        this.sender = commandSender;
        run(sender,args);
        return true;
    }


    protected void tell(String message){
        Common.tell(sender,prefix+" "+message);
    }

    protected void tellNoPrefix(String message) {Common.tell(sender,message);}

    public abstract void run(@NotNull final CommandSender sender, @NotNull String[] args);
}
