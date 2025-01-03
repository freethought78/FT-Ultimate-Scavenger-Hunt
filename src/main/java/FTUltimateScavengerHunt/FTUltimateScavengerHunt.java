package FTUltimateScavengerHunt;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent.XpChange;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
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
    public static Set<String> masterChecklist = new HashSet<>();

    // Player progress (UUID -> checklist progress)
    public static Map<UUID, Map<String, Boolean>> playerProgress = new HashMap<>();
    
    public static boolean isHuntStarted = false;
    public static UUID huntWinner = null; 
    
    // Set the initial world border size (in blocks, adjust as needed)
    private static final int INITIAL_BORDER_SIZE = 128; // 128 blocks (8x8 chunks)
    public static final int EXPANDED_BORDER_SIZE = 1000000; // World border when the hunt starts
    
    public FTUltimateScavengerHunt() {
    	LOGGER.info("FTUltimateScavengerHunt mod is initializing...");
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.register(this);

        // Register blocks and items
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
    
    // Event listener to set world border when the server starts
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
    	ServerLevel world = event.getServer().getLevel(ServerLevel.OVERWORLD);

        // Initially, set a small world border when the hunt hasn't started
        setWorldBorder(world, INITIAL_BORDER_SIZE);
    }
    
    // Method to set the world border size
    public static void setWorldBorder(ServerLevel world, int size) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(world.getSharedSpawnPos().getX(), world.getSharedSpawnPos().getZ());
        border.setSize(size);
    }
    
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
    	ServerLevel world = event.getServer().getLevel(ServerLevel.OVERWORLD);
    	
        LOGGER.info("SERVER STARTED ***");
        
        
        MinecraftServer server = event.getServer();
        Path worldFolderPath = server.getWorldPath(LevelResource.ROOT).toAbsolutePath();
        Path checklistPath = worldFolderPath.resolve("master_checklist.json");

        if (!Files.exists(checklistPath)) {
        	isHuntStarted = false;
        }        	
        huntWinner = null;
        masterChecklist = new HashSet<>();
        playerProgress = new HashMap<>();
        
        if (isHuntStarted) setWorldBorder(world, EXPANDED_BORDER_SIZE);
    }

    
    // Prevent block breaking before the scavenger hunt starts
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot break blocks until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    // Prevent item pickups before the scavenger hunt starts
    @SubscribeEvent
    public static void onItemPickup(ItemPickupEvent event) {
        if (!isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot pick up items until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    // Prevent XP gain before the scavenger hunt starts
    @SubscribeEvent
    public static void onXPChange(XpChange event) {
        if (!isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getEntity().sendMessage(new TextComponent("You cannot gain XP until the scavenger hunt is started with /starthunt."), event.getEntity().getUUID());
        }
    }

    // Prevent player damage before the scavenger hunt starts
    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent event) {
        if (!isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getEntity().sendMessage(new TextComponent("You cannot take damage until the scavenger hunt is started with /starthunt."), event.getEntity().getUUID());
        }
    }

    // Prevent player interactions with the world before the scavenger hunt starts
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (!isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot interact with the world until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    // Prevent player attacks on entities before the scavenger hunt starts
    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (!isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot attack entities until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    // Prevent block placement before the scavenger hunt starts
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getEntity().sendMessage(new TextComponent("You cannot place blocks until the scavenger hunt is started with /starthunt."), event.getEntity().getUUID());
        }
    }

    // Prevent item dropping before the scavenger hunt starts
    @SubscribeEvent
    public static void onItemDrop(ItemTossEvent event) {
        if (!isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot drop items until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    // Prevent item usage before the scavenger hunt starts
    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (!isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot use items until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    // Prevent entity interactions before the scavenger hunt starts
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot interact with entities until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    // Prevent crafting before the scavenger hunt starts
    @SubscribeEvent
    public static void onItemCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot craft items until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    
    // Disable the passage of time until the hunt has started
    private static long frozenTime = -1;  // Variable to store the frozen time
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        // Check if the world is a ServerLevel and if the hunt hasn't started
        if (!isHuntStarted && event.world instanceof ServerLevel) {
            ServerLevel serverWorld = (ServerLevel) event.world;

            // If the time hasn't been frozen yet, freeze it at the current day time
            if (frozenTime == -1) {
                frozenTime = serverWorld.getDayTime();  // Capture the current time when the hunt starts
            }

            // Set the world time to the frozen time to keep it from advancing
            serverWorld.setDayTime(frozenTime);
        }
    }

    
    // Inside onPlayerLoggedIn method (modified)
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (huntWinner != null) {
            // If the hunt has ended, inform the player they cannot interact with the hunt
            event.getPlayer().sendMessage(new TextComponent("The scavenger hunt has ended. You cannot continue the hunt."), event.getPlayer().getUUID());
            event.setCanceled(true);  // Optionally cancel the login event if you want to block further interaction
        } else if (isHuntStarted) {
            // Initialize player progress if the hunt is started and not yet ended
            initializePlayerProgress(event.getPlayer().getServer(), event.getPlayer().getUUID());
        }
    }
    
 // Method to mark a player as the winner and end the hunt
    public static void endHunt(MinecraftServer server, UUID winnerUUID) {
        if (huntWinner == null) {
            huntWinner = winnerUUID;

            // Save the winner's UUID to a file
            saveWinnerToFile(server, winnerUUID);

            // Resolve the player's username from their UUID
            String winnerName = server.getPlayerList().getPlayer(winnerUUID).getName().getString();

            // Create the message
            TextComponent message = new TextComponent("The scavenger hunt has ended! Congatulations to " + winnerName + ", you are the winner!");

            // Broadcast the message to all players in the default chat type
            server.getPlayerList().broadcastMessage(message, ChatType.SYSTEM, null);
        }
    }

    
    private static void saveWinnerToFile(MinecraftServer server, UUID winnerUUID) {
        Path worldFolderPath = server.getWorldPath(LevelResource.ROOT).toAbsolutePath();
        Path winnerFilePath = worldFolderPath.resolve("hunt_winner.json");

        try {
            // Write the UUID to a JSON file
            Files.write(winnerFilePath, new Gson().toJson(winnerUUID.toString()).getBytes());
            LOGGER.info("Hunt winner UUID saved to " + winnerFilePath);
        } catch (IOException e) {
            LOGGER.error("Failed to save hunt winner UUID", e);
        }
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
        Path worldFolderPath = server.getWorldPath(LevelResource.ROOT).toAbsolutePath();
        Path checklistPath = worldFolderPath.resolve("master_checklist.json");
        Path winnerFilePath = worldFolderPath.resolve("hunt_winner.json");

        // Load the winner UUID if the file exists
        if (Files.exists(winnerFilePath)) {
            try {
                String winnerJson = new String(Files.readAllBytes(winnerFilePath));
                huntWinner = UUID.fromString(new Gson().fromJson(winnerJson, String.class));
                LOGGER.info("Hunt winner UUID loaded: " + huntWinner);
            } catch (IOException e) {
                LOGGER.error("Failed to load hunt winner UUID", e);
            }
        }

        // Load the master checklist
        if (Files.exists(checklistPath)) {
            try {
                String json = new String(Files.readAllBytes(checklistPath));
                isHuntStarted = true;
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
                
                //Give the player a quest hub block
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                ItemStack questblock = new ItemStack(FTUltimateScavengerHunt.FT_QUEST_HUB_BLOCK_ITEM.get());
                
                player.getInventory().add(questblock);
                player.sendMessage(new TextComponent("You have received an FT Quest Hub Block to begin your scavenger hunt!"), playerId);
                
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

    
    public static void initializeMasterChecklist(MinecraftServer server) {
        // Try to load the master checklist from file first
        Set<String> loadedChecklist = loadMasterChecklist(server);

        // If loading the checklist fails (i.e., the file doesn't exist or is invalid), generate a new one
        if (loadedChecklist.isEmpty()) {
            LOGGER.info("Master checklist not found, generating a new checklist...");
            generateMasterChecklist(server);
            saveMasterChecklist(server); // Save the newly generated checklist
            
            // Initialize progress for all logged-in players and give them the FT Quest Hub Block
            for (ServerPlayer loggedInPlayer : server.getPlayerList().getPlayers()) {
                FTUltimateScavengerHunt.initializePlayerProgress(server, loggedInPlayer.getUUID());
            }
            
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
