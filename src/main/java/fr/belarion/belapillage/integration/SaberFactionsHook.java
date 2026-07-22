package fr.belarion.belapillage.integration;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Relation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Point d'acces unique vers l'API PUBLIQUE de SaberFactions (com.massivecraft.factions.*),
 * meme principe et meme esprit que SaberFactionsHook dans BelarionFactions : lecture
 * seule, aucune modification de l'etat interne de Factions, jamais de contact avec
 * SaberFactions.jar autrement qu'a travers son API publique.
 * <p>
 * Sert uniquement a determiner si un bloc clique se trouve en territoire ennemi pour la
 * verification de durabilite via une patate (voir PotatoCheckListener).
 */
public final class SaberFactionsHook {

    private SaberFactionsHook() {
    }

    /**
     * @return la faction proprietaire du territoire a cet emplacement, ou null si le
     * terrain est "wilderness" (aucune faction, chunk non revendique) ou une faction
     * speciale (safezone/warzone).
     */
    public static Faction getFactionAt(Location location) {
        Faction faction = Board.getInstance().getFactionAt(new FLocation(location));
        if (faction == null || !faction.isNormal()) {
            return null;
        }
        return faction;
    }

    /**
     * @return true si le bloc se trouve dans un territoire appartenant a une faction
     * normale ET que la relation entre le joueur et cette faction est ENEMY (ennemie
     * declaree). Un joueur sans faction, ou dont la faction n'a pas declare le
     * proprietaire du territoire comme ennemi, ne verra jamais true ici : c'est le
     * comportement Factions standard pour "relation ennemie", coherent avec l'usage
     * deja fait de Relation dans BelarionFactions (FactionDisplayUtil).
     */
    public static boolean isEnemyTerritory(Player player, Block block) {
        Faction territoryFaction = getFactionAt(block.getLocation());
        if (territoryFaction == null) {
            return false;
        }
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        if (fPlayer == null) {
            return false;
        }
        Relation relation = fPlayer.getRelationTo(territoryFaction);
        return relation != null && relation.isEnemy();
    }
}
