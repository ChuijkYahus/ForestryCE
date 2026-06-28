package forestry.apiculture;

import forestry.api.climate.IClimateProvider;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.IMutation;
import forestry.api.genetics.IMutationCondition;
import forestry.api.genetics.MutationConditionType;
import forestry.core.genetics.mutations.MutationConditionCave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

// Duplicate of MutationConditionCave; shares its (field-less) serialization type.
public class CaveMutationCondition implements IMutationCondition {
	@Override
	public float modifyChance(Level level, BlockPos pos, IMutation<?> mutation, IGenome firstGenome, IGenome secondGenome, IClimateProvider climate, float currentChance) {
		for (Direction direction : Direction.VALUES) {
			if (level.getBrightness(LightLayer.SKY, pos.relative(direction)) > 0) {
				return 0;
			}
		}
		return currentChance;
	}

	@Override
	public Component getDescription() {
		return Component.translatable("for.mutation.condition.underground");
	}

	@Override
	public MutationConditionType<?> type() {
		return MutationConditionCave.TYPE;
	}
}
