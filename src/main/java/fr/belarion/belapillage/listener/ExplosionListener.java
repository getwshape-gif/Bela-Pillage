package fr.belarion.belapillage.listener;

import fr.belarion.belapillage.BelaPillage;
import fr.belarion.belapillage.durability.DurabilityManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * Applique la durabilite face aux explosions, et RIEN d'autre : cette classe n'ecoute
 * jamais BlockBreakEvent pour empecher/modifier le minage (voir
 * {@link BlockCleanupListener} pour le seul nettoyage passif necessaire au minage).
 * <p>
 * Aucun risque de conflit avec Bela-Customs : les materiaux geres ici (Obsidienne,
 * Enclume, Table d'Enchantement par defaut, voir config.yml) sont completement
 * disjoints des materiaux utilises par les blocs premium de Bela-Customs
 * (Material.ENDER_CHEST, Material.SEA_LANTERN, Material.PRISMARINE) - confirme en
 * lisant le code source de Bela-Customs (EmeraldChestListener, BlockProtectionListener)
 * et applique en dur via PillageConfig#RESERVED_BY_BELA_CUSTOMS, qui refuse au
 * demarrage toute configuration qui tenterait de faire chevaucher ces materiaux. La
 * priorite d'ecoute (NORMAL) n'a donc strictement aucune importance vis-a-vis de
 * Bela-Customs : les deux plugins ne se voient jamais mutuellement dans blockList().
 */
public final class ExplosionListener implements Listener {

    private final BelaPillage plugin;

    public ExplosionListener(BelaPillage plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    /**
     * Pour chaque bloc protege touche : durabilite -1. Si la durabilite n'est pas encore
     * a 0, le bloc est retire de la liste (il survit a CETTE explosion precise, exactement
     * comme le fait deja EmeraldChestListener/BlockProtectionListener dans Bela-Customs).
     * S'il atteint 0, il reste dans la liste : l'explosion vanilla suit alors son cours
     * normal et le detruit avec un drop naturel standard, sans aucune intervention
     * supplementaire de ce plugin.
     */
    private void handleExplosion(List<Block> blocks) {
        DurabilityManager manager = plugin.getDurabilityManager();
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block block = it.next();
            Material type = block.getType();
            if (!manager.isProtected(type)) {
                continue;
            }

            DurabilityManager.ExplosionResult result = manager.registerExplosionHit(block);
            if (result.isDestroyed()) {
                if (plugin.getPillageConfig().isDebug()) {
                    plugin.getLogger().log(Level.INFO, () -> "[Bela-Pillage] " + type
                            + " detruit par explosion en " + block.getWorld().getName()
                            + " (" + block.getX() + "," + block.getY() + "," + block.getZ() + ")");
                }
            } else {
                it.remove();
                if (plugin.getPillageConfig().isDebug()) {
                    plugin.getLogger().log(Level.INFO, () -> "[Bela-Pillage] " + type
                            + " touche par explosion, durabilite restante: "
                            + result.getRemaining() + "/" + result.getMax());
                }
            }
        }
    }

    /**
     * Nettoyage passif : quand un joueur repose un bloc protege a un emplacement qui
     * portait des degats residuels (bloc precedent mine puis remplace), le nouveau bloc
     * repart a durabilite maximale. N'annule jamais la pose, ne modifie aucun autre
     * comportement.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (plugin.getDurabilityManager().isProtected(block.getType())) {
            plugin.getDurabilityManager().clearEntry(block);
        }
    }
}
