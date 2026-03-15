package forestry.core.blocks;

import forestry.api.core.IBlockSubtype;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.Tags;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum BlockTypeMetalPlating implements IBlockSubtype {

	GOLD,
	IRON,
	COPPER,
	TIN,
	BRONZE,
	NETHERITE,
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
	BLUE,
	LIGHT_BLUE,
	PURPLE,
	MAGENTA,
	PINK;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ENGLISH);
	}

	public String getName(){
		return name().toLowerCase(Locale.ENGLISH).concat("_metal_plating");
	}

	public static Map<TagKey<Item>, BlockTypeMetalPlating> getDye(){
		return new HashMap<>(){{
			this.put(Tags.Items.DYES_WHITE, WHITE);
			this.put(Tags.Items.DYES_LIGHT_GRAY, LIGHT_GRAY);
			this.put(Tags.Items.DYES_GRAY, GRAY);
			this.put(Tags.Items.DYES_BLACK, BLACK);
			this.put(Tags.Items.DYES_BROWN, BROWN);
			this.put(Tags.Items.DYES_RED, RED);
			this.put(Tags.Items.DYES_ORANGE, ORANGE);
			this.put(Tags.Items.DYES_YELLOW, YELLOW);
			this.put(Tags.Items.DYES_LIME, LIME);
			this.put(Tags.Items.DYES_GREEN, GREEN);
			this.put(Tags.Items.DYES_BLUE, BLUE);
			this.put(Tags.Items.DYES_CYAN, CYAN);
			this.put(Tags.Items.DYES_LIGHT_BLUE, LIGHT_BLUE);
			this.put(Tags.Items.DYES_PURPLE, PURPLE);
			this.put(Tags.Items.DYES_MAGENTA, MAGENTA);
			this.put(Tags.Items.DYES_PINK, PINK);
		}};
	}
}
