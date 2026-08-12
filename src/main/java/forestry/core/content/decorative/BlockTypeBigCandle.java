package forestry.core.content.decorative;

import forestry.api.core.IBlockSubtype;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * The nineteen big candles, one half block each.
 */
public enum BlockTypeBigCandle implements IBlockSubtype {
	NORMAL(null),
	WHITE(Tags.Items.DYES_WHITE),
	LIGHT_GRAY(Tags.Items.DYES_LIGHT_GRAY),
	GRAY(Tags.Items.DYES_GRAY),
	BLACK(Tags.Items.DYES_BLACK),
	BROWN(Tags.Items.DYES_BROWN),
	RED(Tags.Items.DYES_RED),
	ORANGE(Tags.Items.DYES_ORANGE),
	YELLOW(Tags.Items.DYES_YELLOW),
	LIME(Tags.Items.DYES_LIME),
	GREEN(Tags.Items.DYES_GREEN),
	CYAN(Tags.Items.DYES_CYAN),
	LIGHT_BLUE(Tags.Items.DYES_LIGHT_BLUE),
	BLUE(Tags.Items.DYES_BLUE),
	PURPLE(Tags.Items.DYES_PURPLE),
	MAGENTA(Tags.Items.DYES_MAGENTA),
	PINK(Tags.Items.DYES_PINK),
	REFRACTORY(null),
	RAINBOW(null);

	@Nullable
	private final TagKey<Item> dye;

	BlockTypeBigCandle(@Nullable TagKey<Item> dye) {
		this.dye = dye;
	}

	/**
	 * @return The dye tag the dyeing recipe takes, or null where the candle is made from wax
	 */
	@Nullable
	public TagKey<Item> getDye() {
		return this.dye;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ENGLISH);
	}
}
