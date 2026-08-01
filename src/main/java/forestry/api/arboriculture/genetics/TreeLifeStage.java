package forestry.api.arboriculture.genetics;

import forestry.api.ForestryConstants;
import forestry.api.genetics.ILifeStage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Locale;

public enum TreeLifeStage implements ILifeStage {
	SAPLING(ForestryConstants.forestry("tree_sapling")),
	POLLEN(ForestryConstants.forestry("tree_pollen"));

	private final String name;
	// resolved on demand, not held: arboriculture registers these items and builds them from
	// these same constants, so holding the item would be a class-init cycle
	private final ResourceLocation itemId;

	TreeLifeStage(ResourceLocation itemId) {
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
