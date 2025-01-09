package FTUltimateScavengerHunt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
        	event.getPlayer().sendMessage(new TextComponent("You cannot break blocks within the spawn protection area."), event.getPlayer().getUUID());
        }
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

    
    // Prevent player interactions with the world before the scavenger hunt starts
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        //Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemStack(); // Get the item in the player's hand

        if (!itemInHand.isEmpty() && itemInHand.getItem() instanceof BlockItem) { // Check if the item is a block
        	
            BlockPos clickedPos = event.getPos(); // The block position the player clicked on
            Direction clickedFace = event.getFace(); // The face of the block that was clicked
            
            // Calculate the target block position where the block would be placed
            BlockPos targetPos = clickedPos.relative(clickedFace);

            if (isWithinSpawnProtectionRadius(targetPos)) {
            	if(event.isCancelable())event.setCanceled(true);
            	event.getPlayer().sendMessage(new TextComponent("You cannot place blocks within the spawn protection area"), event.getPlayer().getUUID());
            }
        }
    }

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
