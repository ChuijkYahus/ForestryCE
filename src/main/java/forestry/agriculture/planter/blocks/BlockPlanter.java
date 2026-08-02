package forestry.agriculture.planter.blocks;

import forestry.core.platform.block.BlockBase;
import forestry.core.platform.render.ParticleRender;
import forestry.agriculture.planter.tiles.TilePlanter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockPlanter extends BlockBase<BlockTypePlanter> {
	private final boolean manual;

	public BlockPlanter(BlockTypePlanter type, boolean manual) {
		super(Properties.of().noOcclusion(), type);
		this.manual = manual;
	}

	public boolean isManual() {
		return this.manual;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
		if (this.blockType == BlockTypePlanter.FARM_ENDER) {
			for (int i = 0; i < 3; ++i) {
				ParticleRender.addPortalFx(level, pos, rand);
			}
		}
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
