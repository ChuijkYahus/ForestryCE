package forestry.api.apiculture.genetics;

import forestry.api.ForestryConstants;
import forestry.api.genetics.ILifeStage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Locale;

public enum BeeLifeStage implements ILifeStage {
	DRONE(ForestryConstants.forestry("drone_bee")),
	PRINCESS(ForestryConstants.forestry("princess_bee")),
	QUEEN(ForestryConstants.forestry("queen_bee")),
	LARVAE(ForestryConstants.forestry("larvae_bee"));

	private final String name;
	// resolved on demand, not held: apiculture registers these items and builds them from
	// these same constants, so holding the item would be a class-init cycle
	private final ResourceLocation itemId;

	BeeLifeStage(ResourceLocation itemId) {
		this.name = name().toLowerCase(Locale.ENGLISH);
		this.itemId = itemId;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	@Override
	public Item getItemForm() {
		return BuiltInRegistries.ITEM.get(this.itemId);
	}
}
