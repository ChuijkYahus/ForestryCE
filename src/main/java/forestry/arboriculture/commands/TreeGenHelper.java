/*******************************************************************************
 * Copyright (c) 2011-2014 SirSengir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Various Contributors including, but not limited to:
 * SirSengir (original work), CovertJaguar, Player, Binnie, MysteriousAges
 ******************************************************************************/
package forestry.arboriculture.commands;

import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.genetics.IGenome;
import forestry.core.utils.BlockUtil;
import forestry.core.worldgen.FeatureBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import javax.annotation.Nullable;
import java.util.Optional;

// todo move into forestry.arboriculture.worldgen
public class TreeGenHelper {
	public static boolean generateTree(ITreeSpecies tree, @Nullable IGenome genome, WorldGenLevel level, RandomSource random, BlockPos pos) {
		Feature<NoneFeatureConfiguration> gen = tree.getGenerator().getTreeFeature(tree);

		BlockState state = level.getBlockState(pos);
		if (BlockUtil.canPlaceTree(state, level, pos)) {
			// todo require FeatureBase so that the genome is always respected
			if (gen instanceof FeatureBase base) {
				if (genome == null) {
					genome = tree.getDefaultGenome();
				}
				return base.place(genome, level, random, pos, true);
			} else {
				return gen.place(new FeaturePlaceContext<>(Optional.empty(), level, ((ServerChunkCache) level.getChunkSource()).getGenerator(), random, pos, FeatureConfiguration.NONE));
			}
		}
		return false;
	}
}
