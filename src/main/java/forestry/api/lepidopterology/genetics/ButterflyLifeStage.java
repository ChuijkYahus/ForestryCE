package forestry.api.lepidopterology.genetics;

import forestry.api.ForestryConstants;
import forestry.api.genetics.ILifeStage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Locale;

public enum ButterflyLifeStage implements ILifeStage {
	BUTTERFLY(ForestryConstants.forestry("butterfly")),
	SERUM(ForestryConstants.forestry("butterfly_serum")),
	CATERPILLAR(ForestryConstants.forestry("caterpillar")),
	COCOON(ForestryConstants.forestry("cocoon"));

	private final String name;
	// resolved on demand, not held: lepidopterology registers these items and builds them from
	// these same constants, so holding the item would be a class-init cycle
	private final ResourceLocation itemId;

	ButterflyLifeStage(ResourceLocation itemId) {
		this.name = name().toLowerCase(Locale.ENGLISH);
		this.itemId = itemId;
	}

	public String getSerializedName() {
		return this.name;
	}

	@Override
	public Item getItemForm() {
		return BuiltInRegistries.ITEM.get(this.itemId);
	}
}
