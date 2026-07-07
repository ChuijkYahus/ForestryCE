package forestry.core.blocks;

import forestry.api.core.IBlockSubtype;
import net.minecraft.world.level.material.MapColor;

import java.util.Locale;

public enum BlockTypeBigCandle implements IBlockSubtype {
	NORMAL,
	WHITE,
	LIGHT_GRAY,
	GRAY,
	BLACK,
	BROWN,
	RED,
	ORANGE,
	YELLOW,
	LIME,
	GREEN,
	CYAN,
	LIGHT_BLUE,
	BLUE,
	PURPLE,
	MAGENTA,
	PINK,
	REFRACTORY,
	RAINBOW;



	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ENGLISH);
	}

	public String getName(){
		return name().toLowerCase(Locale.ENGLISH).concat("_jumbo_candle");
	}
}
