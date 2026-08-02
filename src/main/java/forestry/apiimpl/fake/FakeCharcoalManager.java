package forestry.apiimpl.fake;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.state.BlockState;

import forestry.api.arboriculture.ICharcoalManager;
import forestry.api.arboriculture.ICharcoalPileWall;

/**
 * The charcoal manager used when the arboriculture module is absent. Returned by
 * {@link FakeTreeManager#getCharcoalManager()}, which promises non-null.
 */
@SuppressWarnings("deprecation")
public enum FakeCharcoalManager implements ICharcoalManager {
	INSTANCE;

	@Nullable
	@Override
	public ICharcoalPileWall getWall(BlockState state) {
		return null;
	}

	@Override
	public List<ICharcoalPileWall> getWalls() {
		return List.of();
	}
}
