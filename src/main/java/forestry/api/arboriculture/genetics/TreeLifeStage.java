package forestry.api.arboriculture.genetics;

import forestry.api.ForestryConstants;
import forestry.api.core.genetics.ILifeStage;
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

	/**
	 * @return The registry id of this life stage's item form. Arboriculture registers the item
	 * under this id, so the two cannot drift
	 */
	public ResourceLocation itemId() {
		return this.itemId;
	}

	public String getSerializedName() {
		return this.name;
	}

	@Override
	public Item getItemForm() {
		return BuiltInRegistries.ITEM.get(this.itemId);
	}
}
