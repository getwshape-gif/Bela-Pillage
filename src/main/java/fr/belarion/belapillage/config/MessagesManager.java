package fr.belarion.belapillage.config;

import fr.belarion.belapillage.BelaPillage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Charge messages.yml (migration additive, meme principe que PillageConfig/TagsConfig) et
 * expose des helpers d'envoi avec placeholders simples ({@code {cle}}).
 */
public final class MessagesManager {

    private final BelaPillage plugin;
    private FileConfiguration messages;

    public MessagesManager(BelaPillage plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(file);

        try (InputStream defaultStream = plugin.getResource("messages.yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                messages.setDefaults(defaults);
                messages.options().copyDefaults(true);
                messages.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder messages.yml apres migration additive: " + e.getMessage());
        }
    }

    public String get(String path) {
        String raw = messages.getString(path, path);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String get(String path, String placeholder, String value) {
        return get(path).replace("{" + placeholder + "}", value);
    }

    public void send(CommandSender target, String path) {
        target.sendMessage(get(path));
    }
}
