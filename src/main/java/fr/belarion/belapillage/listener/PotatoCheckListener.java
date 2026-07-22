package fr.belarion.belapillage.listener;

import fr.belarion.belapillage.BelaPillage;
import fr.belarion.belapillage.durability.DurabilityManager;
import fr.belarion.belapillage.durability.ProtectedBlockType;
import fr.belarion.belapillage.integration.SaberFactionsHook;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consultation de la durabilite restante d'un bloc protege : un joueur tenant une
 * patate normale (Material.POTATO_ITEM - a ne pas confondre avec Material.POTATO, qui
 * est le bloc de plante de pomme de terre en 1.8, jamais l'item tenu en main) effectue
 * un clic gauche ou droit sur un bloc en territoire ennemi.
 * <p>
 * Entierement passif : cette classe n'annule jamais l'evenement. Une patate en main ne
 * declenche normalement aucune action speciale sur un bloc en vanilla, donc laisser
 * l'evenement suivre son cours n'a aucun effet de bord.
 */
public final class PotatoCheckListener implements Listener {

    private final BelaPillage plugin;
    private final Map<UUID, Long> lastMessageAt = new ConcurrentHashMap<>();

    public PotatoCheckListener(BelaPillage plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getPillageConfig().isPotatoCheckEnabled()) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() != Material.POTATO_ITEM) {
            return;
        }

        DurabilityManager manager = plugin.getDurabilityManager();
        ProtectedBlockType type = manager.getType(block.getType());
        if (type == null) {
            return;
        }

        if (plugin.getPillageConfig().isRequireEnemyTerritory()
                && !SaberFactionsHook.isEnemyTerritory(player, block)) {
            return;
        }

        if (isOnCooldown(player)) {
            return;
        }

        int remaining = manager.getRemainingDurability(block);
        String message = plugin.getMessagesManager().get("potato-check.result")
                .replace("{block}", type.getDisplayName())
                .replace("{current}", String.valueOf(remaining))
                .replace("{max}", String.valueOf(type.getMaxDurability()));
        player.sendMessage(message);
    }

    private boolean isOnCooldown(Player player) {
        long cooldownMs = plugin.getPillageConfig().getMessageCooldownMs();
        if (cooldownMs <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = lastMessageAt.get(player.getUniqueId());
        if (last != null && now - last < cooldownMs) {
            return true;
        }
        lastMessageAt.put(player.getUniqueId(), now);
        return false;
    }
}
