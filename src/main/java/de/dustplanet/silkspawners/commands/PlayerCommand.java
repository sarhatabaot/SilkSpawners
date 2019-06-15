package de.dustplanet.silkspawners.commands;

import de.dustplanet.silkspawners.util.Common;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


/**
 * @author sarhatabaot
 */
public abstract class PlayerCommand extends Command {
    @Setter(value= AccessLevel.PROTECTED)
    private String prefix;

    private String[] args;
    @Getter
    private Player player;

    public PlayerCommand(@NotNull final String name) {
        super(name);
    }

    @Override
    public boolean execute(@NotNull final CommandSender commandSender, @NotNull final String label, @NotNull final String[] args) {
        if(!(commandSender instanceof Player)){
            Common.tell(commandSender, "&cYou must be in-game to use this command.");
            return false;
        }
        this.player = (Player) commandSender;
        this.args = args;
        run(player,args);
        return true;
    }


    protected void tell(String message){
        Common.tell(player,prefix+message);
    }

    protected void tellNoPrefix(String message) {Common.tell(player,message);}

    public abstract void run(@NotNull final Player player, @NotNull String[] args);
}
