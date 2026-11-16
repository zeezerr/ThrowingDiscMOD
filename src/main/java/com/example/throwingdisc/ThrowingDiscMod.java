package com.example.throwingdisc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class ThrowingDiscMod implements ModInitializer {
    public static final String MOD_ID = "throwingdisc";

    @Override
    public void onInitialize() {
        ModItems.init();
        ModEntities.init();
        ModLoot.init();

        // Crouch + use with a tool/sword in the main hand throws the OFF-HAND disc.
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand == Hand.MAIN_HAND && player.isSneaking() && !player.isSpectator()) {
                ItemStack main = player.getMainHandStack();
                if (main.isDamageable() && !(main.getItem() instanceof ThrowingDiscItem)
                        && player.getOffHandStack().getItem() instanceof ThrowingDiscItem) {
                    ThrowingDiscItem.throwFrom(world, player, Hand.OFF_HAND);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });

        // Crouch + attack an entity (any main hand) throws the OFF-HAND disc instead of hitting.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand == Hand.MAIN_HAND && player.isSneaking() && !player.isSpectator()
                    && player.getOffHandStack().getItem() instanceof ThrowingDiscItem) {
                ThrowingDiscItem.throwFrom(world, player, Hand.OFF_HAND);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}
