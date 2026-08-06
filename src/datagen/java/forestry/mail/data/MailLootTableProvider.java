package forestry.mail.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Generates the loot table pools for the mail jar, delegating to {@link MailBlockLootTables}.
 */
public class MailLootTableProvider extends LootTableProvider {
	public MailLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, Set.of(), List.of(
			new SubProviderEntry(MailBlockLootTables::new, LootContextParamSets.BLOCK)
		), lookupProvider);
	}
}
