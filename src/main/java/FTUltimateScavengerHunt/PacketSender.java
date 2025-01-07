package FTUltimateScavengerHunt;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.network.NetworkDirection;

public class PacketSender {

    // Method to send a custom packet to a specific player
    public static void sendPacketToPlayer(ServerPlayer player, CompoundTag data) {
        CustomPacket packet = new CustomPacket(data); // Create your packet

        // Send the custom packet directly via the network channel to the player
        FTUltimateScavengerHunt.CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);

        System.out.println("+++ SENDING PACKET to Player: " + player.getName().getString());
    }

    // Method to send a custom packet to all connected players
    public static void sendPacketToAllPlayers(MinecraftServer server, CompoundTag data) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendPacketToPlayer(player, data);
        }
    }

    // Create a packet to send the status of isHuntStarted to all players
    public static void sendHuntStartedStatusPacket(Boolean status, MinecraftServer server) {
        // Create a CompoundTag to hold the status data
        CompoundTag data = new CompoundTag();
        data.putString("Type", "isHuntStartedPacket");
        data.putBoolean("isHuntStarted", status); // Store the hunt status

        // Send the packet with the status to all players
        sendPacketToAllPlayers(server, data);
    }
}
