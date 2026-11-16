package com.example.throwingdisc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ThrowingDiscItem extends Item {
    public final DiscTier tier;

    public ThrowingDiscItem(Settings settings, DiscTier tier) {
        super(settings);
        this.tier = tier;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        return throwFrom(world, user, hand) ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    /** Throws the disc held in {@code hand}. Returns true if a disc was thrown (client side: "would throw"). */
    public static boolean throwFrom(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!(stack.getItem() instanceof ThrowingDiscItem item)) return false;
        if (!(world instanceof ServerWorld serverWorld)) return true;

        ThrownDiscEntity disc = new ThrownDiscEntity(ModEntities.THROWN_DISC, serverWorld);
        disc.setOwner(player);
        disc.init(item.tier, stack.copy(), hand == Hand.OFF_HAND, player.getInventory().getSelectedSlot());

        Vec3d look = player.getRotationVec(1.0f);
        disc.setPosition(player.getX() + look.x * 0.6, player.getEyeY() - 0.15 + look.y * 0.6, player.getZ() + look.z * 0.6);
        disc.setVelocity(look.multiply(item.tier.speed));
        serverWorld.spawnEntity(disc);

        serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.8f, 1.4f);
        // The disc "leaves" the slot; it flies back into it (like a Loyalty trident).
        player.setStackInHand(hand, ItemStack.EMPTY);
        return true;
    }
}
