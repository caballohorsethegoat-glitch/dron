package com.dronmod.client;

import com.dronmod.DronMod;
import com.dronmod.entity.DroneEntity;
import com.dronmod.network.EscapePayload;
import com.dronmod.network.LockPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class DronClient implements ClientModInitializer {
    private static final float PRESS_GAIN = 0.07f;   // cuanto sube la barra por cada ESPACIO
    private static final float DECAY = 0.004f;       // cuanto baja sola por tick

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(DronMod.DRON, DroneRenderer::new);

        DroneEntity.clientTickHook = drone -> {
            if (!drone.soundStarted) {
                drone.soundStarted = true;
                MinecraftClient.getInstance().getSoundManager().play(new DroneSoundInstance(drone));
            }
        };

        ClientPlayNetworking.registerGlobalReceiver(LockPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.active()) DroneClientState.start(payload.droneId());
                    else DroneClientState.stop();
                }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DroneClientState.stop());
        ClientTickEvents.END_CLIENT_TICK.register(DronClient::tickLock);
        HudRenderCallback.EVENT.register(DronClient::renderLockHud);
    }

    private static void tickLock(MinecraftClient client) {
        if (!DroneClientState.active || client.player == null || client.world == null) return;

        Entity drone = client.world.getEntityById(DroneClientState.droneId);
        if (drone == null || !drone.isAlive()) {
            if (++DroneClientState.missingTicks > 40) DroneClientState.stop();
            return;
        }
        DroneClientState.missingTicks = 0;

        // Obliga a mirar al dron
        Vec3d from = client.player.getEyePos();
        Vec3d to = drone.getPos().add(0, drone.getHeight() * 0.5, 0);
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        float targetYaw = (float) (-MathHelper.atan2(dx, dz) * 180.0 / Math.PI);
        float targetPitch = (float) (-MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
        client.player.setYaw(client.player.getYaw() + MathHelper.wrapDegrees(targetYaw - client.player.getYaw()) * 0.4f);
        client.player.setPitch(MathHelper.clamp(client.player.getPitch()
                + (targetPitch - client.player.getPitch()) * 0.4f, -90.0f, 90.0f));

        // Cada pulsacion de ESPACIO llena la barra
        while (client.options.jumpKey.wasPressed()) {
            DroneClientState.progress += PRESS_GAIN;
            DroneClientState.lastPressMs = System.currentTimeMillis();
        }
        DroneClientState.progress = Math.max(0f, DroneClientState.progress - DECAY);

        if (DroneClientState.progress >= 1.0f) {
            ClientPlayNetworking.send(new EscapePayload());
            DroneClientState.stop();
        }
    }

    private static void renderLockHud(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
        if (!DroneClientState.active) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        int cx = w / 2;

        long now = System.currentTimeMillis();
        boolean recentlyPressed = now - DroneClientState.lastPressMs < 140;
        boolean down = recentlyPressed || (now / 400) % 2 == 0;   // animacion: la tecla sube y baja

        int keyW = 80, keyH = 18;
        int x = cx - keyW / 2;
        int y = h / 2 + 34;
        int off = down ? 3 : 0;

        ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("¡El dron te tiene! Pulsa ESPACIO repetidamente"),
                cx, y - 18, 0xFFFFFF);

        // tecla ESPACIO (base + tapa que se hunde)
        ctx.fill(x - 1, y + 3, x + keyW + 1, y + keyH + 4, 0xFF111111);
        ctx.fill(x, y + off, x + keyW, y + off + keyH, down ? 0xFFB5B5B5 : 0xFFEAEAEA);
        ctx.fill(x, y + off, x + keyW, y + off + 1, 0xFFFFFFFF);
        ctx.fill(x, y + off + keyH - 1, x + keyW, y + off + keyH, 0xFF8A8A8A);
        Text label = Text.literal("ESPACIO");
        int tw = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, label, cx - tw / 2, y + off + 5, 0xFF202020, false);

        // barra de progreso
        int bw = 120, bh = 8;
        int bx = cx - bw / 2;
        int by = y + keyH + 14;
        float p = Math.max(0f, Math.min(1f, DroneClientState.progress));
        ctx.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFFFFFFFF);
        ctx.fill(bx, by, bx + bw, by + bh, 0xFF000000);
        ctx.fill(bx, by, bx + (int) (bw * p), by + bh, 0xFF55FF55);
    }
}
