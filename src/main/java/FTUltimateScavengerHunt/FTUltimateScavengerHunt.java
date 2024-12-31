package FTUltimateScavengerHunt;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;

@Mod("ftultimatescavengerhunt")
@Mod.EventBusSubscriber(modid = "ftultimatescavengerhunt", bus = Mod.EventBusSubscriber.Bus.FORGE)

public class FTUltimateScavengerHunt {
    // Define a mod id in a common place for everything to reference
    public static final String MODID = "ftultimatescavengerhunt";
	
    // Deferred Registers
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

 // Block Registration
    public static final RegistryObject<Block> FT_QUEST_HUB_BLOCK = BLOCKS.register("ft_quest_hub",
            () -> new FTQuestHubBlock());  // Use your custom block class here

    // BlockItem Registration
    public static final RegistryObject<BlockItem> FT_QUEST_HUB_BLOCK_ITEM = ITEMS.register("ft_quest_hub",
            () -> new BlockItem(FT_QUEST_HUB_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));



    private static final Logger LOGGER = LogUtils.getLogger();
    //public static Map<String, Boolean> itemChecklist = new HashMap<>();
    
    // Master checklist for the current world
    private static final Set<String> masterChecklist = new HashSet<>();

    // Player progress (UUID -> checklist progress)
    public static final Map<UUID, Map<String, Boolean>> playerProgress = new HashMap<>();
    
    public FTUltimateScavengerHunt() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register blocks and items
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
    
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("SERVER STARTED ***");
        initializeMasterChecklist(event.getServer());
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        initializePlayerProgress(event.getPlayer().getServer(), event.getPlayer().getUUID());
    }
    
   
    private static void saveMasterChecklist(MinecraftServer server) {
        // Save `masterChecklist` within the world folder
        Path worldFolderPath = server.getWorldPath(LevelResource.ROOT).toAbsolutePath();
        Path checklistPath = worldFolderPath.resolve("master_checklist.json");

        try {
            // Serialize the master checklist to the JSON file in the world folder
            Files.write(checklistPath, new Gson().toJson(masterChecklist).getBytes());
            LOGGER.info("Master checklist saved to " + checklistPath);
        } catch (IOException e) {
            LOGGER.error("Failed to save master checklist", e);
        }
    }
    
    private static Set<String> loadMasterChecklist(MinecraftServer server) {
        // Load `masterChecklist` from the world folder
        Path worldFolderPath = server.getWorldPath(LevelResource.ROOT).toAbsolutePath();
        Path checklistPath = worldFolderPath.resolve("master_checklist.json");

        if (Files.exists(checklistPath)) {
            try {
                String json = new String(Files.readAllBytes(checklistPath));
                return new Gson().fromJson(json, new TypeToken<Set<String>>() {}.getType());
            } catch (IOException e) {
                LOGGER.error("Failed to load master checklist", e);
            }
        }
        return new HashSet<>();
    }
    
    public static void initializePlayerProgress(MinecraftServer server, UUID playerId) {
        if (!playerProgress.containsKey(playerId)) {
            // Load the player's progress by passing in the server instance
            Map<String, Boolean> progress = loadPlayerProgress(server, playerId);

            if (progress.isEmpty()) {
                // Initialize progress with all false values
                progress = new HashMap<>();
                for (String item : masterChecklist) {
                    progress.put(item, false);
                }
                // Save the player's progress in the correct world folder
                savePlayerProgress(server, playerId, progress);
            }

            playerProgress.put(playerId, progress);
        }
    }
    
    static void savePlayerProgress(MinecraftServer server, UUID playerId, Map<String, Boolean> progress) {
        // Get the current world's path
        Path worldFolderPath = server.getWorldPath(LevelResource.ROOT).toAbsolutePath();
        
        // Create the path for the player's progress file inside the world folder
        Path progressFolderPath = worldFolderPath.resolve("player_progress");
        
        try {
            // Ensure the folder exists
            Files.createDirectories(progressFolderPath); // This may throw IOException
            
            // Now save the player's progress in the corresponding file
            Path playerProgressPath = progressFolderPath.resolve(playerId.toString() + ".json");

            // Serialize `progress` to a JSON file named after the player UUID
            Files.write(playerProgressPath, new Gson().toJson(progress).getBytes());
            LOGGER.info("Player progress saved to " + playerProgressPath);
        } catch (IOException e) {
            LOGGER.error("Failed to create directories or save progress for player " + playerId, e);
        }
    }
    
    private static Map<String, Boolean> loadPlayerProgress(MinecraftServer server, UUID playerId) {
        // Get the current world's path
        Path worldFolderPath = server.getWorldPath(LevelResource.ROOT).toAbsolutePath();
        
        // Create the path for the player's progress file inside the world folder
        Path progressFolderPath = worldFolderPath.resolve("player_progress");
        
        // Construct the path for the specific player's progress file
        Path playerProgressPath = progressFolderPath.resolve(playerId.toString() + ".json");
        
        // Check if the file exists
        if (Files.exists(playerProgressPath)) {
            try {
                // Read the file content into a string
                String json = new String(Files.readAllBytes(playerProgressPath));
                
                // Deserialize the JSON into a Map<String, Boolean> and return it
                return new Gson().fromJson(json, new TypeToken<Map<String, Boolean>>() {}.getType());
            } catch (IOException e) {
                LOGGER.error("Failed to load progress for player " + playerId, e);
            }
        }
        
        // If the file doesn't exist or if reading fails, return an empty progress map
        return new HashMap<>();
    }

    
    private static void initializeMasterChecklist(MinecraftServer server) {
        // Try to load the master checklist from file first
        Set<String> loadedChecklist = loadMasterChecklist(server);

        // If loading the checklist fails (i.e., the file doesn't exist or is invalid), generate a new one
        if (loadedChecklist.isEmpty()) {
            LOGGER.info("Master checklist not found, generating a new checklist...");
            generateMasterChecklist(server);
            saveMasterChecklist(server); // Save the newly generated checklist
        } else {
            masterChecklist.addAll(loadedChecklist); // If the checklist is found, use the loaded checklist
            LOGGER.info("Loaded master checklist from file.");
        }

        // Log the final checklist for reference
        LOGGER.info("Master checklist: " + masterChecklist);
    }

    private static void generateMasterChecklist(MinecraftServer server) {
    	//define how many items are in the scavenger hunt's checklist
    	int checklistSize = 10;
    	
        //Get a list of all recipes in the server across all mods including vanilla
    	Set<String> recipeOutputs = generateRecipeList(server); // List to store recipe outputs

    	// Convert recipeOutputs to a list, shuffle it, and add correct number of items to the master checklist for the scavenger hunt
    	List<String> recipeList = new ArrayList<>(recipeOutputs);
    	Collections.shuffle(recipeList);
    	masterChecklist.addAll(recipeList.subList(0, Math.min(checklistSize, recipeList.size())));

        
        LOGGER.info("$$$" + masterChecklist.toString());
    }
    
    private static Set<String> generateRecipeList(MinecraftServer server) {
        RecipeManager recipeManager = server.getRecipeManager(); // Get recipe manager
        Set<String> recipeOutputs = new HashSet<>(); // Initialize the set to store recipe outputs

        // Collect all recipe outputs
        recipeManager.getRecipes().forEach(entry -> {
            String result = entry.getResultItem().getDisplayName().getString(); // Get the recipe from the map entry
            if (result != null) {
                recipeOutputs.add(result); // Add result item display name
            }
        });

        return recipeOutputs;
    }

}
