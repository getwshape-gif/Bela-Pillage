package fr.belarion.belapillage.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Le GUI Bela-Pillage est un affichage pur : aucun item ne doit pouvoir en etre retire
 * ni deplace. Tout clic dans un inventaire identifie par {@link PillageGUIHolder} est
 * systematiquement annule, y compris les clics dans l'inventaire du joueur pendant que
 * ce GUI est ouvert (shift-click, etc.), pour eviter tout transfert accidentel.
 */
public final class GUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        if (topHolder instanceof PillageGUIHolder) {
            event.setCancelled(true);
        }
    }
}
