package fr.belarion.belapillage.durability;

import org.bukkit.Material;

/**
 * Un type de bloc protege contre les explosions par Bela-Pillage, tel que defini dans
 * config.yml (section {@code blocks}). Purement une donnee de configuration : aucune
 * logique ici, voir {@link DurabilityManager} et le listener d'explosion.
 */
public final class ProtectedBlockType {

    private final String id;
    private final Material material;
    private final int maxDurability;
    private final String displayName;

    public ProtectedBlockType(String id, Material material, int maxDurability, String displayName) {
        if (maxDurability <= 0) {
            throw new IllegalArgumentException("La durabilite de '" + id + "' doit etre > 0 (valeur: " + maxDurability + ")");
        }
        this.id = id;
        this.material = material;
        this.maxDurability = maxDurability;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public String getDisplayName() {
        return displayName;
    }
}
