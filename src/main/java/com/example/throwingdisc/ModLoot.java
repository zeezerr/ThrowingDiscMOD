package com.example.throwingdisc;

import java.util.Map;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;

/** Adds discs to vanilla loot tables: diamond in vaults, netherite in bastions. */
public final class ModLoot {
    private record Entry(DiscTier tier, float chance) {}

    private static final Map<String, Entry> TABLES = Map.of(
            "minecraft:chests/trial_chambers/reward",         new Entry(DiscTier.DIAMOND, 0.10f),
            "minecraft:chests/trial_chambers/reward_ominous", new Entry(DiscTier.DIAMOND, 0.25f),
            "minecraft:chests/bastion_other",                 new Entry(DiscTier.NETHERITE, 0.04f), // rare
            "minecraft:chests/bastion_treasure",              new Entry(DiscTier.NETHERITE, 0.30f)  // much higher
    );

    private ModLoot() {}

    public static void init() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            Entry entry = TABLES.get(key.getValue().toString());
            if (entry == null) return;
            Item item = ModItems.get(entry.tier());
            tableBuilder.pool(LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .conditionally(RandomChanceLootCondition.builder(entry.chance()))
                    .with(ItemEntry.builder(item)));
        });
    }
}
