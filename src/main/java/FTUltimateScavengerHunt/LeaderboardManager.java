package FTUltimateScavengerHunt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.minecraft.server.MinecraftServer;

public class LeaderboardManager {
    // Data object to store leaderboard: List of LeaderboardEntry objects
    private static List<LeaderboardEntry> leaderboard = new ArrayList<>();

    // Class to store leaderboard entries
    public static class LeaderboardEntry {
        public final String playerName;
        public final int completionCount;

        public LeaderboardEntry(String playerName, int completionCount) {
            this.playerName = playerName;
            this.completionCount = completionCount;
        }
    }

    // Function to update the leaderboard based on masterPlayerProgress
    public static void updateLeaderboard(MinecraftServer server) {
        List<LeaderboardEntry> updatedLeaderboard = PlayerProgressManager.masterPlayerProgress.entrySet().stream()
            .map(entry -> {
                String playerName = entry.getKey(); // Use player names directly
                ConcurrentHashMap<String, Boolean> progress = entry.getValue();
                int completionCount = (int) progress.values().stream().filter(Boolean::booleanValue).count();
                return new LeaderboardEntry(playerName, completionCount);
            })
            .sorted(Comparator.comparingInt((LeaderboardEntry e) -> e.completionCount).reversed()) // Sort by completion count, descending
            .collect(Collectors.toList());

        // Update the leaderboard
        leaderboard = updatedLeaderboard;
    }

    public static List<LeaderboardEntry> getLeaderboard() {
        return new ArrayList<>(leaderboard); // Return a copy of the leaderboard list
    }

    public static void cleanUpForShutDown() {
        leaderboard.clear(); // Clears the leaderboard list
    }
}
