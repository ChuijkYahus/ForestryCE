package forestry.lepidopterology.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class LepidopterologyLootTableProvider extends LootTableProvider {
	public LepidopterologyLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, Set.of(), List.of(
			new SubProviderEntry(LepidopterologyBlockLootTables::new, LootContextParamSets.BLOCK)
		), lookupProvider);
	}
}
