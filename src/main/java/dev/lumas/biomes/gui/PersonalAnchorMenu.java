package dev.lumas.biomes.gui;

import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.configuration.OkaeriLittleBiome;
import dev.lumas.biomes.model.KeyedData;
import dev.lumas.biomes.model.PersonalBiomes;
import dev.lumas.biomes.util.TextUtil;
import dev.wyck.keys.ResourceKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PersonalAnchorMenu implements InventoryHolder {

    public static final int ANCHOR_SLOT = 13;

    private static final int MENU_ROWS = 3;
    private static final int ROW_WIDTH = 9;
    private static final int MENU_SIZE = MENU_ROWS * ROW_WIDTH;

    private static final String TITLE = "<dark_gray><b>Personal Biome Anchor";

    private static final Material EMPTY_SLOT_MATERIAL = Material.ANVIL;
    private static final String EMPTY_SLOT_NAME = "<gray>Empty Slot";
    private static final List<String> EMPTY_SLOT_LORE = List.of(
            "<dark_gray>Place a biome anchor here and close",
            "<dark_gray>this menu to use it on a personal biome.",
            "",
            "<dark_gray>Toggle it with <gray>/pbiome<dark_gray>."
    );

    private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

    private static final Material[] DECORATION_ROW = {
            Material.GREEN_STAINED_GLASS_PANE,
            Material.BUSH,
            Material.FERN,
            Material.BUSH,
            Material.TORCHFLOWER,
            Material.BUSH,
            Material.FERN,
            Material.BUSH,
            Material.GREEN_STAINED_GLASS_PANE
    };

    private final Inventory inventory;
    private final @Nullable ResourceKey activeWhenOpened;
    private @Nullable ResourceKey storedAnchor;

    private PersonalAnchorMenu(Player player) {
        this.storedAnchor = PersonalBiomes.INSTANCE.state(player).anchorKey();
        this.activeWhenOpened = PersonalBiomes.INSTANCE.activeBiome(player);
        this.inventory = Bukkit.createInventory(this, MENU_SIZE, TextUtil.minimessage(TITLE));

        decorate();
        renderAnchorSlot();
    }

    public static void open(Player player) {
        PersonalAnchorMenu menu = new PersonalAnchorMenu(player);

        if (menu.storedAnchor != null && LittleBiomes.okaeriConfig().getLittleBiome(menu.storedAnchor) == null) {
            TextUtil.msg(player, "<red>Your stored anchor's biome (<gray>%s<!b><red>) no longer exists. Let a staff member know!".formatted(
                    menu.storedAnchor));
        }

        player.openInventory(menu.getInventory());
    }


    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    @Nullable
    public ResourceKey storedAnchor() {
        return this.storedAnchor;
    }

    public boolean hasAnchor() {
        return this.storedAnchor != null;
    }

    public boolean activeBiomeChanged(Player player) {
        return !Objects.equals(this.activeWhenOpened, PersonalBiomes.INSTANCE.activeBiome(player));
    }


    @Nullable
    public ItemStack withdraw(Player player) {
        if (this.storedAnchor == null) {
            return null;
        }

        OkaeriLittleBiome littleBiome = LittleBiomes.okaeriConfig().getLittleBiome(this.storedAnchor);
        if (littleBiome == null) {
            TextUtil.msg(player, "<red>That anchor's biome no longer exists, so it can't be handed back yet.");
            return null;
        }

        this.storedAnchor = null;
        PersonalBiomes.INSTANCE.setAnchor(player, null);
        renderAnchorSlot();
        return littleBiome.anchorItem();
    }

    public boolean deposit(Player player, @Nullable ItemStack candidate) {
        if (this.storedAnchor != null || candidate == null || !KeyedData.ANCHOR.matches(candidate)) {
            return false;
        }

        String serializedKey = KeyedData.ANCHOR.get(candidate);
        if (serializedKey == null) {
            return false;
        }

        ResourceKey anchorKey;
        try {
            anchorKey = ResourceKey.fromString(serializedKey);
        } catch (RuntimeException e) {
            return false;
        }

        if (LittleBiomes.okaeriConfig().getLittleBiome(anchorKey) == null) {
            TextUtil.msg(player, "<red>That anchor's biome is not configured on this server.");
            return false;
        }

        this.storedAnchor = anchorKey;
        PersonalBiomes.INSTANCE.setAnchor(player, anchorKey);
        renderAnchorSlot();
        return true;
    }


    private void renderAnchorSlot() {
        OkaeriLittleBiome littleBiome = this.storedAnchor == null
                ? null
                : LittleBiomes.okaeriConfig().getLittleBiome(this.storedAnchor);

        this.inventory.setItem(ANCHOR_SLOT, littleBiome != null ? littleBiome.anchorItem() : emptySlotItem());
    }

    private void decorate() {
        ItemStack filler = decoration(FILLER);
        for (int slot = 0; slot < MENU_SIZE; slot++) {
            this.inventory.setItem(slot, filler);
        }

        for (int column = 0; column < ROW_WIDTH; column++) {
            ItemStack decoration = decoration(DECORATION_ROW[column]);
            this.inventory.setItem(column, decoration);
            this.inventory.setItem(MENU_SIZE - ROW_WIDTH + column, decoration);
        }
    }

    private static ItemStack emptySlotItem() {
        ItemStack itemStack = new ItemStack(EMPTY_SLOT_MATERIAL);
        itemStack.editMeta(meta -> {
            meta.displayName(TextUtil.minimessage("<!i>" + EMPTY_SLOT_NAME));
            meta.lore(EMPTY_SLOT_LORE.stream()
                    .map(line -> TextUtil.minimessage("<!i>" + line))
                    .toList());
        });
        return itemStack;
    }

    private static ItemStack decoration(Material material) {
        ItemStack itemStack = new ItemStack(material);
        itemStack.editMeta(meta -> {
            meta.displayName(Component.empty());
            meta.addItemFlags(ItemFlag.values());
        });
        return itemStack;
    }

    public static void give(Player player, ItemStack itemStack) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
}
