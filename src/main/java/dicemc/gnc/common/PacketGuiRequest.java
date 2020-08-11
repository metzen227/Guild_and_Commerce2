package dicemc.gnc.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import dicemc.gnc.GnC;
import dicemc.gnc.account.AccountUtils;
import dicemc.gnc.guild.Guild;
import dicemc.gnc.land.network.PacketOpenGui_Land;
import dicemc.gnc.land.client.GuiLandManager.ChunkSummary;
import dicemc.gnc.setup.Networking;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
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
				UUID gid = GnC.NIL;
				Guild guild = new Guild("N/A", GnC.NIL);
				if (GnC.gMgr.getGuildByMember(ctx.get().getSender().getUniqueID()) != null) {
					guild = GnC.gMgr.getGuildByMember(ctx.get().getSender().getUniqueID());
				}
				ServerWorld server = ctx.get().getSender().getServerWorld();
				ChunkPos center = new ChunkPos(ctx.get().getSender().chunkCoordX, ctx.get().getSender().chunkCoordZ);
				//collect center and adjacent chunk summaries
				List<ChunkSummary> chunkData = new ArrayList<ChunkSummary>();
				for (int x = center.x-6; x <= center.x+6; x++) {
					for (int z = center.z-6; z <= center.z+6; z++) {
						ChunkPos pos = new ChunkPos(x, z);
						chunkData.add(new ChunkSummary(GnC.ckMgr.getChunk(pos), ownerName(pos, server)));
					}
				}
				//Start mapColors gathering
				int baseX = center.getXStart()-(16*5);
				int baseZ = center.getZStart()-(16*5);
				int[][] mapColors = new int[176][176];
				for (int x = baseX; x < baseX+176; x++) {
					for (int z = baseZ; z < baseZ+176; z++) {
						for (int y = 255; y >= 0; y--) {
							if (!server.getBlockState(new BlockPos(x, y, z)).isAir()) {
								mapColors[x-baseX][z-baseZ] = server.getBlockState(new BlockPos(x, y, z)).getMaterialColor(server.getWorld(), new BlockPos(x, y, z)).colorValue;
								break;
							}
						}
					}
				}
				//end mapColors gathering
				double balG = gid.equals(GnC.NIL) ? 0.0 : AccountUtils.getBalance(gid);
				double balP = AccountUtils.getBalance(ctx.get().getSender().getUniqueID());
				Networking.sendToClient(new PacketOpenGui_Land(balG, balP, guild, mapColors, center, chunkData), ctx.get().getSender()); 
				break;
			}
			case 1: {break;}
			case 2: {break;}
			default:
			}
			
		});
		return true;
	}
	
	private String ownerName(ChunkPos pos, ServerWorld server) {
		if (GnC.ckMgr.getChunk(pos).owner.equals(GnC.NIL)) {return "Unowned";}
		if (GnC.gMgr.getGuildByID(GnC.ckMgr.getChunk(pos).owner) != null) {return GnC.gMgr.getGuildByID(GnC.ckMgr.getChunk(pos).owner).name;}
		if (server.getPlayerByUuid(GnC.ckMgr.getChunk(pos).owner) != null) {return server.getPlayerByUuid(GnC.ckMgr.getChunk(pos).owner).getDisplayName().toString();}
		return "Logical Error";
	}
}
