package FTUltimateScavengerHunt;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;

public class FTQuestHubBlock extends Block {

    private static final Logger LOGGER = LogUtils.getLogger();

    public FTQuestHubBlock() {
        super(BlockBehaviour.Properties.of(Material.METAL).strength(3.0f, 3.0f)); // Material and strength
    }

    @Override
    public String getDescriptionId() {
        return "FT Quest Hub";
    }

    // Override getDrops to make the block drop itself when broken
    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        // Ensure the block drops itself
        return Collections.singletonList(new ItemStack(this));
    }

    // Override the 'use' method to handle right-click behavior
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Get the item the player is holding
        ItemStack heldItem = player.getItemInHand(hand);
        String itemName = heldItem.getDisplayName().getString();
        UUID playerID = player.getUUID();

        // On the client side only, when the block is right clicked with an empty hand, display the scavenger hunt's progress panel
        if (level.isClientSide()) {
            if (heldItem.isEmpty()) {
                Minecraft.getInstance().setScreen(new ScavengerHuntPanel());
                return InteractionResult.SUCCESS;
            }
        }

        // On the server side
        if (!level.isClientSide()) {
            if (heldItem.isEmpty()) {
                return InteractionResult.SUCCESS;
            }

            // Check if the item is part of the scavenger hunt checklist
            if (!isItemInChecklist(playerID, itemName)) {
                player.sendMessage(new TextComponent(itemName + " is not part of the scavenger hunt."), playerID);
                return InteractionResult.SUCCESS;
            }

            // Check if the item has already been completed
            if (!isItemComplete(playerID, itemName)) {
                markItemComplete(player.getServer(), playerID, itemName);
                heldItem.shrink(1); // Decrease the item stack count
                player.sendMessage(new TextComponent(itemName + " is now complete!"), playerID);

                // Calculate and display the player's progress
                Map<String, Boolean> progress = FTUltimateScavengerHunt.playerProgress.get(playerID);
                long itemsTurnedIn = progress.values().stream().filter(Boolean::booleanValue).count();
                long totalItems = FTUltimateScavengerHunt.masterChecklist.size();
                long itemsRemaining = totalItems - itemsTurnedIn;

                // Broadcast the item turn-in to the server
                String playerName = player.getName().getString();
                String broadcastMessage = String.format(
                    "%s has turned in %s! They have now turned in %d out of %d items and have %d more to go!",
                    playerName, itemName, itemsTurnedIn, totalItems, itemsRemaining
                );
                player.getServer().getPlayerList().broadcastMessage(new TextComponent(broadcastMessage), ChatType.SYSTEM, null);

                // Check if the player has completed the scavenger hunt and declare them as the winner
                if (isPlayerComplete(playerID)) {
                    FTUltimateScavengerHunt.endHunt(player.getServer(), playerID);
                }

                return InteractionResult.SUCCESS;
            } else {
                player.sendMessage(new TextComponent(itemName + " was already turned in."), playerID);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.SUCCESS; // Return SUCCESS to indicate that the action was handled
    }


    private void markItemComplete(MinecraftServer server, UUID playerID, String itemName) {
        if (FTUltimateScavengerHunt.playerProgress.containsKey(playerID)) {
            Map<String, Boolean> progress = FTUltimateScavengerHunt.playerProgress.get(playerID);
            if (progress.containsKey(itemName) && !progress.get(itemName)) {
                progress.put(itemName, true);
                FTUltimateScavengerHunt.savePlayerProgress(server, playerID, progress);
            }
        }
    }

    private Boolean isItemInChecklist(UUID playerID, String itemName) {
        if (FTUltimateScavengerHunt.playerProgress.containsKey(playerID)) {
            Map<String, Boolean> progress = FTUltimateScavengerHunt.playerProgress.get(playerID);
            return progress.containsKey(itemName);
        }
        LOGGER.info("Player is not registered in the hunt! - This is BAD ***");
        return false;
    }

    public static boolean isItemComplete(UUID playerId, String itemName) {
        if (FTUltimateScavengerHunt.playerProgress.containsKey(playerId)) {
            Map<String, Boolean> progress = FTUltimateScavengerHunt.playerProgress.get(playerId);
            return progress.getOrDefault(itemName, false);
        }
        return false;
    }

    private void broadcastItemTurnedIn(MinecraftServer server, String playerName, String itemName) {
        // Broadcast the item turn-in message to the entire server
        String message = playerName + " has turned in the item: " + itemName;
        server.getPlayerList().broadcastMessage(new TextComponent(message), ChatType.SYSTEM, null);
    }

    private boolean isPlayerComplete(UUID playerId) {
        if (FTUltimateScavengerHunt.playerProgress.containsKey(playerId)) {
            Map<String, Boolean> progress = FTUltimateScavengerHunt.playerProgress.get(playerId);

            // Check if all items are complete
            for (Boolean completed : progress.values()) {
                if (!completed) {
                    return false;
                }
            }

            return true; // Player has completed all items
        }

        return false; // Player progress not found
    }
}
