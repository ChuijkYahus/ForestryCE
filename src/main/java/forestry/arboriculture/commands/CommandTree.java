package forestry.arboriculture.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import com.mojang.brigadier.builder.ArgumentBuilder;

import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.core.platform.commands.CommandSaveStats;
import forestry.core.platform.commands.GiveSpeciesCommand;
import forestry.core.platform.commands.IStatsSaveHelper;
import forestry.core.platform.commands.ModifyGenomeCommand;
import forestry.core.platform.util.SpeciesUtil;

public class CommandTree {
	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		IStatsSaveHelper saveHelper = new TreeStatsSaveHelper();
		ITreeSpeciesType type = SpeciesUtil.TREE_TYPE.get();

		return Commands.literal("tree")
			.then(CommandTreeSpawn.register("spawnTree", new TreeSpawner()))
			.then(CommandTreeSpawn.register("spawnForest", new ForestSpawner()))
			.then(CommandSaveStats.register(saveHelper))
			.then(GiveSpeciesCommand.register(type))
			.then(ModifyGenomeCommand.register(type));
	}
}
