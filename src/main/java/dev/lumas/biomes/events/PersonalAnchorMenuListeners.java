package dev.lumas.biomes.events;

import dev.lumas.biomes.gui.PersonalAnchorMenu;
import dev.lumas.biomes.model.PersonalBiomes;
import dev.lumas.biomes.util.Executors;
import dev.lumas.biomes.util.TextUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class PersonalAnchorMenuListeners implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PersonalAnchorMenu menu) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getInventory().getSize()) {
            onOwnInventoryClick(event, menu, player);
            return;
        }

        event.setCancelled(true);
        if (rawSlot != PersonalAnchorMenu.ANCHOR_SLOT) {
            return;
        }

        switch (event.getClick()) {
            case LEFT, RIGHT -> {
                ItemStack cursor = event.getCursor().clone();
                if (cursor.isEmpty()) {
                    ItemStack withdrawn = menu.withdraw(player);
                    if (withdrawn != null) {
                        event.getView().setCursor(withdrawn);
                        TextUtil.msg(player, "Anchor removed.");
                    }
                } else if (depositOrSwap(menu, player, cursor)) {
                    cursor.setAmount(cursor.getAmount() - 1);
                    event.getView().setCursor(cursor.isEmpty() ? null : cursor);
                }
            }
            case SHIFT_LEFT, SHIFT_RIGHT -> {
                ItemStack withdrawn = menu.withdraw(player);
                if (withdrawn != null) {
                    PersonalAnchorMenu.give(player, withdrawn);
                    TextUtil.msg(player, "Anchor removed.");
                }
            }
            default -> { }
        }
    }

    private void onOwnInventoryClick(InventoryClickEvent event, PersonalAnchorMenu menu, Player player) {
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }
        if (!event.isShiftClick()) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !depositOrSwap(menu, player, clicked)) {
            return;
        }

        ItemStack remaining = clicked.clone();
        remaining.setAmount(remaining.getAmount() - 1);
        event.setCurrentItem(remaining.isEmpty() ? null : remaining);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof PersonalAnchorMenu menu)
                || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int menuSize = event.getInventory().getSize();
        long slotsInMenu = event.getRawSlots().stream().filter(slot -> slot < menuSize).count();
        if (slotsInMenu == 0) {
            return;
        }

        event.setCancelled(true);
        if (slotsInMenu != 1 || !event.getRawSlots().contains(PersonalAnchorMenu.ANCHOR_SLOT)) {
            return;
        }

        Executors.syncPlayerDelayed(player, 1, () -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory().getHolder() != menu) {
                return;
            }

            ItemStack cursor = player.getItemOnCursor().clone();
            if (!depositOrSwap(menu, player, cursor)) {
                return;
            }

            cursor.setAmount(cursor.getAmount() - 1);
            player.setItemOnCursor(cursor.isEmpty() ? null : cursor);
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof PersonalAnchorMenu menu) || !(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (!player.isOnline()) {
            return;
        }

        PersonalBiomes.INSTANCE.flush(player);
        if (menu.activeBiomeChanged(player)) {
            PersonalBiomes.INSTANCE.refresh(player);
        }
    }

    private static boolean depositOrSwap(PersonalAnchorMenu menu, Player player, ItemStack candidate) {
        if (!menu.hasAnchor()) {
            boolean deposited = menu.deposit(player, candidate);
            if (deposited) {
                TextUtil.msg(player, "Deposited!");
                if (!PersonalBiomes.INSTANCE.state(player).enabled()) {
                    TextUtil.msg(player, "<gray>Use <white>/pbiome<gray> to switch your personal biome on.");
                }
            }
            return deposited;
        }

        ItemStack previous = menu.withdraw(player);
        if (previous == null) {
            return false;
        }
        if (!menu.deposit(player, candidate)) {
            menu.deposit(player, previous);
            return false;
        }

        PersonalAnchorMenu.give(player, previous);
        TextUtil.msg(player, "Anchor swapped.");
        if (!PersonalBiomes.INSTANCE.state(player).enabled()) {
            TextUtil.msg(player, "<gray>Use <white>/pbiome<gray> to switch your personal biome on.");
        }
        return true;
    }


}
