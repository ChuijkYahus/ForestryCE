package forestry.mail.client;

import forestry.api.client.IClientModuleHandler;
import forestry.mail.features.MailMenuTypes;
import forestry.mail.gui.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class MailClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(MailClientHandler::registerMenuScreens);
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(MailMenuTypes.CATALOGUE.menuType(), GuiCatalogue::new);
		event.register(MailMenuTypes.LETTER.menuType(), GuiLetter::new);
		event.register(MailMenuTypes.MAILBOX.menuType(), GuiMailbox::new);
		event.register(MailMenuTypes.STAMP_COLLECTOR.menuType(), GuiStampCollector::new);
		event.register(MailMenuTypes.TRADE_NAME.menuType(), GuiTradeName::new);
		event.register(MailMenuTypes.TRADER.menuType(), GuiTrader::new);
	}
}
