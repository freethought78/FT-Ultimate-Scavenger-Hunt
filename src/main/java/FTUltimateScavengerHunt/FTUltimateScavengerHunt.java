package FTUltimateScavengerHunt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

@Mod("ftultimatescavengerhunt")
@Mod.EventBusSubscriber(modid = "ftultimatescavengerhunt", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FTUltimateScavengerHunt {

    public static final String MODID = "ftultimatescavengerhunt";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    // Block Registration
    public static final RegistryObject<Block> FT_QUEST_HUB_BLOCK = BLOCKS.register("ft_quest_hub", 
            () -> new FTQuestHubBlock());

    // BlockItem Registration
    public static final RegistryObject<BlockItem> FT_QUEST_HUB_BLOCK_ITEM = ITEMS.register("ft_quest_hub",
            () -> new BlockItem(FT_QUEST_HUB_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

    
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // Master checklist for the current world
    public static Set<String> masterChecklist = new HashSet<>();
    
    // List of recipe outputs across all mods
    static List<String> recipeList;
    

    public static boolean isHuntStarted = false;
    public static String huntWinner = null;  // Changed from UUID to String player name

    // Set the initial world border size
    private static final int INITIAL_BORDER_SIZE = 128;
    public static final int EXPANDED_BORDER_SIZE = 1000000; // World border when the hunt starts
    
    public FTUltimateScavengerHunt() {
        LOGGER.info("FTUltimateScavengerHunt mod is initializing...");
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(TaskScheduler.class);

        // Register blocks and items
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
    
    // Event listener to set world border when the server starts
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
    	MinecraftServer server = event.getServer();
        Set<String> recipeOutputs = generateRecipeList(event.getServer()); // List to store recipe outputs

        recipeList = new ArrayList<>(recipeOutputs);
    	
        ServerLevel world = event.getServer().getLevel(ServerLevel.OVERWORLD);

        // Initially, set a small world border when the hunt hasn't started
        setWorldBorder(world, INITIAL_BORDER_SIZE);
        
        //When server starts, load player progress, load master checklist, and update the leaderboard
        masterChecklist = loadMasterChecklist(server);
        PlayerProgressManager.loadAllPlayerProgress(server);
        LeaderboardManager.updateLeaderboard(server);
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

        isHuntStarted = Files.exists(checklistPath);

        if (isHuntStarted) {
            setWorldBorder(world, EXPANDED_BORDER_SIZE);
        } else {
            deleteNonPlayerEntities(world);
        }

        // Load player progress
    }
    
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Clear data structures to avoid stale state
        isHuntStarted = false;
        huntWinner = null;
        masterChecklist.clear();
        recipeList.clear();
        
        PlayerProgressManager.cleanUpForShutDown(event.getServer());
        LeaderboardManager.cleanUpForShutDown();
    }

    public static void deleteNonPlayerEntities(ServerLevel world) {
        // Iterate through all entities in the world and remove non-player entities
        Iterable<net.minecraft.world.entity.Entity> entities = world.getAllEntities();
        
        entities.forEach(entity -> {
            // If the entity is not a player, remove it
            if (entity != null && !(entity instanceof Player)) {
                entity.discard(); // Remove non-player entities
            }
        });
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (isHuntStarted) return;
        
        // Prevent non-player entities from joining the world
        if (!(event.getEntity() instanceof Player)) {
            event.setCanceled(true); // Cancel the entity from joining
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    	if (isHuntStarted) {
            // Initialize player progress if the hunt is started and not yet ended
            PlayerProgressManager.initializePlayerProgress(event.getPlayer().getName().getString(), event.getPlayer().getServer());
        }
    }
    
 // Method to mark a player as the winner and end the hunt
    public static void endHunt(MinecraftServer server, String winnerName) {
        if (huntWinner == null) {
            huntWinner = winnerName;

            // Save the winner's name to a file
            saveWinnerToFile(server, winnerName);

            // Create the message
            TextComponent message = new TextComponent("The scavenger hunt has ended! Congratulations to " + winnerName + ", you are the winner!");

            // Broadcast the message to all players in the default chat type
            server.getPlayerList().broadcastMessage(message, ChatType.SYSTEM, null);

            // Trigger fireworks for all players
            launchFireworksForPlayers(server);
        }
    }

    // Method to launch fireworks around every player
    private static void launchFireworksForPlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Start a fireworks show around the player
            startFireworksShow(player);
        }
    }

    // Method to start a fireworks show around a single player
    private static void startFireworksShow(ServerPlayer player) {
        ServerLevel level = player.getLevel();
        Random random = new Random();

        // Schedule fireworks for 10 seconds
        int durationInTicks = 200; // 10 seconds (20 ticks per second)
        int interval = 10; // Launch every 10 ticks

        for (int tick = 0; tick < durationInTicks; tick += interval) {
            final int delay = tick;  // Capture the correct delay for each scheduled task
            TaskScheduler.scheduleTask(delay, () -> {
                // Spawn fireworks around the player
                for (int dx = -5; dx <= 5; dx += 2) {
                    for (int dz = -5; dz <= 5; dz += 2) {  // Fix loop condition
                        double offsetX = dx + random.nextDouble() - 0.5;
                        double offsetZ = dz + random.nextDouble() - 0.5;

                        FireworkRocketEntity firework = new FireworkRocketEntity(
                            level,
                            player.getX() + offsetX,
                            player.getY() + 1 + random.nextDouble(), // Slight random height variation
                            player.getZ() + offsetZ,
                            createRandomFireworkItem(random)
                        );

                        level.addFreshEntity(firework);
                    }
                }
            });
        }
    }



    // Method to create a firework item with random properties
    private static ItemStack createRandomFireworkItem(Random random) {
        ItemStack fireworkItem = new ItemStack(Items.FIREWORK_ROCKET);

        CompoundTag fireworkTag = new CompoundTag();
        fireworkTag.putInt("Flight", random.nextInt(3) + 1); // Random flight duration (1 to 3)

        // Create explosion data
        ListTag explosions = new ListTag();
        int explosionCount = random.nextInt(3) + 1; // 1 to 3 explosions per firework

        for (int i = 0; i < explosionCount; i++) {
            CompoundTag explosion = new CompoundTag();
            explosion.putIntArray("Colors", getRandomColors(random, random.nextInt(3) + 1)); // 1 to 3 random colors
            explosion.putIntArray("FadeColors", getRandomColors(random, random.nextInt(2) + 1)); // 1 to 2 fade colors
            explosion.putByte("Type", (byte) random.nextInt(5)); // Random shape (0 to 4)
            explosion.putBoolean("Flicker", random.nextBoolean()); // Random flicker
            explosion.putBoolean("Trail", random.nextBoolean()); // Random trail

            explosions.add(explosion);
        }

        fireworkTag.put("Explosions", explosions);
        fireworkItem.getOrCreateTag().put("Fireworks", fireworkTag);

        return fireworkItem;
    }

    // Helper method to generate random colors
    private static int[] getRandomColors(Random random, int count) {
        int[] colors = new int[count];
        for (int i = 0; i < count; i++) {
            colors[i] = random.nextInt(0xFFFFFF); // Random RGB color
        }
        return colors;
    }




    private static void saveWinnerToFile(MinecraftServer server, String winnerName) {
        Path worldFolderPath = server.getWorldPath(LevelResource.ROOT).toAbsolutePath();
        Path winnerFilePath = worldFolderPath.resolve("hunt_winner.json");

        try {
            // Write the winner's name to a JSON file
            Files.write(winnerFilePath, new Gson().toJson(winnerName).getBytes());
            LOGGER.info("Hunt winner name saved to " + winnerFilePath);
        } catch (IOException e) {
            LOGGER.error("Failed to save hunt winner name", e);
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

    static Set<String> loadMasterChecklist(MinecraftServer server) {
        Path worldFolderPath = server.getWorldPath(LevelResource.ROOT).toAbsolutePath();
        Path checklistPath = worldFolderPath.resolve("master_checklist.json");
        Path winnerFilePath = worldFolderPath.resolve("hunt_winner.json");

        // Load the winner name if the file exists
        if (Files.exists(winnerFilePath)) {
            try {
                String winnerJson = new String(Files.readAllBytes(winnerFilePath));
                huntWinner = new Gson().fromJson(winnerJson, String.class);
                LOGGER.info("Hunt winner name loaded: " + huntWinner);
            } catch (IOException e) {
                LOGGER.error("Failed to load hunt winner name", e);
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

    public static void initializeMasterChecklist(MinecraftServer server, int checklistSize) {
        // Try to load the master checklist from file first
        Set<String> loadedChecklist = loadMasterChecklist(server);

        // If loading the checklist fails, generate a new one
        if (loadedChecklist.isEmpty()) {
            LOGGER.info("Master checklist not found, generating a new checklist...");
            generateMasterChecklist(server, checklistSize);
            saveMasterChecklist(server); // Save the newly generated checklist

            // Initialize progress for all logged-in players and give them the FT Quest Hub Block
            for (ServerPlayer loggedInPlayer : server.getPlayerList().getPlayers()) {
                PlayerProgressManager.initializePlayerProgress(loggedInPlayer.getName().getString(), server);
            }

        } else {
            masterChecklist.addAll(loadedChecklist); // If the checklist is found, use the loaded checklist
            LOGGER.info("Loaded master checklist from file.");
        }

        // Log the final checklist for reference
        LOGGER.info("Master checklist: " + masterChecklist);
    }

    private static void generateMasterChecklist(MinecraftServer server, int checklistSize) {
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
