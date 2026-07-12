package forestry.apiculture;

import java.util.HashSet;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import forestry.api.ForestryTags;
import forestry.api.apiculture.IFlowerType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class TagFlowerType implements IFlowerType {
	protected final TagKey<Block> acceptableFlowers;
	protected final boolean dominant;
	@Nullable
	protected final TagKey<Biome> biomes;

	public TagFlowerType(TagKey<Block> acceptableFlowers, boolean dominant) {
		this(acceptableFlowers, dominant, null);
	}

	public TagFlowerType(TagKey<Block> acceptableFlowers, boolean dominant, @Nullable TagKey<Biome> biomes) {
		this.acceptableFlowers = acceptableFlowers;
		this.dominant = dominant;
		this.biomes = biomes;
	}

	@Override
	public boolean isAcceptableFlower(Level level, BlockPos pos) {
		if (this.biomes != null && level.getBiome(pos).is(this.biomes)) {
			return true;
		}
		return level.getBlockState(pos).is(this.acceptableFlowers);
	}

	@Override
	public boolean plantRandomFlower(Level level, BlockPos pos, List<BlockState> nearbyFlowers) {
		if (level.hasChunkAt(pos) && isPlantablePosition(level, pos)) {
			ObjectArrayList<BlockState> uniqueNearbyFlowers = new ObjectArrayList<>(new HashSet<>(nearbyFlowers));
			Util.shuffle(uniqueNearbyFlowers, level.random);

			for (BlockState state : uniqueNearbyFlowers) {
				if (state.is(ForestryTags.Blocks.PLANTABLE_FLOWERS) && state.canSurvive(level, pos)) {
					if (state.hasProperty(DoublePlantBlock.HALF)) {
						BlockPos topPos = pos.above();
						if (level.isEmptyBlock(topPos)) {
							return level.setBlockAndUpdate(pos, state.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER))
								&& level.setBlockAndUpdate(topPos, state.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
						}
					} else {
						return level.setBlockAndUpdate(pos, state);
					}
				}
			}
		}
		return false;
	}

	public boolean isPlantablePosition(Level level, BlockPos pos) {
		return level.isEmptyBlock(pos);
	}

	@Override
	public boolean isDominant() {
		return this.dominant;
	}

	public TagKey<Block> acceptableFlowers() {
		return this.acceptableFlowers;
	}

	public boolean dominant() {
		return this.dominant;
	}

	@Nullable
	public TagKey<Biome> biomes() {
		return this.biomes;
	}
}
