package com.example.throwingdisc;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public final class ModAdvancements {
    private ModAdvancements() {}

    /** Grants an advancement whose single criterion is called "hit" (minecraft:impossible trigger). */
    public static void grant(ServerWorld world, ServerPlayerEntity player, String name) {
        AdvancementEntry entry = world.getServer().getAdvancementLoader().get(Identifier.of(ThrowingDiscMod.MOD_ID, name));
        if (entry != null) player.getAdvancementTracker().grantCriterion(entry, "hit");
    }
}
