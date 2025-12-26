package net.lumamc.biomes.model;

import net.lumamc.biomes.PetiteBiomes;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public final class KeyedData<V> {


    public static final KeyedData<String> ANCHOR = new KeyedData<>("anchor", PersistentDataType.STRING);
    public static final KeyedData<String> ANCHOR_BLOCK = new KeyedData<>("anchor-block", PersistentDataType.STRING);
    public static final KeyedData<String> CHUNK_BIOME = new KeyedData<>("chunk-biome", PersistentDataType.STRING);


    private final NamespacedKey namespacedKey;
    private final PersistentDataType<V, V> type;

    public KeyedData(NamespacedKey namespacedKey, PersistentDataType<V, V> type) {
        this.namespacedKey = namespacedKey;
        this.type = type;
    }
    public KeyedData(String key, PersistentDataType<V, V> type) {
        this(new NamespacedKey(PetiteBiomes.instance(), key), type);
    }


    public boolean matches(PersistentDataHolder holder) {
        return holder.getPersistentDataContainer().has(namespacedKey);
    }

    public boolean matches(ItemStack item) {
        return item.hasItemMeta() && matches(item.getItemMeta());
    }

    @Nullable
    public V get(PersistentDataHolder holder) {
        return holder.getPersistentDataContainer().get(namespacedKey, type);
    }

    @Nullable
    public V get(ItemStack item) {
        if (item.hasItemMeta()) {
            return get(item.getItemMeta());
        }
        return null;
    }


    public void set(PersistentDataHolder holder, V value) {
        holder.getPersistentDataContainer().set(namespacedKey, type, value);
    }


    public void remove(PersistentDataHolder holder) {
        holder.getPersistentDataContainer().remove(namespacedKey);
    }

}
