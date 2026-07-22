package fr.belarion.belapillage.command;

import fr.belarion.belapillage.BelaPillage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * /belapillage reload : recharge config.yml et messages.yml a chaud (registre des blocs
 * proteges reconstruit, aucune perte des degats deja enregistres puisque ceux-ci vivent
 * dans DurabilityManager/le fichier de durabilite, jamais dans la configuration).
 */
public final class AdminCommand implements CommandExecutor {

    private final BelaPillage plugin;

    public AdminCommand(BelaPillage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("belapillage.admin")) {
            sender.sendMessage(plugin.getMessagesManager().get("general.no-permission"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.getPillageConfig().load();
            plugin.getMessagesManager().load();
            sender.sendMessage(plugin.getMessagesManager().get("general.reload-success"));
            return true;
        }

        sender.sendMessage("Usage: /" + label + " reload");
        return true;
    }
}
