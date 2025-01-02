package FTUltimateScavengerHunt;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import org.slf4j.Logger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.common.Mod;

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
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("starthunt")
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

                    // Initialize progress for all logged-in players
                    for (ServerPlayer loggedInPlayer : context.getSource().getServer().getPlayerList().getPlayers()) {
                        FTUltimateScavengerHunt.initializePlayerProgress(context.getSource().getServer(), loggedInPlayer.getUUID());
                    }

                    context.getSource().sendSuccess(new TextComponent("Master checklist initialized and hunt started!"), false);
                }

                return Command.SINGLE_SUCCESS;
            });

        // Register the command with the server's dispatcher
        server.getCommands().getDispatcher().register(command);
    }
}

