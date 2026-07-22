package fr.belarion.belapillage.config;

import fr.belarion.belapillage.BelaPillage;
import fr.belarion.belapillage.durability.DurabilityManager;
import fr.belarion.belapillage.durability.ProtectedBlockType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Charge config.yml et alimente le {@link DurabilityManager}. Migration additive comme
 * dans Bela-Customs/BelarionFactions (TagsConfig) : {@code options().copyDefaults(true)}
 * ajoute les cles manquantes d'une future mise a jour sans jamais ecraser une valeur deja
 * personnalisee par l'utilisateur, puis sauvegarde le resultat.
 */
public final class PillageConfig {

    /**
     * Materiaux deja utilises par des blocs premium de Bela-Customs. Toute entree de
     * config.yml (section "blocks") pointant vers l'un de ces materiaux est ignoree avec
     * un avertissement en console, pour ne jamais pouvoir interferer avec le systeme de
     * durabilite deja en place dans Bela-Customs, meme en cas d'erreur de configuration.
     */
    private static final Set<Material> RESERVED_BY_BELA_CUSTOMS = EnumSet.of(
            Material.ENDER_CHEST,   // Coffre en Émeraude
            Material.SEA_LANTERN,   // Enclume Émeraude
            Material.PRISMARINE     // Table d'Enchantement Émeraude (data 2 uniquement, mais le
                                     // materiau entier est exclu par prudence)
    );

    public static final class ManagedByOtherPlugin {
        private final String displayName;
        private final Material icon;
        private final String resistanceDisplay;
        private final boolean insensible;

        ManagedByOtherPlugin(String displayName, Material icon, String resistanceDisplay, boolean insensible) {
            this.displayName = displayName;
            this.icon = icon;
            this.resistanceDisplay = resistanceDisplay;
            this.insensible = insensible;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }

        public String getResistanceDisplay() {
            return resistanceDisplay;
        }

        public boolean isInsensible() {
            return insensible;
        }
    }

    private final BelaPillage plugin;
    private final Logger logger;

    private boolean potatoCheckEnabled;
    private boolean requireEnemyTerritory;
    private long messageCooldownMs;
    private int autosaveIntervalSeconds;
    private boolean debug;
    private final List<ManagedByOtherPlugin> managedByBelaCustoms = new ArrayList<>();

    public PillageConfig(BelaPillage plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        config.options().copyDefaults(true);
        plugin.saveConfig();

        loadProtectedBlocks(config);
        loadManagedByBelaCustoms(config);
        loadMisc(config);
    }

    private void loadProtectedBlocks(FileConfiguration config) {
        DurabilityManager manager = plugin.getDurabilityManager();
        manager.clearRegistry();

        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection == null) {
            logger.warning("Aucune section 'blocks' trouvee dans config.yml : aucun bloc ne sera protege.");
            return;
        }

        for (String id : blocksSection.getKeys(false)) {
            ConfigurationSection entry = blocksSection.getConfigurationSection(id);
            if (entry == null) {
                continue;
            }

            String materialName = entry.getString("material");
            Material material = materialName == null ? null : Material.matchMaterial(materialName);
            if (material == null) {
                logger.warning("Bloc '" + id + "' ignore : materiau invalide '" + materialName + "'.");
                continue;
            }

            if (RESERVED_BY_BELA_CUSTOMS.contains(material)) {
                logger.warning("Bloc '" + id + "' ignore : le materiau " + material
                        + " est deja utilise par un bloc premium de Bela-Customs. "
                        + "Bela-Pillage ne doit jamais gerer ce materiau pour eviter tout conflit.");
                continue;
            }

            int durability = entry.getInt("durability", -1);
            if (durability <= 0) {
                logger.warning("Bloc '" + id + "' ignore : durabilite invalide (" + durability + "), doit etre > 0.");
                continue;
            }

            String displayName = entry.getString("display-name", id);

            ProtectedBlockType type = new ProtectedBlockType(id, material, durability, displayName);
            if (!manager.registerType(type)) {
                logger.warning("Bloc '" + id + "' ignore : le materiau " + material
                        + " est deja associe a un autre bloc protege dans config.yml (une seule entree par materiau).");
            }
        }
    }

    private void loadManagedByBelaCustoms(FileConfiguration config) {
        managedByBelaCustoms.clear();
        ConfigurationSection section = config.getConfigurationSection("already-managed-by-bela-customs");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) {
                continue;
            }
            String displayName = entry.getString("display-name", id);
            Material icon = Material.matchMaterial(entry.getString("icon", "BARRIER"));
            if (icon == null) {
                icon = Material.BARRIER;
            }
            String resistanceDisplay = entry.getString("resistance-display", "?");
            boolean insensible = entry.getBoolean("insensible", false);
            managedByBelaCustoms.add(new ManagedByOtherPlugin(displayName, icon, resistanceDisplay, insensible));
        }
    }

    private void loadMisc(FileConfiguration config) {
        potatoCheckEnabled = config.getBoolean("potato-check.enabled", true);
        requireEnemyTerritory = config.getBoolean("potato-check.require-enemy-territory", true);
        messageCooldownMs = config.getLong("potato-check.message-cooldown-ms", 500L);
        autosaveIntervalSeconds = config.getInt("storage.autosave-interval-seconds", 300);
        debug = config.getBoolean("debug", false);
    }

    public boolean isPotatoCheckEnabled() {
        return potatoCheckEnabled;
    }

    public boolean isRequireEnemyTerritory() {
        return requireEnemyTerritory;
    }

    public long getMessageCooldownMs() {
        return messageCooldownMs;
    }

    public int getAutosaveIntervalSeconds() {
        return autosaveIntervalSeconds;
    }

    public boolean isDebug() {
        return debug;
    }

    public List<ManagedByOtherPlugin> getManagedByBelaCustoms() {
        return managedByBelaCustoms;
    }
}
