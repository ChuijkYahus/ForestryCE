package forestry.apiimpl.client.fake;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;

import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import forestry.api.client.arboriculture.ILeafSprite;
import forestry.api.client.arboriculture.ILeafTint;
import forestry.api.client.plugin.IClientHelper;

/**
 * The client helper used when the arboriculture module is absent. Every method on IClientHelper
 * builds an arboriculture object, so base can only return the api-level equivalents.
 *
 * <p>This one is not optional the way the managers are: ForestryLeafSprites resolves the helper from
 * a static initializer, so without a fallback the ServiceLoader lookup throws while any client is
 * starting, not only on the paths that draw leaves.
 */
public enum FakeClientHelper implements IClientHelper {
	INSTANCE;

	// Matches FixedLeafTint.NONE, the value the real helper returns
	private static final ILeafTint NONE = (level, pos) -> 0xffffff;

	@Override
	public ILeafTint createNoneTint() {
		return NONE;
	}

	@Override
	public ILeafTint createFixedTint(TextColor color) {
		int value = color.getValue();
		return (level, pos) -> value;
	}

	@Override
	public ILeafTint createBiomeTint() {
		return ILeafTint.DEFAULT;
	}

	@Override
	public ILeafTint createBiomeTint(Int2IntFunction mapper) {
		return (level, pos) -> mapper.applyAsInt(ILeafTint.DEFAULT.get(level, pos));
	}

	@Override
	public ILeafSprite createLeafSprite(ResourceLocation id) {
		return ILeafSprite.MISSING;
	}
}
