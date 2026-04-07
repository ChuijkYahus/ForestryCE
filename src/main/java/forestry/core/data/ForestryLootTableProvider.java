package forestry.core.data;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ForestryLootTableProvider extends LootTableProvider {
	public ForestryLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, Set.of(), List.of(
			new SubProviderEntry(ForestryBlockLootTables::new, LootContextParamSets.BLOCK),
			new SubProviderEntry(ForestryChestLootTables::new, LootContextParamSets.CHEST)
		), lookupProvider);
	}
}
