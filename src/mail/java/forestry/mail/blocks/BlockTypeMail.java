package forestry.mail.blocks;

import forestry.core.platform.block.IBlockType;
import forestry.core.platform.block.IMachineProperties;
import forestry.core.platform.block.MachineProperties;
import forestry.core.platform.tile.IForestryTicker;
import forestry.core.platform.tile.TileForestry;
import forestry.mail.features.MailTiles;
import forestry.mail.postoffice.TileStampCollector;
import forestry.mail.tradestation.TileTrader;
import forestry.core.platform.registration.FeatureTileType;

import javax.annotation.Nullable;

public enum BlockTypeMail implements IBlockType {
	MAILBOX(MailTiles.MAILBOX, "mailbox", null),
	TRADE_STATION(MailTiles.TRADER, "trade_station", TileTrader::serverTick),
	STAMP_COLLETOR(MailTiles.STAMP_COLLECTOR, "stamp_collector", TileStampCollector::serverTick);

	private final IMachineProperties<?> machineProperties;

	<T extends TileForestry> BlockTypeMail(FeatureTileType<T> teClass, String name, @Nullable IForestryTicker<T> serverTicker) {
		this.machineProperties = new MachineProperties.Builder<>(teClass, name).setServerTicker(serverTicker).create();
	}

	@Override
	public IMachineProperties<?> getMachineProperties() {
		return this.machineProperties;
	}

	@Override
	public String getSerializedName() {
		return getMachineProperties().getSerializedName();
	}
}
