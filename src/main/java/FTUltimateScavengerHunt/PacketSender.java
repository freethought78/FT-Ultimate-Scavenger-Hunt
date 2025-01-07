package FTUltimateScavengerHunt;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
        data.putBoolean("isHuntStarted", status); // Store the hunt status

        // Send the packet with the status to all players
        sendPacketToAllPlayers(server, data);
    }
    
    // Method to send the MasterPlayerProgress as a packet
    public static void sendMasterPlayerProgressPacket(ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> masterPlayerProgress, MinecraftServer server) {
        // Serialize the masterPlayerProgress data into a CompoundTag
        CompoundTag data = new CompoundTag();
        CompoundTag progressData = new CompoundTag();

        // Iterate through the masterPlayerProgress map and serialize it
        for (String playerName : masterPlayerProgress.keySet()) {
            CompoundTag playerProgress = new CompoundTag();
            ConcurrentHashMap<String, Boolean> playerTasks = masterPlayerProgress.get(playerName);

            for (String task : playerTasks.keySet()) {
                playerProgress.putBoolean(task, playerTasks.get(task)); // Serialize task completion status
            }

            progressData.put(playerName, playerProgress); // Add each player's progress to the tag
        }

        data.put("masterPlayerProgress", progressData); // Store the progress data in the main tag

        // Send the serialized data as a packet to all players
        sendPacketToAllPlayers(server, data);
    }
    
    public static void sendLeaderboardPacket(List<LeaderboardManager.LeaderboardEntry> leaderboard, MinecraftServer server) {
        // Create a CompoundTag to hold the leaderboard data
        CompoundTag data = new CompoundTag();

        // Create a list tag to hold all leaderboard entries
        ListTag leaderboardList = new ListTag();

        // Iterate through the leaderboard and serialize each entry without altering the data
        for (LeaderboardManager.LeaderboardEntry entry : leaderboard) {
            CompoundTag entryData = new CompoundTag();
            entryData.putString("playerName", entry.playerName);
            entryData.putInt("completionCount", entry.completionCount);

            // Add each entry's data to the list
            leaderboardList.add(entryData);
        }

        // Add the leaderboard list to the main data tag
        data.put("leaderboard", leaderboardList);

        // Send the leaderboard packet to all players
        sendPacketToAllPlayers(server, data);
    }


}
