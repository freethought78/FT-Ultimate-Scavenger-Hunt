package FTUltimateScavengerHunt;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.Level;
import java.util.List;

@Mod.EventBusSubscriber(modid = FTUltimateScavengerHunt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("*$*$ onServerStarting, from within ModCommands");
        // Ensure this runs only on the server side (for both single-player and dedicated servers)
        if (event.getServer().isDedicatedServer() || event.getServer().isSingleplayer()) {
            // Register the command only for server-side execution
            registerCommands(event.getServer());
        }
    }

    public static void registerCommands(MinecraftServer server) {
        // Create the /starthunt command
        LiteralArgumentBuilder<CommandSourceStack> startHuntCommand = Commands.literal("starthunt")
            .executes(context -> {
                // Retrieve the player from the context
                ServerPlayer player = (ServerPlayer) context.getSource().getEntity();

                // Check permissions and state of the hunt
                if (!player.hasPermissions(1) && !player.getServer().isSingleplayer()) {
                    context.getSource().sendSuccess(new TextComponent("You must be an operator to start the hunt."), false);
                    return Command.SINGLE_SUCCESS;
                }

                if (FTUltimateScavengerHunt.huntWinner != null) {
                    context.getSource().sendSuccess(new TextComponent("The hunt has ended! Winner: " + FTUltimateScavengerHunt.huntWinner), false);
                    return Command.SINGLE_SUCCESS;
                }

                if (FTUltimateScavengerHunt.isHuntStarted) {
                    context.getSource().sendSuccess(new TextComponent("The hunt is already in progress!"), false);
                } else {
                    // Start the hunt
                    FTUltimateScavengerHunt.initializeMasterChecklist(context.getSource().getServer());
                    FTUltimateScavengerHunt.isHuntStarted = true;
                    
                    FTUltimateScavengerHunt.setWorldBorder(server.overworld(), FTUltimateScavengerHunt.EXPANDED_BORDER_SIZE);
                    context.getSource().sendSuccess(new TextComponent("Master checklist initialized and hunt started!"), false);
                }

                return Command.SINGLE_SUCCESS;
            });

        // Create the /respawn command (available only when the hunt is not started)
        LiteralArgumentBuilder<CommandSourceStack> respawnCommand = Commands.literal("respawn")
            .executes(context -> {
                ServerPlayer player = (ServerPlayer) context.getSource().getEntity();

                // Check if the hunt is not started
                if (FTUltimateScavengerHunt.isHuntStarted) {
                    context.getSource().sendSuccess(new TextComponent("You cannot respawn during the scavenger hunt."), false);
                    return Command.SINGLE_SUCCESS;
                }

                // Respawn the player at the server's default spawn point (Overworld spawn)
                player.teleportTo(player.getServer().getLevel(Level.OVERWORLD).getSharedSpawnPos().getX(), 
                                  player.getServer().getLevel(Level.OVERWORLD).getSharedSpawnPos().getY(),
                                  player.getServer().getLevel(Level.OVERWORLD).getSharedSpawnPos().getZ());

                context.getSource().sendSuccess(new TextComponent("You have been respawned at the default spawn point."), false);

                return Command.SINGLE_SUCCESS;
            });

     // Create the /leaderboard command
        LiteralArgumentBuilder<CommandSourceStack> leaderboardCommand = Commands.literal("leaderboard")
            .executes(context -> {
                ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
                List<LeaderboardManager.LeaderboardEntry> leaderboard = LeaderboardManager.getLeaderboard();

                if (leaderboard.isEmpty()) {
                    context.getSource().sendSuccess(new TextComponent("The leaderboard is currently empty."), false);
                    return Command.SINGLE_SUCCESS;
                }

                // Build the leaderboard display message
                StringBuilder leaderboardMessage = new StringBuilder("Leaderboard:\n");
                int rank = 1;
                for (LeaderboardManager.LeaderboardEntry entry : leaderboard) {
                    // Use the correct field name from LeaderboardEntry (e.g., playerName)
                    leaderboardMessage.append(String.format("%d. %s - %d completions\n", rank++, entry.playerName, entry.completionCount));
                }

                // Send the leaderboard message to the player
                player.sendMessage(new TextComponent(leaderboardMessage.toString()), player.getUUID());

                return Command.SINGLE_SUCCESS;
            });


        // Create the /isitemcomplete command
        LiteralArgumentBuilder<CommandSourceStack> isItemCompleteCommand = Commands.literal("isitemcomplete")
            .then(Commands.argument("itemName", StringArgumentType.word())
                .executes(context -> {
                    ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
                    String itemName = StringArgumentType.getString(context, "itemName");

                    // Check if the item is in the checklist and if it's completed
                    boolean isComplete = PlayerProgressManager.isItemComplete(player.getName().getString(), itemName);

                    if (isComplete) {
                        context.getSource().sendSuccess(new TextComponent("You have completed the item: " + itemName), false);
                    } else {
                        context.getSource().sendSuccess(new TextComponent("You have not completed the item: " + itemName), false);
                    }

                    return Command.SINGLE_SUCCESS;
                })
            );

        // Register the commands with the server's dispatcher
        server.getCommands().getDispatcher().register(startHuntCommand);
        server.getCommands().getDispatcher().register(respawnCommand);
        server.getCommands().getDispatcher().register(leaderboardCommand);  // Register the leaderboard command
        server.getCommands().getDispatcher().register(isItemCompleteCommand);  // Register the new isitemcomplete command
    }
}
