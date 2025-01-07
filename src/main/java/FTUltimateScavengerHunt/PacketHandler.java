package FTUltimateScavengerHunt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PacketHandler {
    // The packet handler now accepts both the packet and the context supplier
    public static void onPacketReceived(CustomPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        System.out.println("--- PACKET RECEIVED");

        // Get the context for the network event (e.g., for networking synchronization)
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Process the received packet here
            CompoundTag data = packet.getData();
            System.out.println("Received custom packet with data: " + data);

            // Handle isHuntStarted status
            if (data.contains("isHuntStarted")) {
                FTUltimateScavengerHunt.isHuntStarted = data.getBoolean("isHuntStarted");
            }

            // Handle masterPlayerProgress
            if (data.contains("masterPlayerProgress", Tag.TAG_COMPOUND)) {
                CompoundTag progressData = data.getCompound("masterPlayerProgress");
                ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> decodedProgress = new ConcurrentHashMap<>();

                for (String playerName : progressData.getAllKeys()) {
                    CompoundTag playerProgressTag = progressData.getCompound(playerName);
                    ConcurrentHashMap<String, Boolean> playerProgress = new ConcurrentHashMap<>();

                    for (String taskName : playerProgressTag.getAllKeys()) {
                        playerProgress.put(taskName, playerProgressTag.getBoolean(taskName));
                    }

                    decodedProgress.put(playerName, playerProgress);
                }

                // Update the PlayerProgress.masterPlayerProgress
                PlayerProgressManager.masterPlayerProgress = decodedProgress;
                System.out.println("Updated masterPlayerProgress: " + PlayerProgressManager.masterPlayerProgress);
            }
        });

        // Mark the event as handled
        context.setPacketHandled(true);
    }
}
