package forestry.arboriculture.charcoal;

import com.google.common.base.Preconditions;
import forestry.api.arboriculture.ICharcoalManager;
import forestry.api.arboriculture.ICharcoalPileWall;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CharcoalManager implements ICharcoalManager {
	// Charcoal
	public static final int charcoalAmountBase = 8;
	public static final int charcoalWallCheckRange = 16;
	private final List<ICharcoalPileWall> walls = new ArrayList<>();

	@Override
	public void registerWall(Block block, int amount) {
		Preconditions.checkNotNull(block, "block must not be null.");
		Preconditions.checkArgument(amount > (-charcoalAmountBase) && amount < (63 - charcoalAmountBase), "amount must be bigger than -10 and smaller than 64.");
        this.walls.add(new CharcoalPileWall(block, amount));
	}

	@Override
	public void registerWall(BlockState blockState, int amount) {
		Preconditions.checkNotNull(blockState, "block state must not be null.");
		Preconditions.checkArgument(amount > (-charcoalAmountBase) && amount < (63 - charcoalAmountBase), "amount must be bigger than -10 and smaller than 64.");
        this.walls.add(new CharcoalPileWall(blockState, amount));
	}

	@Override
	public void registerWall(ICharcoalPileWall wall) {
        this.walls.add(wall);
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
	public boolean removeWall(Block block) {
		return removeWall(block.defaultBlockState());
	}

	@Override
	public boolean removeWall(BlockState state) {
		for (ICharcoalPileWall wall : this.walls) {
			if (wall.matches(state)) {
				return this.walls.remove(wall);
			}
		}
		return false;
	}

	@Override
	public List<ICharcoalPileWall> getWalls() {
		return this.walls;
	}
}
