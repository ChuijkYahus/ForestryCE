package forestry.mail.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.mail.blocks.BlockTypeMail;
import forestry.mail.postoffice.MailboxBlockEntity;
import forestry.mail.postoffice.StampCollectorBlockEntity;
import forestry.mail.tradestation.TradeStationBlockEntity;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureTileType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class MailTiles {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.MAIL);

	public static final FeatureTileType<MailboxBlockEntity> MAILBOX = REGISTRY.tile(MailboxBlockEntity::new, "mailbox", () -> MailBlocks.BASE.get(BlockTypeMail.MAILBOX).collect());
	public static final FeatureTileType<StampCollectorBlockEntity> STAMP_COLLECTOR = REGISTRY.tile(StampCollectorBlockEntity::new, "stamp_collector", () -> MailBlocks.BASE.get(BlockTypeMail.STAMP_COLLETOR).collect());
	public static final FeatureTileType<TradeStationBlockEntity> TRADER = REGISTRY.tile(TradeStationBlockEntity::new, "trader", () -> MailBlocks.BASE.get(BlockTypeMail.TRADE_STATION).collect());
}
