package FTUltimateScavengerHunt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class CustomPacket {
    private final CompoundTag data;

    public CustomPacket(CompoundTag data) {
        this.data = data;
    }

    // Encode the packet data to the network buffer
    public static void encode(CustomPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    // Decode the packet data from the network buffer
    public static CustomPacket decode(FriendlyByteBuf buffer) {
        return new CustomPacket(buffer.readNbt());
    }

    // Getter for the data
    public CompoundTag getData() {
        return data;
    }
}
