package com.example.throwingdisc;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Map<DiscTier, ThrowingDiscItem> DISCS = new EnumMap<>(DiscTier.class);

    private ModItems() {}

    public static ThrowingDiscItem get(DiscTier tier) {
        return DISCS.get(tier);
    }

    public static void init() {
        for (DiscTier tier : DiscTier.values()) {
            RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ThrowingDiscMod.MOD_ID, tier.id));

            List<Text> lore = List.of(
                    Text.literal("When thrown:").formatted(Formatting.GRAY).styled(s -> s.withItalic(false)),
                    Text.literal(" " + trim(tier.damage) + " Attack Damage").formatted(Formatting.DARK_GREEN).styled(s -> s.withItalic(false))
            );

            Item.Settings settings = new Item.Settings()
                    .registryKey(key)
                    .maxCount(1)
                    .maxDamage(tier.durability)
                    .enchantable(tier.enchantability)
                    .component(DataComponentTypes.LORE, new LoreComponent(lore));

            DISCS.put(tier, Registry.register(Registries.ITEM, key, new ThrowingDiscItem(settings, tier)));
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            for (DiscTier tier : DiscTier.values()) entries.add(DISCS.get(tier));
        });
    }

    private static String trim(float f) {
        return f == (int) f ? Integer.toString((int) f) : Float.toString(f);
    }
}
