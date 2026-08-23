package forestry.arboriculture.leaves;

import forestry.api.arboriculture.genetics.ITree;
import forestry.api.client.IForestryClientApi;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.item.ITintedItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Item for the tile-less genetic leaves ({@link forestry.arboriculture.leaves.BlockDefaultLeaves} and
 * {@link forestry.arboriculture.leaves.BlockDefaultLeavesFruit}). Their stacks carry no genome NBT, so the species must
 * be resolved from the block's {@link ForestryLeafType} - exactly as {@code ItemBlockDecorativeLeaves} does for
 * decorative leaves. Using {@code ItemBlockLeaves} here (its no-NBT fallbacks) is what made these items show the raw
 * grammar name and a uniform default-foliage tint.
 */
public class ItemBlockDefaultLeaves extends ItemBlockForestry<BlockAbstractLeaves> implements ITintedItem {
	public ItemBlockDefaultLeaves(BlockAbstractLeaves block, Item.Properties properties) {
		super(block, properties);
	}

	private ForestryLeafType leafType() {
		return ((ILeafTypeBlock) getBlock()).getType();
	}

	@Override
	public Component getName(ItemStack itemStack) {
		return ItemBlockLeaves.getDisplayName(leafType().getIndividual().getSpecies());
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public int getColorFromItemStack(ItemStack itemStack, int renderPass) {
		ForestryLeafType type = leafType();
		if (renderPass == BlockAbstractLeaves.FRUIT_COLOR_INDEX) {
			return type.getFruit().getDecorativeColor();
		}
		ITree individual = type.getIndividual();
		return IForestryClientApi.INSTANCE.getTreeManager().getTint(individual.getSpecies()).get(null, null);
	}
}
