package com.example.throwingdisc;

/** Stats for every disc variant (values taken from the reference sheet). */
public enum DiscTier {
    //          id                       damage durability enchant speed (blocks/tick)
    COPPER   ("copper_throwing_disc",    4.0f,  190,  13, 1.10),
    IRON     ("iron_throwing_disc",      5.0f,  250,  14, 1.20),
    GOLDEN   ("golden_throwing_disc",    3.5f,   30,  22, 1.70), // fastest flight
    DIAMOND  ("diamond_throwing_disc",   6.0f, 1560,  10, 1.30),
    NETHERITE("netherite_throwing_disc", 7.0f, 2030,  15, 1.40);

    public static final double RANGE = 16.0; // blocks flown before the disc turns around

    public final String id;
    public final float damage;
    public final int durability;
    public final int enchantability;
    public final double speed;

    DiscTier(String id, float damage, int durability, int enchantability, double speed) {
        this.id = id;
        this.damage = damage;
        this.durability = durability;
        this.enchantability = enchantability;
        this.speed = speed;
    }
}
