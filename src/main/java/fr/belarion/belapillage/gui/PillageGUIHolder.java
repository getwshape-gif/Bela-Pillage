package fr.belarion.belapillage.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marqueur permettant a {@link GUIListener} d'identifier de facon fiable un inventaire
 * ouvert par Bela-Pillage, sans dependre du titre (qui peut varier ou etre traduit).
 */
public final class PillageGUIHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
