package dev.lumas.biomes.model;

import dev.lumas.biomes.LittleBiomes;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public final class KeyedData<P, V> {


    public static final KeyedData<String, String> ANCHOR = new KeyedData<>("anchor", PersistentDataType.STRING);
    public static final KeyedData<String, String> ANCHOR_BLOCK = new KeyedData<>("anchor-block", PersistentDataType.STRING);
    public static final KeyedData<String, String> CHUNK_BIOME = new KeyedData<>("chunk-biome", PersistentDataType.STRING);

    public static final KeyedData<String, String> PERSONAL_BIOME = new KeyedData<>("personal-biome", PersistentDataType.STRING);
    public static final KeyedData<Byte, Boolean> PERSONAL_BIOME_ENABLED = new KeyedData<>("personal-biome-enabled", PersistentDataType.BOOLEAN);


    private final NamespacedKey namespacedKey;
    private final PersistentDataType<P, V> type;

    public KeyedData(NamespacedKey namespacedKey, PersistentDataType<P, V> type) {
        this.namespacedKey = namespacedKey;
        this.type = type;
    }
    public KeyedData(String key, PersistentDataType<P, V> type) {
        this(new NamespacedKey(LittleBiomes.instance(), key), type);
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
