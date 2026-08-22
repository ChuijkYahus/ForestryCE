package forestry.mail.client;

import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import forestry.core.platform.models.ClientManager;
import forestry.mail.features.MailItems;
import forestry.api.client.IClientModuleHandler;
import forestry.mail.features.MailMenuTypes;
import forestry.mail.gui.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class MailClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(MailClientHandler::registerItemColors);
		modBus.addListener(MailClientHandler::registerMenuScreens);
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(MailMenuTypes.CATALOGUE.menuType(), CatalogScreen::new);
		event.register(MailMenuTypes.LETTER.menuType(), LetterScreen::new);
		event.register(MailMenuTypes.MAILBOX.menuType(), MailboxScreen::new);
		event.register(MailMenuTypes.STAMP_COLLECTOR.menuType(), StampCollectorScreen::new);
		event.register(MailMenuTypes.TRADE_NAME.menuType(), TradeStationNamingScreen::new);
		event.register(MailMenuTypes.TRADER.menuType(), TradeStationScreen::new);
	}

	private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register(ClientManager.FORESTRY_ITEM_COLOR, MailItems.STAMPS.itemArray());
	}

}
