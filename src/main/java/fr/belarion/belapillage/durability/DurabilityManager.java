package fr.belarion.belapillage.durability;

import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coeur du systeme de durabilite face aux explosions.
 * <p>
 * Deux structures en memoire :
 * <ul>
 *     <li>{@code registry} : les types de blocs proteges (materiau -&gt; durabilite max),
 *     defini par config.yml, tres petit (quelques entrees) ;</li>
 *     <li>{@code hitsTaken} : uniquement les blocs ayant deja subi au moins une explosion
 *     (cle {@link BlockKey} -&gt; nombre d'explosions deja encaissees). Un bloc jamais
 *     touche n'a AUCUNE entree ici : la structure reste donc proportionnelle au nombre de
 *     blocs reellement endommages, et non a la totalite des blocs proteges du monde, ce
 *     qui la garde performante meme avec plusieurs milliers de blocs endommages.</li>
 * </ul>
 * {@link ConcurrentHashMap} est utilise pour {@code hitsTaken} car elle est lue de
 * facon asynchrone par la sauvegarde periodique (voir BelaPillage) pendant qu'elle
 * continue d'etre modifiee par le thread principal a chaque explosion.
 * <p>
 * IMPORTANT : cette classe ne touche JAMAIS a BlockBreakEvent pour empecher ou modifier
 * le minage. Le seul nettoyage effectue au minage (voir le listener d'explosion) est
 * une suppression passive d'entree, en priorite MONITOR et sans jamais annuler
 * quoi que ce soit, uniquement pour eviter qu'une entree obsolete ne reste en memoire
 * indefiniment apres qu'un joueur ait mine puis remplace le bloc.
 */
public final class DurabilityManager {

    private final Map<Material, ProtectedBlockType> registry = new EnumMap<>(Material.class);
    private final Map<BlockKey, Integer> hitsTaken = new ConcurrentHashMap<>();

    /**
     * Resultat d'une explosion touchant un bloc protege.
     */
    public static final class ExplosionResult {
        private final boolean destroyed;
        private final int remaining;
        private final int max;

        ExplosionResult(boolean destroyed, int remaining, int max) {
            this.destroyed = destroyed;
            this.remaining = remaining;
            this.max = max;
        }

        public boolean isDestroyed() {
            return destroyed;
        }

        public int getRemaining() {
            return remaining;
        }

        public int getMax() {
            return max;
        }
    }

    public void clearRegistry() {
        registry.clear();
    }

    /**
     * @return false si un type est deja enregistre pour ce materiau (conflit de config,
     * a ignorer par l'appelant plutot que d'ecraser silencieusement).
     */
    public boolean registerType(ProtectedBlockType type) {
        if (registry.containsKey(type.getMaterial())) {
            return false;
        }
        registry.put(type.getMaterial(), type);
        return true;
    }

    public boolean isProtected(Material material) {
        return registry.containsKey(material);
    }

    public ProtectedBlockType getType(Material material) {
        return registry.get(material);
    }

    public Collection<ProtectedBlockType> getRegisteredTypes() {
        return registry.values();
    }

    /**
     * Enregistre l'impact d'une explosion sur ce bloc (deja verifie comme protege par
     * l'appelant). A appeler une seule fois par bloc et par explosion.
     */
    public ExplosionResult registerExplosionHit(Block block) {
        ProtectedBlockType type = registry.get(block.getType());
        if (type == null) {
            throw new IllegalStateException("registerExplosionHit appele sur un bloc non protege: " + block.getType());
        }
        BlockKey key = BlockKey.of(block);
        int taken = hitsTaken.merge(key, 1, Integer::sum);
        int max = type.getMaxDurability();
        if (taken >= max) {
            hitsTaken.remove(key);
            return new ExplosionResult(true, 0, max);
        }
        return new ExplosionResult(false, max - taken, max);
    }

    /**
     * @return la durabilite restante actuelle du bloc (sans effet de bord), ou -1 si ce
     * materiau n'est pas protege par Bela-Pillage.
     */
    public int getRemainingDurability(Block block) {
        ProtectedBlockType type = registry.get(block.getType());
        if (type == null) {
            return -1;
        }
        Integer taken = hitsTaken.get(BlockKey.of(block));
        return taken == null ? type.getMaxDurability() : Math.max(0, type.getMaxDurability() - taken);
    }

    /**
     * Supprime toute trace de degats pour cet emplacement : a appeler quand un bloc
     * protege est pose ou mine (nettoyage passif, ne modifie jamais l'evenement lui-meme).
     */
    public void clearEntry(Block block) {
        hitsTaken.remove(BlockKey.of(block));
    }

    public int getDamagedBlockCount() {
        return hitsTaken.size();
    }

    // ---- Utilise uniquement par le stockage (chargement/sauvegarde) ----

    public Map<BlockKey, Integer> getRawHitsMap() {
        return hitsTaken;
    }

    public void loadRawEntry(BlockKey key, int hits) {
        if (hits > 0) {
            hitsTaken.put(key, hits);
        }
    }
}
