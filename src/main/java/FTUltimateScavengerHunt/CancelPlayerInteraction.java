package FTUltimateScavengerHunt;



import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent.XpChange;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ftultimatescavengerhunt", bus = Mod.EventBusSubscriber.Bus.FORGE)


public class CancelPlayerInteraction {
	
    // Enforce spawn protection
    private static Boolean  isWithinSpawnProtectionRadius(BlockPos pos) {
        BlockPos spawnPos = FTUltimateScavengerHunt.defaultSpawnPosition;

        // Convert both positions to Vec3 and calculate distance squared
        Vec3 playerVec = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        Vec3 spawnVec = new Vec3(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        double distanceSquared = playerVec.distanceToSqr(spawnVec);

		return distanceSquared <= FTUltimateScavengerHunt.spawnProtectionRadius * FTUltimateScavengerHunt.spawnProtectionRadius;
    }
    
    // Prevent block breaking before the hunt starts
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!FTUltimateScavengerHunt.isHuntStarted) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot break blocks until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        } else {
        	if(isWithinSpawnProtectionRadius(event.getPos())) event.setCanceled(true);
        }
    }
    
    
    
    // Prevent block placement before the hunt starts
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
    	
        if (!FTUltimateScavengerHunt.isHuntStarted) {
            cancelBlockPlacement(event);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.sendMessage(
                    new TextComponent("You cannot place blocks until the scavenger hunt is started with /starthunt."),
                    player.getUUID()
                );
            }
        } else {
        	if (isWithinSpawnProtectionRadius(event.getPos())) {
        		cancelBlockPlacement(event);
        	}
        }
    }

    private static void cancelBlockPlacement(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.getLevel();
            BlockPos pos = event.getPos();

            // Remove the placed block from the world immediately
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            delayReturnBlock(event, player);
        }
    }
    
    private static void delayReturnBlock(BlockEvent.EntityPlaceEvent event, ServerPlayer player) {    
        // Get the block type the player attempted to place
        BlockState placedBlockState = event.getPlacedBlock();
        Block placedBlock = placedBlockState.getBlock();
        ItemStack blockItem = new ItemStack(placedBlock.asItem());
        
        // Create a ScheduledExecutorService to handle the task
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = () -> {
        	int currentStackSize = getItemCount(player, blockItem);  	
        	if (currentStackSize < 1) {
	            player.getInventory().add(blockItem);
            	delayReturnBlock(event, player);
        	}
        };
        scheduler.schedule(task, 500, TimeUnit.MILLISECONDS);  
    }

    private static int getItemCount(ServerPlayer player, ItemStack itemStack) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stackInSlot = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(itemStack, stackInSlot)) {
                count += stackInSlot.getCount();
            }
        }
        return count;
    }











    

    // Prevent item pickups before the scavenger hunt starts
    @SubscribeEvent
    public static void onItemPickup(ItemPickupEvent event) {
        if (!FTUltimateScavengerHunt.isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot pick up items until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    // Prevent XP gain before the scavenger hunt starts
    @SubscribeEvent
    public static void onXPChange(XpChange event) {
        if (!FTUltimateScavengerHunt.isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getEntity().sendMessage(new TextComponent("You cannot gain XP until the scavenger hunt is started with /starthunt."), event.getEntity().getUUID());
        }
    }

    // Prevent player damage before the scavenger hunt starts
    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent event) {
        if (!FTUltimateScavengerHunt.isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getEntity().sendMessage(new TextComponent("You cannot take damage until the scavenger hunt is started with /starthunt."), event.getEntity().getUUID());
        }
    }

    /*
    // Prevent player interactions with the world before the scavenger hunt starts
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (!FTUltimateScavengerHunt.isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot interact with the world until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }
    */

    // Prevent player attacks on entities before the scavenger hunt starts
    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (!FTUltimateScavengerHunt.isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot attack entities until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }
    
    // Prevent item dropping before the scavenger hunt starts
    @SubscribeEvent
    public static void onItemDrop(ItemTossEvent event) {
        if (!FTUltimateScavengerHunt.isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot drop items until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    // Prevent item usage before the scavenger hunt starts
    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (!FTUltimateScavengerHunt.isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot use items until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    // Prevent entity interactions before the scavenger hunt starts
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!FTUltimateScavengerHunt.isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot interact with entities until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    // Prevent crafting before the scavenger hunt starts
    @SubscribeEvent
    public static void onItemCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!FTUltimateScavengerHunt.isHuntStarted && event.isCancelable()) {
            event.setCanceled(true);
            event.getPlayer().sendMessage(new TextComponent("You cannot craft items until the scavenger hunt is started with /starthunt."), event.getPlayer().getUUID());
        }
    }

    
    // Disable the passage of time until the hunt has started
    private static long frozenTime = -1;  // Variable to store the frozen time
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        // Check if the world is a ServerLevel and if the hunt hasn't started
        if (!FTUltimateScavengerHunt.isHuntStarted && event.world instanceof ServerLevel) {
            ServerLevel serverWorld = (ServerLevel) event.world;

            // If the time hasn't been frozen yet, freeze it at the current day time
            if (frozenTime == -1) {
                frozenTime = serverWorld.getDayTime();  // Capture the current time when the hunt starts
            }

            // Set the world time to the frozen time to keep it from advancing
            serverWorld.setDayTime(frozenTime);
        }
    }

}
