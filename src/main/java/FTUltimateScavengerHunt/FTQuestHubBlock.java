package FTUltimateScavengerHunt;

import java.util.Collections;
import net.minecraftforge.api.distmarker.Dist;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
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
import net.minecraftforge.api.distmarker.OnlyIn;

public class FTQuestHubBlock extends Block {

    @SuppressWarnings("unused")
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
    
    @OnlyIn(Dist.CLIENT)
    public void openScreenOnClient(ItemStack heldItem) {
        Minecraft.getInstance().setScreen(new ScavengerHuntPanel());
    }

    // Override the 'use' method to handle right-click behavior
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Get the item the player is holding
        ItemStack heldItem = player.getItemInHand(hand);
        String itemName = heldItem.getDisplayName().getString();
        String playerName = player.getName().getString();

        // On the client side only, when the block is right-clicked with an empty hand, display the scavenger hunt's progress panel
        if (level.isClientSide()) {
            if (heldItem.isEmpty()) {
                openScreenOnClient(heldItem);
                return InteractionResult.SUCCESS;
            }
        }

        // On the server side
        if (!level.isClientSide()) {
            if (heldItem.isEmpty()) {
                return InteractionResult.SUCCESS;
            }

            // Check if the item is part of the scavenger hunt checklist
            if (!PlayerProgressManager.isItemInChecklist(playerName, itemName)) {
                player.sendMessage(new TextComponent(itemName + " is not part of the scavenger hunt."), player.getUUID());
                return InteractionResult.SUCCESS;
            }

            // Check if the item has already been completed
            if (!PlayerProgressManager.isItemComplete(playerName, itemName)) {
                PlayerProgressManager.markItemComplete(player.getServer(), playerName, itemName);
                heldItem.shrink(1); // Decrease the item stack count
                player.sendMessage(new TextComponent(itemName + " is now complete!"), player.getUUID());

                // Calculate and display the player's progress
                Map<String, Boolean> progress = PlayerProgressManager.masterPlayerProgress.get(playerName);
                long itemsTurnedIn = progress.values().stream().filter(Boolean::booleanValue).count();
                long totalItems = FTUltimateScavengerHunt.masterChecklist.size();
                long itemsRemaining = totalItems - itemsTurnedIn;

                // Broadcast the item turn-in to the server
                String broadcastMessage = String.format(
                    "%s has turned in %s! They have now turned in %d out of %d items and have %d more to go!",
                    playerName, itemName, itemsTurnedIn, totalItems, itemsRemaining
                );
                player.getServer().getPlayerList().broadcastMessage(new TextComponent(broadcastMessage), ChatType.SYSTEM, player.getUUID());

                // Check if the player has completed the scavenger hunt and declare them as the winner
                if (PlayerProgressManager.isPlayerComplete(playerName)) {
                    FTUltimateScavengerHunt.endHunt(player.getServer(), playerName);
                }

                return InteractionResult.SUCCESS;
            } else {
                player.sendMessage(new TextComponent(itemName + " was already turned in."), player.getUUID());
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.SUCCESS; // Return SUCCESS to indicate that the action was handled
    }


    // Utility methods using PlayerProgressManager

    public static boolean isItemComplete(String playerName, String itemName) {
        // This method now uses PlayerProgressManager's isItemComplete method with player name
        return PlayerProgressManager.isItemComplete(playerName, itemName);
    }
}
