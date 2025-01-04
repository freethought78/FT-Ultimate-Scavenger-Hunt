package FTUltimateScavengerHunt;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

public class PlayerProgressManager {
    // Master map to hold all player progress
    static ConcurrentHashMap<UUID, ConcurrentHashMap<String, Boolean>> masterPlayerProgress = new ConcurrentHashMap<>();

    private static final Gson gson = new Gson(); // Gson instance for serialization and deserialization

 // Load all player progress files into the master map
    public static void loadAllPlayerProgress(MinecraftServer server) {
        // Get the world name to construct the path for player progress directory
        String worldName = server.getWorldData().getLevelName();
        
        // Create a LevelResource for the player progress directory based on world
        LevelResource playerProgressDir = new LevelResource(worldName + "/playerprogress");

        // Get the path for the player progress folder
        File progressFolder = server.getWorldPath(playerProgressDir).toFile();

        // Create the directory if it doesn't exist
        if (!progressFolder.exists() || !progressFolder.isDirectory()) {
            progressFolder.mkdirs();
        }

        // Iterate through each file in the folder and load its contents
        for (File file : progressFolder.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                try {
                    // Get the player UUID from the file name
                    UUID playerId = UUID.fromString(file.getName().replace(".json", ""));
                    // Load the player's progress from the file
                    ConcurrentHashMap<String, Boolean> progress = loadPlayerProgressFromFile(server, playerId);
                    // Add the loaded progress to the master map
                    masterPlayerProgress.put(playerId, progress);
                } catch (Exception e) {
                    System.err.println("Failed to load progress for file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }


 // Load the player's progress from a file
    public static ConcurrentHashMap<String, Boolean> loadPlayerProgressFromFile(MinecraftServer server, UUID playerId) {
        String worldName = server.getWorldData().getLevelName();
        LevelResource playerProgressDir = new LevelResource(worldName + "/playerprogress");
        File progressFolder = server.getWorldPath(playerProgressDir).toFile();
        File progressFile = new File(progressFolder, playerId + ".json");

        if (progressFile.exists()) {
            try (FileReader reader = new FileReader(progressFile)) {
                // Deserialize the progress map from the file
                ConcurrentHashMap<String, Boolean> progress = gson.fromJson(reader, ConcurrentHashMap.class);
                if (progress != null) {
                    return progress;  // Return the loaded progress
                } else {
                    System.err.println("Error: Failed to load progress for player: " + playerId);
                }
            } catch (IOException e) {
                System.err.println("Error reading player progress file: " + progressFile.getPath());
                e.printStackTrace();
            }
        } else {
            // Handle the case where no progress file exists
            System.err.println("No progress file found for player: " + playerId);
        }

        return null;  // Return null if no progress is found or an error occurred
    }



    // Save the player's progress to a file
    public static void savePlayerProgressToFile(UUID playerId, MinecraftServer server) {
        ConcurrentHashMap<String, Boolean> progress = masterPlayerProgress.get(playerId);

        if (progress == null) {
            System.err.println("No progress found for player: " + playerId);
            return;
        }

        // Get the world name using getWorldData().getLevelName()
        String worldName = server.getWorldData().getLevelName();

        // Create a LevelResource for the player progress directory
        LevelResource playerProgressDir = new LevelResource(worldName + "/playerprogress");

        // Get the path for the player progress folder
        File progressFolder = server.getWorldPath(playerProgressDir).toFile();

        // Create the playerprogress folder inside the world folder if it doesn't exist
        if (!progressFolder.exists()) {
            progressFolder.mkdirs();  // Create the directory if it doesn't exist
        }

        // Define the player progress file
        File progressFile = new File(progressFolder, playerId + ".json");

        try (FileWriter writer = new FileWriter(progressFile)) {
            // Serialize the progress map and write it to the file
            gson.toJson(progress, writer);
        } catch (IOException e) {
            System.err.println("Error saving player progress for: " + playerId);
            e.printStackTrace();
        }
    }






    public static void initializePlayerProgress(UUID playerId, MinecraftServer server) {
        // Get the world name
        String worldName = server.getWorldData().getLevelName();

        // Get the player progress directory path
        LevelResource playerProgressDir = new LevelResource(worldName + "/playerprogress");
        File progressFolder = server.getWorldPath(playerProgressDir).toFile();

        // Ensure the player progress folder exists
        if (!progressFolder.exists()) {
            progressFolder.mkdirs();
        }

        // Define the player's progress file
        File progressFile = new File(progressFolder, playerId + ".json");

        // Load player progress from file, if it exists
        ConcurrentHashMap<String, Boolean> existingProgress = loadPlayerProgressFromFile(server, playerId);

        // If no existing progress is found, initialize it
        if (existingProgress == null) {
            ConcurrentHashMap<String, Boolean> progress = new ConcurrentHashMap<>();
            
            // Load the checklist for this world (ensure master_checklist.json is loaded first)
            Set<String> checklist = FTUltimateScavengerHunt.loadMasterChecklist(server);
            
            // Initialize progress map with all checklist items set to false (not completed)
            for (String item : checklist) {
                progress.put(item, false);  // Mark the item as not completed
            }

            // Add the player's progress to the master map
            masterPlayerProgress.put(playerId, progress);

            // Save the initial progress to the player's file
            savePlayerProgressToFile(playerId, server);

            // Give the FTQuestHubBlock to the player since it's their first time
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                giveFTQuestHubBlockToPlayer(player);
            }
        } else {
            // If the progress file exists, update the global map with the loaded progress
            masterPlayerProgress.put(playerId, existingProgress);
        }
    }




    
 // Method to give the FTQuestHubBlock to the player
    private static void giveFTQuestHubBlockToPlayer(Player player) {
        // Define the FTQuestHubBlock as an ItemStack (assuming you have a method to get the block item)
        ItemStack questHubBlockItem = new ItemStack(FTUltimateScavengerHunt.FT_QUEST_HUB_BLOCK.get()); // Assuming you have registered FT_QUEST_HUB_BLOCK
        
        // Add the block to the player's inventory
        if (!player.getInventory().add(questHubBlockItem)) {
            // If the inventory is full, drop the block at the player's feet
            BlockPos pos = player.blockPosition();
            player.getLevel().addFreshEntity(new ItemEntity(player.getLevel(), pos.getX(), pos.getY(), pos.getZ(), questHubBlockItem));
        }
    }

    
    

 // Example: Method to update progress when an item is turned in
    public static void updatePlayerProgress(UUID playerId, String item, boolean turnedIn, MinecraftServer server) {
        ConcurrentHashMap<String, Boolean> progress = masterPlayerProgress.get(playerId);
        if (progress == null) {
            // If no progress is found, initialize it
            initializePlayerProgress(playerId, server);
            progress = masterPlayerProgress.get(playerId);
        }
        
        // Update the progress for the item
        progress.put(item, turnedIn);
        // Save the progress after the update
        savePlayerProgressToFile(playerId, server);  // Ensure the server instance is passed
    }

    // Utility: Check the progress of a player
    public static boolean getPlayerProgress(UUID playerId, String item) {
        ConcurrentHashMap<String, Boolean> progress = masterPlayerProgress.get(playerId);
        return progress != null && progress.getOrDefault(item, false);
    }
    
    // Method to save progress for all players to their individual files
    public static void saveAllPlayerProgressToFiles(MinecraftServer server) {
        // Iterate over all players in the master progress map and save their progress
        for (UUID playerId : masterPlayerProgress.keySet()) {
            savePlayerProgressToFile(playerId, server);
        }
    }
    
 // Method to check if an item is part of the player's progress checklist
    public static boolean isItemInChecklist(UUID playerId, String itemName) {
        ConcurrentHashMap<String, Boolean> progress = masterPlayerProgress.get(playerId);
        return progress != null && progress.containsKey(itemName);
    }
    
 // Method to check if an item is complete for the player
    public static boolean isItemComplete(UUID playerId, String itemName) {
        ConcurrentHashMap<String, Boolean> progress = masterPlayerProgress.get(playerId);
        return progress != null && progress.getOrDefault(itemName, false);
    }
    
 // Method to mark an item as complete for the player
    public static void markItemComplete(MinecraftServer server, UUID playerId, String itemName) {
        ConcurrentHashMap<String, Boolean> progress = masterPlayerProgress.get(playerId);
        if (progress != null && progress.containsKey(itemName) && !progress.get(itemName)) {
            // Mark the item as completed
            progress.put(itemName, true);
            // Save the updated progress to file
            savePlayerProgressToFile(playerId, server);
        }
    }
    
 // Method to check if the player has completed all items in the scavenger hunt
    public static boolean isPlayerComplete(UUID playerId) {
        ConcurrentHashMap<String, Boolean> progress = masterPlayerProgress.get(playerId);

        // Ensure progress exists for the player
        if (progress != null) {
            // Check if all items are completed
            for (Boolean completed : progress.values()) {
                if (!completed) {
                    return false; // If any item is not completed, the player is not complete
                }
            }
            return true; // Player has completed all items
        }
        return false; // Player progress not found
    }


	public static void cleanUpForShutDown(MinecraftServer server) {
		saveAllPlayerProgressToFiles(server);
		masterPlayerProgress.clear();
	}

}
