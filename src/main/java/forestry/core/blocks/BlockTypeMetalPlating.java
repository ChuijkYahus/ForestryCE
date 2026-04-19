package forestry.core.blocks;

import forestry.api.core.IBlockSubtype;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.MapColor;
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

	//TODO: see BlockTypeJumboCandle
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

	public static Map<BlockTypeMetalPlating, MapColor> getMapColour(){
		return new HashMap<>(){{
			this.put(BlockTypeMetalPlating.BLUE, MapColor.COLOR_BLUE);
			this.put(BlockTypeMetalPlating.BLACK, MapColor.COLOR_BLACK);
			this.put(BlockTypeMetalPlating.CYAN, MapColor.COLOR_CYAN);
			this.put(BlockTypeMetalPlating.BRONZE, MapColor.TERRACOTTA_YELLOW);
			this.put(BlockTypeMetalPlating.GOLD, MapColor.GOLD);
			this.put(BlockTypeMetalPlating.BROWN, MapColor.COLOR_BROWN);
			this.put(BlockTypeMetalPlating.COPPER, MapColor.COLOR_ORANGE);
			this.put(BlockTypeMetalPlating.GRAY, MapColor.COLOR_GRAY);
			this.put(BlockTypeMetalPlating.GREEN, MapColor.COLOR_GREEN);
			this.put(BlockTypeMetalPlating.IRON, MapColor.METAL);
			this.put(BlockTypeMetalPlating.LIGHT_BLUE, MapColor.COLOR_LIGHT_BLUE);
			this.put(BlockTypeMetalPlating.LIGHT_GRAY, MapColor.COLOR_LIGHT_GRAY);
			this.put(BlockTypeMetalPlating.LIME, MapColor.COLOR_LIGHT_GREEN);
			this.put(BlockTypeMetalPlating.MAGENTA, MapColor.COLOR_MAGENTA);
			this.put(BlockTypeMetalPlating.NETHERITE, MapColor.COLOR_BLACK);
			this.put(BlockTypeMetalPlating.ORANGE, MapColor.COLOR_ORANGE);
			this.put(BlockTypeMetalPlating.PINK, MapColor.COLOR_PINK);
			this.put(BlockTypeMetalPlating.PURPLE, MapColor.COLOR_PURPLE);
			this.put(BlockTypeMetalPlating.RED, MapColor.COLOR_RED);
			this.put(BlockTypeMetalPlating.TIN, MapColor.METAL);
			this.put(BlockTypeMetalPlating.WHITE, MapColor.WOOL);
			this.put(BlockTypeMetalPlating.YELLOW, MapColor.COLOR_YELLOW);
		}};
	}
}
