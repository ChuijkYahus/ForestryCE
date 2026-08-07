package forestry.mail.data;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import forestry.api.ForestryConstants;
import forestry.core.data.models.ForestryBlockStateProvider;
import forestry.mail.blocks.BlockTypeMail;
import forestry.mail.features.MailBlocks;

/**
 * Generates the blockstates and block models for the mail jar. All three machines are horizontal-facing
 * cubes, and their item models stay hand-authored for the custom display transforms.
 */
public class MailBlockStateProvider extends BlockStateProvider {
	public MailBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		super(output, ForestryConstants.MOD_ID, existingFileHelper);
	}

	@Override
	protected void registerStatesAndModels() {
		ForestryBlockStateProvider.horizontalMachine(this, MailBlocks.BASE.get(BlockTypeMail.MAILBOX).block(), "mailbox", 0, 1, 2, 2, 2, 2, 2);
		ForestryBlockStateProvider.horizontalMachine(this, MailBlocks.BASE.get(BlockTypeMail.STAMP_COLLETOR).block(), "philatelist", 0, 1, 3, 2, 2, 2, 2);
		ForestryBlockStateProvider.horizontalMachine(this, MailBlocks.BASE.get(BlockTypeMail.TRADE_STATION).block(), "tradestation", 0, 1, 3, 2, 4, 4, 4);
	}
}
