package forestry.core.blocks;

import forestry.api.core.IBlockSubtype;
import net.minecraft.world.level.material.MapColor;

import java.util.Locale;

public enum BlockTypeJumboCandle implements IBlockSubtype {

	NORMAL(MapColor.SAND),
	WHITE(MapColor.WOOL),
	LIGHT_GRAY(MapColor.COLOR_LIGHT_GRAY),
	GRAY(MapColor.COLOR_GRAY),
	BLACK(MapColor.COLOR_BLACK),
	BROWN(MapColor.COLOR_BROWN),
	RED(MapColor.COLOR_RED),
	ORANGE(MapColor.COLOR_ORANGE),
	YELLOW(MapColor.COLOR_YELLOW),
	LIME(MapColor.COLOR_LIGHT_GREEN),
	GREEN(MapColor.COLOR_GREEN),
	CYAN(MapColor.COLOR_CYAN),
	LIGHT_BLUE(MapColor.COLOR_LIGHT_BLUE),
	BLUE(MapColor.COLOR_BLUE),
	PURPLE(MapColor.COLOR_PURPLE),
	MAGENTA(MapColor.COLOR_MAGENTA),
	PINK(MapColor.COLOR_PINK),
	RGB(MapColor.COLOR_MAGENTA);


	BlockTypeJumboCandle(MapColor col) {
		this.color = col;
	}

	private final MapColor color;

	public MapColor getMapColor(){
		return this.color;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ENGLISH);
	}

	public String getName(){
		return name().toLowerCase(Locale.ENGLISH).concat("_jumbo_candle");
	}
}
