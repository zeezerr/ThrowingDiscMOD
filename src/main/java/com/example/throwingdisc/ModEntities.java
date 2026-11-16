package com.example.throwingdisc;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static EntityType<ThrownDiscEntity> THROWN_DISC;

    private ModEntities() {}

    public static void init() {
        RegistryKey<EntityType<?>> key =
                RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(ThrowingDiscMod.MOD_ID, "thrown_disc"));
        THROWN_DISC = Registry.register(Registries.ENTITY_TYPE, key,
                EntityType.Builder.<ThrownDiscEntity>create(ThrownDiscEntity::new, SpawnGroup.MISC)
                        .dimensions(0.5f, 0.5f)
                        .maxTrackingRange(8)
                        .trackingTickInterval(1)
                        .build(key));
    }
}
