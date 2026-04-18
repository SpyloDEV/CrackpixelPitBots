package crackpixel.pitbots.nms;

import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.NetworkManager;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PlayerConnection;

public class BotPlayerConnection extends PlayerConnection {

    public BotPlayerConnection(MinecraftServer minecraftServer, NetworkManager networkManager, EntityPlayer entityPlayer) {
        super(minecraftServer, networkManager, entityPlayer);
    }

    @Override
    public void sendPacket(Packet packet) {

    }

    @Override
    public void disconnect(String message) {

    }

    @Override
    public boolean isDisconnected() {
        return false;
    }
}
