package fr.belarion.belapillage.command;

import fr.belarion.belapillage.BelaPillage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executeur partage par /explosion et /pillage (voir plugin.yml) : les deux commandes
 * ouvrent exactement le meme GUI, sans aucune difference de comportement.
 */
public final class PillageCommand implements CommandExecutor {

    private final BelaPillage plugin;

    public PillageCommand(BelaPillage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessagesManager().get("general.player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("belapillage.use")) {
            player.sendMessage(plugin.getMessagesManager().get("general.no-permission"));
            return true;
        }

        plugin.getPillageGUI().open(player);
        return true;
    }
}
