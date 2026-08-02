package forestry.core.network.packets;

import forestry.api.IForestryApi;
import forestry.api.core.ForestryEvent;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.ISpeciesType;
import forestry.core.genetics.BreedingTracker;
import forestry.core.network.PacketIdClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;

public record PacketGenomeTrackerSync(@Nullable CompoundTag nbt) implements CustomPacketPayload {
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdClient.GENOME_TRACKER_UPDATE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketGenomeTrackerSync msg) {
		buffer.writeNbt(msg.nbt);
	}

	public static PacketGenomeTrackerSync decode(RegistryFriendlyByteBuf buffer) {
		return new PacketGenomeTrackerSync(buffer.readNbt());
	}

	public static void handle(PacketGenomeTrackerSync msg, Player player) {
		if (msg.nbt != null) {
			String type = msg.nbt.getString(BreedingTracker.TYPE_KEY);
			ISpeciesType<?, ?> root = IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypeSafe(ResourceLocation.parse(type));

			if (root != null) {
				IBreedingTracker tracker = root.getBreedingTracker(player.getCommandSenderWorld(), player.getGameProfile());
				tracker.load(msg.nbt);
				NeoForge.EVENT_BUS.post(new ForestryEvent.SyncedBreedingTracker(tracker, player));
			}
		}
	}
}
