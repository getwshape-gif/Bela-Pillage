package fr.belarion.belapillage.gui;

import fr.belarion.belapillage.BelaPillage;
import fr.belarion.belapillage.config.PillageConfig;
import fr.belarion.belapillage.durability.ProtectedBlockType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI premium (verres decoratifs, couleurs sobres, touches d'emeraude) affichant la
 * resistance aux explosions de tous les blocs concernes : ceux geres activement par
 * Bela-Pillage, et ceux deja geres par Bela-Customs (purement informatif, voir
 * config.yml -&gt; already-managed-by-bela-customs). Purement un affichage : aucun item
 * de ce GUI n'est cliquable (voir {@link GUIListener}).
 */
public final class PillageGUI {

    private static final int SIZE = 36;
    private static final String TITLE = ChatColor.DARK_GRAY + "» " + ChatColor.DARK_GREEN
            + "" + ChatColor.BOLD + "Bela-Pillage" + ChatColor.RESET + ChatColor.DARK_GRAY + " «";

    // Slots interieurs disponibles pour les items (bordure decorative tout autour).
    private static final int[] CONTENT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    private final BelaPillage plugin;

    public PillageGUI(BelaPillage plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        PillageGUIHolder holder = new PillageGUIHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.setInventory(inventory);

        fillBorder(inventory);
        fillContent(inventory);

        player.openInventory(inventory);
    }

    private void fillBorder(Inventory inventory) {
        ItemStack grayPane = pane((short) 8, " ");
        ItemStack greenPane = pane((short) 13, " ");

        // Rangee du haut et rangee du bas : verre gris, avec les 4 coins en vert emeraude.
        int[] topRow = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        int[] bottomRow = {27, 28, 29, 30, 31, 32, 33, 34, 35};
        for (int slot : topRow) {
            inventory.setItem(slot, grayPane);
        }
        for (int slot : bottomRow) {
            inventory.setItem(slot, grayPane);
        }
        inventory.setItem(0, greenPane);
        inventory.setItem(8, greenPane);
        inventory.setItem(27, greenPane);
        inventory.setItem(35, greenPane);

        // Colonnes gauche/droite des deux rangees centrales.
        int[] sides = {9, 17, 18, 26};
        for (int slot : sides) {
            inventory.setItem(slot, grayPane);
        }
    }

    private ItemStack pane(short data, String name) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private void fillContent(Inventory inventory) {
        PillageConfig config = plugin.getPillageConfig();
        List<ItemStack> entries = new ArrayList<>();

        for (ProtectedBlockType type : sortedByName(plugin.getDurabilityManager().getRegisteredTypes())) {
            entries.add(buildBlockItem(
                    type.getMaterial(),
                    type.getDisplayName(),
                    type.getMaxDurability() + " explosions",
                    false,
                    "Bela-Pillage"
            ));
        }

        for (PillageConfig.ManagedByOtherPlugin managed : config.getManagedByBelaCustoms()) {
            entries.add(buildBlockItem(
                    managed.getIcon(),
                    managed.getDisplayName(),
                    managed.getResistanceDisplay(),
                    managed.isInsensible(),
                    "Bela-Customs"
            ));
        }

        entries.add(buildInfoItem());

        int slotIndex = 0;
        for (ItemStack entry : entries) {
            if (slotIndex >= CONTENT_SLOTS.length) {
                plugin.getLogger().warning("GUI Bela-Pillage : trop d'entrees pour le nombre de slots disponibles, certaines ne sont pas affichees.");
                break;
            }
            inventory.setItem(CONTENT_SLOTS[slotIndex++], entry);
        }
    }

    private List<ProtectedBlockType> sortedByName(Iterable<ProtectedBlockType> types) {
        List<ProtectedBlockType> list = new ArrayList<>();
        types.forEach(list::add);
        list.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
        return list;
    }

    private ItemStack buildBlockItem(Material icon, String displayName, String resistanceDisplay, boolean insensible, String managedBy) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + displayName);

        String resistanceLine = insensible
                ? ChatColor.DARK_GREEN + "✦ " + ChatColor.GREEN + "Insensible aux explosions" + ChatColor.DARK_GREEN + " ✦"
                : ChatColor.GRAY + "Résistance : " + ChatColor.GREEN + resistanceDisplay;

        meta.setLore(Arrays.asList(
                resistanceLine,
                "",
                ChatColor.DARK_GRAY + "Géré par " + ChatColor.DARK_GREEN + managedBy
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem() {
        ItemStack item = new ItemStack(Material.POTATO_ITEM);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Vérification de durabilité");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Tiens une patate normale et clique",
                ChatColor.GRAY + "(gauche ou droit) sur un bloc protégé",
                ChatColor.GRAY + "situé en territoire ennemi pour voir",
                ChatColor.GRAY + "sa durabilité restante."
        ));
        item.setItemMeta(meta);
        return item;
    }
}
