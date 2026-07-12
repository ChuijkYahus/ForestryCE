package forestry.apiculture;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class WaterTagFlowerType extends TagFlowerType {
	public WaterTagFlowerType(TagKey<Block> acceptableFlowers, boolean dominant) {
		super(acceptableFlowers, dominant);
	}

	public WaterTagFlowerType(TagKey<Block> acceptableFlowers, boolean dominant, @Nullable TagKey<Biome> biomes) {
		super(acceptableFlowers, dominant, biomes);
	}

	@Override
	public boolean isPlantablePosition(Level level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() == Blocks.WATER;
	}
}
