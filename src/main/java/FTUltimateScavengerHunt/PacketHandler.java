package FTUltimateScavengerHunt;

import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;


public class PacketHandler {
    // The packet handler now accepts both the packet and the context supplier
    public static void onPacketReceived(CustomPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
    	
    	System.out.println("--- PACKET RECEIVED");
    	
        // Get the context for the network event (e.g., for networking synchronization)
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Process the received packet here
            System.out.println("Received custom packet with data: " + packet.getData());
            
            // You can use the data, e.g., set the status of the hunt
            if (packet.getData().contains("isHuntStarted")) {
                FTUltimateScavengerHunt.isHuntStarted = packet.getData().getBoolean("isHuntStarted");
            }
        });
        
        // Mark the event as handled
        context.setPacketHandled(true);
    }
}

