package forestry.core.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Used for simple, burnable blocks.
 */
public abstract class BlockBurnable extends Block {

	private final int FLAMMABILITY;
	private final int SPREAD_SPEED;

	public BlockBurnable(Properties properties, int flammability, int speadSpeed) {
		super(properties);
		this.FLAMMABILITY = flammability;
		this.SPREAD_SPEED = speadSpeed;
	}

	public BlockBurnable(Properties properties, int flammability) {
		super(properties);
		this.FLAMMABILITY = flammability;
		this.SPREAD_SPEED = flammability;
	}

	public BlockBurnable(Properties properties) {
		super(properties);
		this.FLAMMABILITY = 45; //I think this is the default
		this.SPREAD_SPEED = 45;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return this.FLAMMABILITY;
	}

	@Override
	public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return true;
	}

	@Override
	public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
		super.onCaughtFire(state, level, pos, direction, igniter);
		level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3); //???
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return this.SPREAD_SPEED ;
	}

	@Override
	public boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) {
		return true;
	}



}
