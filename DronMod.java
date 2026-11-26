package com.dronmod;

import com.dronmod.entity.DroneEntity;
import com.dronmod.network.EscapePayload;
import com.dronmod.network.LockPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public class DronMod implements ModInitializer {
    public static final String MOD_ID = "dronmod";

    public static final EntityType<DroneEntity> DRON = Registry.register(
            Registries.ENTITY_TYPE,
            id("dron"),
            EntityType.Builder.create(DroneEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.0f, 0.7f)
                    .maxTrackingRange(10)
                    .build("dron"));

    public static final Item DRON_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            id("dron_spawn_egg"),
            new SpawnEggItem(DRON, 0xFFFFFF, 0xFFFFFF, new Item.Settings()));

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.init();
        PayloadTypeRegistry.playS2C().register(LockPayload.ID, LockPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EscapePayload.ID, EscapePayload.CODEC);

        FabricDefaultAttributeRegistry.register(DRON, DroneEntity.createDroneAttributes());

        // El jugador lleno la barra: el dron (o drones) que lo tienen agarrado lo sueltan y huyen
        ServerPlayNetworking.registerGlobalReceiver(EscapePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            Box box = player.getBoundingBox().expand(40.0);
            for (DroneEntity d : player.getServerWorld().getEntitiesByClass(DroneEntity.class, box, e -> e.isGrabbing(player))) {
                d.releasePlayer(true);
            }
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e -> e.add(DRON_SPAWN_EGG));
    }
}
