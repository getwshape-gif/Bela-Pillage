package fr.belarion.belapillage;

import fr.belarion.belapillage.command.AdminCommand;
import fr.belarion.belapillage.command.PillageCommand;
import fr.belarion.belapillage.config.MessagesManager;
import fr.belarion.belapillage.config.PillageConfig;
import fr.belarion.belapillage.durability.DurabilityManager;
import fr.belarion.belapillage.gui.GUIListener;
import fr.belarion.belapillage.gui.PillageGUI;
import fr.belarion.belapillage.listener.BlockCleanupListener;
import fr.belarion.belapillage.listener.ExplosionListener;
import fr.belarion.belapillage.listener.PotatoCheckListener;
import fr.belarion.belapillage.storage.DurabilityStorage;
import fr.belarion.belapillage.storage.FlatFileDurabilityStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Point d'entree du plugin, cablage de tout Bela-Pillage.
 * <p>
 * Rappel du perimetre (voir README) : ce plugin ne gere QUE la durabilite des blocs
 * protege face aux EXPLOSIONS. Il n'ecoute jamais d'evenement pouvant modifier le
 * minage vanilla, et ne touche jamais aux blocs deja proteges par Bela-Customs
 * (materiaux completement disjoints, voir ExplosionListener et PillageConfig).
 */
public final class BelaPillage extends JavaPlugin {

    private DurabilityManager durabilityManager;
    private DurabilityStorage storage;
    private PillageConfig pillageConfig;
    private MessagesManager messagesManager;
    private PillageGUI pillageGUI;

    private int autosaveTaskId = -1;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("Factions") == null) {
            getLogger().severe("SaberFactions (nom interne 'Factions') est introuvable. "
                    + "Bela-Pillage a besoin de son API pour la verification de territoire ennemi. Desactivation.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        durabilityManager = new DurabilityManager();
        pillageConfig = new PillageConfig(this);
        messagesManager = new MessagesManager(this);
        pillageGUI = new PillageGUI(this);
        storage = new FlatFileDurabilityStorage(new File(getDataFolder(), "durability.dat"));

        pillageConfig.load();
        messagesManager.load();

        try {
            storage.load(durabilityManager);
            getLogger().info("Durabilite chargee : " + durabilityManager.getDamagedBlockCount() + " bloc(s) endommage(s) restaure(s).");
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Impossible de charger le fichier de durabilite, demarrage avec un etat vide.", e);
        }

        Bukkit.getPluginManager().registerEvents(new ExplosionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockCleanupListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PotatoCheckListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(), this);

        PillageCommand pillageCommand = new PillageCommand(this);
        getCommand("explosion").setExecutor(pillageCommand);
        getCommand("pillage").setExecutor(pillageCommand);
        getCommand("belapillage").setExecutor(new AdminCommand(this));

        scheduleAutosave();

        getLogger().info("Bela-Pillage active : " + durabilityManager.getRegisteredTypes().size() + " type(s) de bloc protege(s).");
    }

    @Override
    public void onDisable() {
        if (autosaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autosaveTaskId);
            autosaveTaskId = -1;
        }
        saveNow("extinction du plugin");
    }

    private void scheduleAutosave() {
        int intervalSeconds = Math.max(30, pillageConfig.getAutosaveIntervalSeconds());
        long ticks = intervalSeconds * 20L;
        autosaveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> saveNow("sauvegarde automatique"), ticks, ticks).getTaskId();
    }

    private void saveNow(String reason) {
        if (storage == null || durabilityManager == null) {
            return;
        }
        try {
            storage.save(durabilityManager);
            if (pillageConfig != null && pillageConfig.isDebug()) {
                getLogger().info("[Bela-Pillage] Durabilite sauvegardee (" + reason + "), "
                        + durabilityManager.getDamagedBlockCount() + " bloc(s) endommage(s).");
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Echec de la sauvegarde de la durabilite (" + reason + ")", e);
        }
    }

    public DurabilityManager getDurabilityManager() {
        return durabilityManager;
    }

    public PillageConfig getPillageConfig() {
        return pillageConfig;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public PillageGUI getPillageGUI() {
        return pillageGUI;
    }
}
