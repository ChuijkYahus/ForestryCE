package forestry.apiculture.items;

import forestry.core.platform.item.ItemOverlay;
import net.minecraft.network.chat.TextColor;

import java.util.Locale;

public enum EnumPropolis implements ItemOverlay.IOverlayInfo {
	NORMAL(TextColor.fromRgb(0xc5b24e)),
	PULSATING(TextColor.fromRgb(0x2ccdb1)),
	SILKY(TextColor.fromRgb(0xddff00)),
	;

	private final String name;
	private final int primaryColor;

	EnumPropolis(TextColor color) {
		this.name = toString().toLowerCase(Locale.ENGLISH);
		this.primaryColor = color.getValue();
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	@Override
	public int getPrimaryColor() {
		return this.primaryColor;
	}

	@Override
	public int getSecondaryColor() {
		return 0;
	}
}
