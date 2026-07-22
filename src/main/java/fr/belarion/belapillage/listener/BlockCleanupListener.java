package fr.belarion.belapillage.listener;

import fr.belarion.belapillage.BelaPillage;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Ecouteur strictement passif : sa SEULE responsabilite est d'oublier les degats
 * residuels d'un bloc protege quand un joueur le mine normalement, pour eviter qu'une
 * entree obsolete ne s'accumule indefiniment en memoire (pertinent sur la duree avec
 * plusieurs milliers de blocs vus au fil du temps).
 * <p>
 * Priorite MONITOR et aucun appel a setCancelled(...) nulle part dans cette classe :
 * elle ne peut par construction avoir aucune influence sur le minage vanilla (cassage,
 * drop, experience, outil requis, tout reste 100% gere par le serveur et/ou d'autres
 * plugins comme avant l'installation de Bela-Pillage). Voir ExplosionListener pour la
 * seule logique qui modifie reellement un evenement (et uniquement des evenements
 * d'explosion).
 */
public final class BlockCleanupListener implements Listener {

    private final BelaPillage plugin;

    public BlockCleanupListener(BelaPillage plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (plugin.getDurabilityManager().isProtected(block.getType())) {
            plugin.getDurabilityManager().clearEntry(block);
        }
    }
}
