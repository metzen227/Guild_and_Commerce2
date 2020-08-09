package dicemc.gnc.common;

import java.util.function.Supplier;

import dicemc.gnc.account.AccountUtils;
import dicemc.gnc.land.network.PacketOpenGui_Land;
import dicemc.gnc.setup.Networking;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class PacketGuiRequest {

	private final int gui;
	
	public PacketGuiRequest(PacketBuffer buf) {gui = buf.readInt();}
	
	public PacketGuiRequest(int guiType) {this.gui = guiType;}
	
	public void toBytes(PacketBuffer buf) {buf.writeInt(gui);}
 	
	public boolean handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			switch(gui) {
			case 0: {
				double balP = AccountUtils.getBalance(ctx.get().getSender().getUniqueID());
				Networking.sendToClient(new PacketOpenGui_Land(balP), ctx.get().getSender()); 
				break;
			}
			case 1: {break;}
			case 2: {break;}
			default:
			}
			
		});
		return true;
	}
}