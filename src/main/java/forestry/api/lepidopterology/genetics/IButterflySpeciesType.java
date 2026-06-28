package forestry.api.lepidopterology.genetics;

import com.mojang.authlib.GameProfile;
import forestry.api.genetics.IBreedingTracker;
import forestry.api.genetics.ISpeciesType;
import forestry.api.lepidopterology.IButterflyCocoon;
import forestry.api.lepidopterology.IButterflyEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;

public interface IButterflySpeciesType extends ISpeciesType<IButterflySpecies, IButterfly> {
	@Override
	IBreedingTracker getBreedingTracker(LevelAccessor level, @Nullable GameProfile profile);

	/**
	 * @return The cocoon registered with the given ID. Backs the {@code cocoon} reference chromosome.
	 */
	IButterflyCocoon getCocoon(ResourceLocation id);

	/**
	 * @return The butterfly effect registered with the given ID. Backs the {@code butterfly_effect} reference chromosome.
	 */
	IButterflyEffect getButterflyEffect(ResourceLocation id);

	/**
	 * Spawns the given butterfly in the world.
	 *
	 * @return butterfly entity on success, null otherwise.
	 */
	Mob spawnButterflyInWorld(Level level, IButterfly butterfly, double x, double y, double z);

	@Nullable
	BlockPos plantCocoon(LevelAccessor level, BlockPos pos, IButterfly caterpillar, int age, boolean createNursery);

	/**
	 * @return true if passed item is mated.
	 */
	boolean isMated(ItemStack stack);
}
