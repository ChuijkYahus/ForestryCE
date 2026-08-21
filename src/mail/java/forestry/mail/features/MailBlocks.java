package forestry.mail.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.mail.blocks.MailBlock;
import forestry.mail.blocks.BlockTypeMail;
import forestry.core.platform.registration.FeatureBlockGroup;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

import java.util.List;

@FeatureProvider
public class MailBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.MAIL);

	public static final FeatureBlockGroup<MailBlock, BlockTypeMail> BASE = REGISTRY.blockGroup(MailBlock::new, List.of(BlockTypeMail.values())).item(ItemBlockForestry::new).create();
}
