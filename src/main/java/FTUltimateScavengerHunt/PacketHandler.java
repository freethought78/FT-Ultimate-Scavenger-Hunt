package FTUltimateScavengerHunt;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PacketHandler {
    // The packet handler now accepts both the packet and the context supplier
    public static void onPacketReceived(CustomPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        System.out.println("--- PACKET RECEIVED");

        // Get the context for the network event (e.g., for networking synchronization)
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Process the received packet here
            CompoundTag data = packet.getData();
            System.out.println("Received custom packet with data: " + data);

            // Handle isHuntStarted status
            if (data.contains("isHuntStarted")) {
                FTUltimateScavengerHunt.isHuntStarted = data.getBoolean("isHuntStarted");
            }

            // Handle masterPlayerProgress
            if (data.contains("masterPlayerProgress", Tag.TAG_COMPOUND)) {
                CompoundTag progressData = data.getCompound("masterPlayerProgress");
                ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> decodedProgress = new ConcurrentHashMap<>();

                for (String playerName : progressData.getAllKeys()) {
                    CompoundTag playerProgressTag = progressData.getCompound(playerName);
                    ConcurrentHashMap<String, Boolean> playerProgress = new ConcurrentHashMap<>();

                    for (String taskName : playerProgressTag.getAllKeys()) {
                        playerProgress.put(taskName, playerProgressTag.getBoolean(taskName));
                    }

                    decodedProgress.put(playerName, playerProgress);
                }

                // Update the PlayerProgress.masterPlayerProgress
                PlayerProgressManager.masterPlayerProgress = decodedProgress;
                System.out.println("Updated masterPlayerProgress: " + PlayerProgressManager.masterPlayerProgress);
            }
            
            // Handle leaderboard data (new functionality)
            if (data.contains("leaderboard", Tag.TAG_LIST)) {
                // Deserialize the leaderboard list
                ListTag leaderboardList = data.getList("leaderboard", Tag.TAG_COMPOUND);

                // Clear the current leaderboard (replace with new data)
                LeaderboardManager.leaderboard.clear();

                // Iterate through the leaderboard entries
                for (Tag entryTag : leaderboardList) {
                    CompoundTag entryData = (CompoundTag) entryTag;

                    // Extract player name and completion count
                    String playerName = entryData.getString("playerName");
                    int completionCount = entryData.getInt("completionCount");

                    // Create a new LeaderboardEntry and add it to the leaderboard
                    LeaderboardManager.LeaderboardEntry entry = new LeaderboardManager.LeaderboardEntry(playerName, completionCount);
                    LeaderboardManager.leaderboard.add(entry);
                }

                // Optionally, print the updated leaderboard
                System.out.println("Updated leaderboard: " + LeaderboardManager.getLeaderboard());
            }
            
            // Set BlockPlayerInteraction.spawnProtectionRadius
            if (data.contains("spawnProtectionRadius", Tag.TAG_INT)) {
                int spawnProtectionRadius = data.getInt("spawnProtectionRadius");
                int spawnPosX = data.getInt("spawnPosX");
                int spawnPosY = data.getInt("spawnPosY");
                int spawnPosZ = data.getInt("spawnPosZ");
                
                FTUltimateScavengerHunt.spawnProtectionRadius = spawnProtectionRadius; 
                
                FTUltimateScavengerHunt.defaultSpawnPosition = new BlockPos(spawnPosX, spawnPosY, spawnPosZ);
                
                System.out.println("Updated spawn protection radius: " + spawnProtectionRadius);
            }
        });

        // Mark the event as handled
        context.setPacketHandled(true);
    }
}
