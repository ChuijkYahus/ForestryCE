package forestry.core.items.definitions;

import forestry.api.core.IItemSubtype;
import forestry.core.items.ItemForestry;
import forestry.core.items.ItemOverlay;
import net.minecraft.network.chat.TextColor;

import java.util.Locale;

public enum EnumElectronTube implements IItemSubtype {
	COPPER,
	TIN,
	BRONZE,
	IRON,
	GOLD,
	DIAMOND,
	OBSIDIAN,
	BLAZE,
	EMERALD,
	APATITE,
	LAPIS,
	ENDER,
	AMBER,
	SILICON;

	private final String uid;

	EnumElectronTube() {
		this.uid = name().toLowerCase(Locale.ENGLISH);
	}

	@Override
	public String getSerializedName() {
		return this.uid;
	}

}
