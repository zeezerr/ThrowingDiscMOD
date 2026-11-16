package com.example.throwingdisc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public class ThrowingDiscClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Renders the disc using the item's own model/texture.
        EntityRendererRegistry.register(ModEntities.THROWN_DISC, FlyingItemEntityRenderer::new);
    }
}
