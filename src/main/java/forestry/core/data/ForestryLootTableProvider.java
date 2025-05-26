package forestry.core.data;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public class ForestryLootTableProvider extends LootTableProvider {
	public ForestryLootTableProvider(PackOutput pOutput) {
		super(pOutput, Set.of(), List.of(
			new SubProviderEntry(ForestryBlockLootTables::new, LootContextParamSets.BLOCK),
			new SubProviderEntry(ForestryChestLootTables::new, LootContextParamSets.CHEST)
		));
	}
}
