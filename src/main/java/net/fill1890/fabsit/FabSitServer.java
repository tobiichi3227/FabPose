package net.fill1890.fabsit;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fill1890.fabsit.config.ConfigManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class FabSitServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        ServerPlayNetworking.registerGlobalReceiver(FabSit.fabsitChannel, FabSitServer::handleCheckResponse);

        // on player joins, ping them with a fabsit check packet to see if they have the mod loaded
        ServerPlayConnectionEvents.JOIN.register((ServerPlayNetworkHandler networkHandler, PacketSender sender, MinecraftServer server)
                -> sender.sendPacket(FabSit.fabsitChannel, PacketByteBufs.empty()));
    }

    // if the client has the mod loaded, add them to the supported player registry
    private static void handleCheckResponse(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler networkHandler, PacketByteBuf packetByteBuf, PacketSender sender) {
        server.execute(() -> ConfigManager.loadedPlayers.add(player));
    }
}