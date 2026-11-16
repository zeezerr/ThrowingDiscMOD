package com.example.throwingdisc;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * The flying disc. Server-authoritative: it flies straight (no gravity), pierces mobs,
 * ricochets off hard blocks (longer + faster), then slows, turns around and flies home.
 */
public class ThrownDiscEntity extends ThrownItemEntity {
    private enum Phase { OUT, SLOWING, RETURNING }

    private DiscTier tier = DiscTier.IRON;
    private ItemStack disc = ItemStack.EMPTY;
    private boolean offhand;
    private int mainSlot;

    private Phase phase = Phase.OUT;
    private double speed;
    private double range = DiscTier.RANGE;
    private double travelled;
    private double returnSpeed;
    private int bounces;
    private int age;
    private final Set<Integer> alreadyHit = new HashSet<>();
    private final Set<Integer> mobsKilled = new HashSet<>(); // for "Olympic Champion"

    public ThrownDiscEntity(EntityType<? extends ThrownDiscEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
    }

    public void init(DiscTier tier, ItemStack stack, boolean offhand, int mainSlot) {
        this.tier = tier;
        this.disc = stack;
        this.offhand = offhand;
        this.mainSlot = mainSlot;
        this.speed = tier.speed;
        this.setItem(stack.copy()); // only used for rendering
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.get(DiscTier.IRON);
    }

    /** A disc mid-air is not saved (it would lose its owner anyway). */
    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public void tick() {
        this.baseTick();
        if (!(this.getEntityWorld() instanceof ServerWorld world)) return; // positions are synced by the server
        if (disc.isEmpty()) { discard(); return; }
        if (++age > 300) { giveBack(world); return; } // safety net

        switch (phase) {
            case OUT -> {
                advance(world, true);
                if (travelled >= range) phase = Phase.SLOWING;
            }
            case SLOWING -> {
                setVelocity(getVelocity().multiply(0.6));
                advance(world, false);
                if (getVelocity().length() < 0.18) {
                    phase = Phase.RETURNING;
                    alreadyHit.clear(); // can hurt everything again on the way back
                    returnSpeed = 0.3;
                }
            }
            case RETURNING -> {
                Entity o = getOwner();
                if (!(o instanceof LivingEntity owner) || !owner.isAlive() || owner.getEntityWorld() != world) {
                    giveBack(world);
                    return;
                }
                Vec3d here = new Vec3d(getX(), getY(), getZ());
                Vec3d toOwner = owner.getEyePos().subtract(0, 0.4, 0).subtract(here);
                if (toOwner.length() < 1.3) { giveBack(world); return; }
                returnSpeed = Math.min(returnSpeed + 0.12, Math.max(speed, 1.0));
                setVelocity(toOwner.normalize().multiply(Math.min(returnSpeed, toOwner.length())));
                advance(world, false);
            }
        }
    }

    /** Moves one step, hitting entities along the path and (optionally) bouncing off blocks. */
    private void advance(ServerWorld world, boolean collideWithBlocks) {
        Vec3d from = new Vec3d(getX(), getY(), getZ());
        Vec3d vel = getVelocity();
        Vec3d to = from.add(vel);

        BlockHitResult blockHit = null;
        if (collideWithBlocks) {
            BlockHitResult r = world.raycast(new RaycastContext(from, to,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
            if (r.getType() == HitResult.Type.BLOCK) {
                if (world.getBlockState(r.getBlockPos()).isOf(Blocks.DECORATED_POT)) {
                    world.breakBlock(r.getBlockPos(), true, getOwner()); // smash pots and keep flying
                } else {
                    blockHit = r;
                }
            }
        }

        Vec3d end = blockHit != null ? blockHit.getPos() : to;
        hitEntities(world, from, end);
        if (isRemoved()) return;
        travelled += from.distanceTo(end);

        if (blockHit != null) ricochet(world, blockHit, vel);
        else setPosition(end);
    }

    private void ricochet(ServerWorld world, BlockHitResult hit, Vec3d vel) {
        Vec3d n = new Vec3d(hit.getSide().getOffsetX(), hit.getSide().getOffsetY(), hit.getSide().getOffsetZ());
        Vec3d reflected = vel.subtract(n.multiply(2 * vel.dotProduct(n)));
        speed = Math.min(speed * 1.25, 3.0);          // faster after every bounce
        range += 6.0;                                  // ...and flies further
        bounces++;
        if (bounces >= 8) range = travelled;           // cap so it cannot bounce forever
        setVelocity(reflected.normalize().multiply(speed));
        setPosition(hit.getPos().add(n.multiply(0.15)));
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 0.8f, 1.6f);
    }

    private void hitEntities(ServerWorld world, Vec3d from, Vec3d to) {
        Box sweep = new Box(from, to).expand(0.5);
        for (Entity e : world.getOtherEntities(this, sweep, this::canHit)) {
            Box box = e.getBoundingBox().expand(0.3);
            if (!box.contains(from) && box.raycast(from, to).isEmpty()) continue;

            alreadyHit.add(e.getId());
            LivingEntity target = (LivingEntity) e;
            Entity owner = getOwner();
            DamageSource source = getDamageSources().thrown(this, owner);
            if (target.damage(world, source, tier.damage)) {
                Vec3d dir = getVelocity().normalize();
                target.takeKnockback(0.5, -dir.x, -dir.z);
                if (target.isDead() && target instanceof MobEntity && mobsKilled.add(e.getId())
                        && mobsKilled.size() >= 5 && owner instanceof ServerPlayerEntity killer) {
                    ModAdvancements.grant(world, killer, "olympic_champion");
                }
                world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 0.8f, 1.2f);
                disc.damage(1, world, owner instanceof ServerPlayerEntity sp ? sp : null, item -> {});
                if (disc.isEmpty()) { // broke
                    world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.8f, 1.0f);
                    discard();
                    return;
                }
            }
        }
    }

    private boolean canHit(Entity e) {
        if (!(e instanceof LivingEntity) || !e.isAlive() || e.isSpectator() || alreadyHit.contains(e.getId())) return false;
        Entity owner = getOwner();
        if (e == owner) return false;
        return !(owner instanceof PlayerEntity p && e instanceof PlayerEntity t && !p.shouldDamagePlayer(t));
    }

    /** Puts the disc back into the slot it left; drops it if the owner is gone. */
    private void giveBack(ServerWorld world) {
        if (!disc.isEmpty()) {
            if (getOwner() instanceof ServerPlayerEntity p && p.isAlive()) {
                if (offhand) {
                    if (p.getOffHandStack().isEmpty()) p.setStackInHand(Hand.OFF_HAND, disc);
                    else p.getInventory().offerOrDrop(disc);
                } else if (p.getInventory().getStack(mainSlot).isEmpty()) {
                    p.getInventory().setStack(mainSlot, disc);
                } else {
                    p.getInventory().offerOrDrop(disc);
                }
                world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.4f, 1.6f);
            } else {
                dropStack(world, disc);
            }
            disc = ItemStack.EMPTY;
        }
        discard();
    }
}
