 package dicemc.gnc.land.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dicemc.gnc.GnC;
import dicemc.gnc.account.AccountManager;
import dicemc.gnc.guild.Guild;
import dicemc.gnc.land.WhitelistItem;
import dicemc.gnc.land.client.GuiLandManager.ChunkSummary;
import dicemc.gnc.setup.Networking;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

public class PacketChunkDataToServer {
	private final PkType action;
	private final ChunkPos pos;
	private UUID id = GnC.NIL;
	private double dbl = 0d;
	private int igr = 0;
	private String str = "";
		
	public enum PkType {
		TEMPCLAIM((packet, ctx) -> GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).tempClaim(packet.pos, packet.id)), 
		GUILDCLAIM((packet, ctx) -> GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).guildClaim(packet.pos, packet.id)), 
		ABANDON((packet, ctx) -> ""), 
		EXTEND((packet, ctx) -> GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).extendClaim(packet.pos, packet.id)), 
		SELL((packet, ctx) -> ""), 
		UPDATESUB((packet, ctx) -> ""), 
		PUBLIC((packet, ctx) -> GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).publicToggle(packet.pos, packet.igr == 1)), 
		MINRANK((packet, ctx) -> ""), 
		DISABLESUB((packet, ctx) -> ""), 
		BREAK((packet, ctx) -> {
			CompoundNBT nbt = new CompoundNBT();
			try {nbt = JsonToNBT.getTagFromJson(packet.str);} catch (CommandSyntaxException e) {}
			return GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).updateWhitelistItem(packet.pos, new WhitelistItem(nbt)); 
		}),
		INTERACT((packet, ctx) -> {
			CompoundNBT nbt = new CompoundNBT();
			try {nbt = JsonToNBT.getTagFromJson(packet.str);} catch (CommandSyntaxException e) {}
			return GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).updateWhitelistItem(packet.pos, new WhitelistItem(nbt)); 
		}),
		CLEARWL((packet, ctx) -> GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).setWhitelist(packet.pos, new ArrayList<WhitelistItem>())), 
		MEMBER((packet, ctx) -> {
			Map<UUID, String> map = GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).getPlayers(packet.pos);
			for (Map.Entry<UUID, String> entry : map.entrySet()) {if (entry.getValue().equalsIgnoreCase(packet.str)) {return GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).removePlayer(packet.pos, entry.getKey());}}
			UUID player = ctx.get().getSender().getServer().getPlayerProfileCache().getGameProfileForUsername(packet.str).getId();
			return GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).addPlayer(packet.pos, player);
		});
	
		public final BiFunction<PacketChunkDataToServer, Supplier<NetworkEvent.Context>, String> packetHandler;
		
		PkType(BiFunction<PacketChunkDataToServer, Supplier<NetworkEvent.Context>, String> packetHandler) { this.packetHandler = packetHandler;}
	}
	
	public PacketChunkDataToServer(PacketBuffer buf) {
		action = PkType.values()[buf.readVarInt()];
		pos = new ChunkPos(buf.readLong());
		id = buf.readUniqueId();
		dbl = buf.readDouble();
		igr = buf.readInt();
		str = buf.readString();
	}
	
	/**
	 * Use with actions ABANDON, DISABLESUB, and CLEARWL only.
	 * @param action the PkType enum indiating what action to take with the provided data
	 * @param pos the chunk position being changed
	 */
	public PacketChunkDataToServer(PkType action, ChunkPos pos) {
		this.action = action;
		this.pos = pos;
	}
	
	/**
	 * Use with actions TEMPCLAIM, EXTEND, and GUILDCLAIM only
	 * @param action the PkType enum indiating what action to take with the provided data
	 * @param pos the chunk position being changed
	 * @param id the player or guild UUID executing the claim
	 */
	public PacketChunkDataToServer(PkType action, ChunkPos pos, UUID id) {
		this.action = action;
		this.pos = pos;
		this.id = id;
	}
	
	/**
	 * Use with action SELL only
	 * @param action the PkType enum indiating what action to take with the provided data
	 * @param pos the chunk position being changed
	 * @param value the price being sold for
	 */
	public PacketChunkDataToServer(PkType action, ChunkPos pos, double value) {
		this.action = PkType.SELL;
		this.pos = pos;
		this.dbl = value;
	}
	
	/**
	 * use with action UPDATESUB only
	 * @param action the PkType enum indiating what action to take with the provided data
	 * @param pos the chunk position being changed
	 * @param rate the rental price
	 * @param hours the rental duration in hours
	 */
	public PacketChunkDataToServer(PkType action, ChunkPos pos, double rate, int hours) {
		this.action = PkType.UPDATESUB;
		this.pos = pos;
		this.dbl = rate;
		this.igr = hours;
	}
	
	/**
	 * Use with actions PUBLIC and MINRANK only
	 * @param action the PkType enum indiating what action to take with the provided data
	 * @param pos the chunk position being changed
	 * @param change for PUBLIC action enter desired value.
	 * for MINRANK true equals an increase, false equals decrease.
	 */
	public PacketChunkDataToServer(PkType action, ChunkPos pos, boolean change) {
		this.action = action;
		this.pos = pos;
		this.igr = (change ? 1 : -1);
	}
	
	/**
	 * Use with action MEMBER, INTERACT, and BREAK only
	 * @param action the PkType enum indiating what action to take with the provided data
	 * @param pos the chunk position being changed
	 * @param name of member being added/removed from the list or raw NBT string of a WhitelistItem
	 */
	public PacketChunkDataToServer(PkType action, ChunkPos pos, String name) {
		this.action = action;
		this.pos = pos;
		this.str = name;
	}
	
	public void toBytes(PacketBuffer buf) {
		buf.writeVarInt(action.ordinal());
		buf.writeLong(pos.asLong());
		buf.writeUniqueId(id);
		buf.writeDouble(dbl);
		buf.writeInt(igr);
		buf.writeString(str);
		}
 	
	public boolean handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			String resp = this.action.packetHandler.apply(this, ctx);
			//Gather updated information from current server status
			UUID gid = GnC.NIL;
			Guild guild = new Guild("N/A", GnC.NIL, false);
			if (GnC.gMgr.getGuildByMember(ctx.get().getSender().getUniqueID()) != null) {
				guild = GnC.gMgr.getGuildByMember(ctx.get().getSender().getUniqueID());
			}
			ServerWorld server = ctx.get().getSender().getServerWorld();
			ChunkPos center = new ChunkPos(ctx.get().getSender().chunkCoordX, ctx.get().getSender().chunkCoordZ);
			List<ChunkSummary> chunkData = new ArrayList<ChunkSummary>();
			for (int x = center.x-6; x <= center.x+6; x++) {
				for (int z = center.z-6; z <= center.z+6; z++) {
					ChunkPos pos = new ChunkPos(x, z);
					chunkData.add(new ChunkSummary(GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).getChunk(pos), ownerName(pos, server, ctx)));
				}
			}
			double balG = gid.equals(GnC.NIL) ? 0.0 : GnC.aMgr.getBalance(gid);
			double balP = GnC.aMgr.getBalance(ctx.get().getSender().getUniqueID());
			Networking.sendToClient(new PacketUpdateChunkManagerGui(balG, balP, guild, chunkData, resp), ctx.get().getSender()); 
		});
		return true;
	}
	
	private String ownerName(ChunkPos pos, ServerWorld server, Supplier<NetworkEvent.Context> ctx) {
		if (GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).getChunk(pos).owner.equals(GnC.NIL) &&
				GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).getChunk(pos).renter.equals(GnC.NIL)) {
				return "Unowned";
			}
			if (GnC.gMgr.getGuildByID(GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).getChunk(pos).owner) != null) {
				return GnC.gMgr.getGuildByID(GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).getChunk(pos).owner).name;
			}
			if (GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).getChunk(pos).owner.equals(GnC.NIL) &&
				!GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).getChunk(pos).renter.equals(GnC.NIL)) {
				return server.getPlayerByUuid(GnC.wldMgr.get(ctx.get().getSender().getEntityWorld().func_234923_W_()).getChunk(pos).renter).getScoreboardName();
			}
			return "Logical Error";
	}
}
