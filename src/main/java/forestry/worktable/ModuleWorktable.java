package forestry.worktable;

import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.modules.IPacketRegistry;
import forestry.core.platform.network.PacketIdClient;
import forestry.core.platform.network.PacketIdServer;
import forestry.modules.BlankForestryModule;
import forestry.worktable.client.WorktableClientHandler;
import forestry.worktable.network.packets.PacketWorktableMemoryUpdate;
import forestry.worktable.network.packets.PacketWorktableRecipeRequest;
import forestry.worktable.network.packets.PacketWorktableRecipeUpdate;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

@ForestryModule
public class ModuleWorktable extends BlankForestryModule {
	@Override
	public ResourceLocation getId() {
		return ForestryModuleIds.WORKTABLE;
	}

	@Override
	public void registerPackets(IPacketRegistry registry) {
		registry.serverbound(PacketIdServer.WORKTABLE_RECIPE_REQUEST, PacketWorktableRecipeRequest::encode, PacketWorktableRecipeRequest::decode, PacketWorktableRecipeRequest::handle);

		registry.clientbound(PacketIdClient.WORKTABLE_MEMORY_UPDATE, PacketWorktableMemoryUpdate::encode, PacketWorktableMemoryUpdate::decode, PacketWorktableMemoryUpdate::handle);
		registry.clientbound(PacketIdClient.WORKTABLE_CRAFTING_UPDATE, PacketWorktableRecipeUpdate::encode, PacketWorktableRecipeUpdate::decode, PacketWorktableRecipeUpdate::handle);
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new WorktableClientHandler());
	}
}
