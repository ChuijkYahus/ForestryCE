package forestry.cultivation.blocks;

import forestry.core.blocks.BlockBase;
import forestry.core.render.ParticleRender;
import forestry.cultivation.tiles.TilePlanter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockPlanter extends BlockBase<BlockTypePlanter> {
	private final boolean manual;

	public BlockPlanter(BlockTypePlanter type, boolean manual) {
		super(type, Properties.of().noOcclusion());
		this.manual = manual;
	}

	public boolean isManual() {
		return this.manual;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		BlockEntity tile = super.newBlockEntity(pos, state);

		if (tile instanceof TilePlanter planter) {
			planter.setManual(this.manual);
		}

		return tile;
	}
}
