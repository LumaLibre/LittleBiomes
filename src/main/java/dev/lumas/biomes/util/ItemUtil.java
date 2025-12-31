package dev.lumas.biomes.util;

import lombok.NoArgsConstructor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@NoArgsConstructor
public final class ItemUtil {

    public static ItemStack editMeta(ItemStack itemStack, EditMeta editMeta) {
        if (itemStack == null) {
            return null;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }
        editMeta.edit(itemMeta);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @FunctionalInterface
    public interface EditMeta {
        void edit(ItemMeta meta);
    }
}
