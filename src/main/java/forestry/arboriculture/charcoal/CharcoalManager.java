package forestry.arboriculture.charcoal;

import com.google.common.base.Preconditions;
import forestry.api.arboriculture.ICharcoalManager;
import forestry.api.arboriculture.ICharcoalPileWall;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CharcoalManager implements ICharcoalManager {
	public static final int charcoalAmountBase = 8;
	public static final int charcoalWallCheckRange = 16;
	private final List<ICharcoalPileWall> walls = new ArrayList<>();

	/**
	 * Internal-only adder used by {@code ArboricultureRegistration.registerCharcoalPitWall}.
	 * Plugins must register through {@link forestry.api.plugin.IArboricultureRegistration#registerCharcoalPitWall}.
	 */
	public void addWall(BlockState blockState, int amount) {
		Preconditions.checkNotNull(blockState, "block state must not be null.");
		Preconditions.checkArgument(amount > (-charcoalAmountBase) && amount < (63 - charcoalAmountBase), "amount must be bigger than -10 and smaller than 64.");
		this.walls.add(new CharcoalPileWall(blockState, amount));
	}

	@Nullable
	@Override
	public ICharcoalPileWall getWall(BlockState state) {
		for (ICharcoalPileWall wall : this.walls) {
			if (wall.matches(state)) {
				return wall;
			}
		}
		return null;
	}

	@Override
	public List<ICharcoalPileWall> getWalls() {
		return this.walls;
	}
}
