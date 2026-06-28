package forestry.core.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.ISpeciesType;
import forestry.api.genetics.alleles.Allele;
import forestry.api.genetics.alleles.AllelePair;
import forestry.api.genetics.alleles.IChromosome;
import forestry.api.genetics.alleles.IKaryotype;
import forestry.api.genetics.ILifeStage;
import forestry.api.genetics.capability.IIndividualHandlerItem;
import forestry.api.plugin.IGenomeBuilder;
import forestry.core.utils.GeneticsUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModifyGenomeCommand {
	private static final DynamicCommandExceptionType ERROR_NO_GENETICS = new DynamicCommandExceptionType(found -> {
		return Component.literal("The following item does not contain genetic data: " + found);
	});

	public static LiteralArgumentBuilder<CommandSourceStack> register(ISpeciesType<?, ?> type) {
		return Commands.literal("modify").requires(CommandHelpers.ADMIN)
			.then(Commands.argument("chromosome", new ChromosomeArgument(type))
				.then(Commands.argument("allele", new AlleleArgument(type))
					.suggests((ctx, builder) -> suggestAlleles(type, ctx, builder))
					.executes(ctx -> ModifyGenomeCommand.execute(type, ctx, true, true))
					.then(Commands.literal("both")
						.executes(ctx -> ModifyGenomeCommand.execute(type, ctx, true, true)))
					.then(Commands.literal("dominant")
						.executes(ctx -> ModifyGenomeCommand.execute(type, ctx, true, false)))
					.then(Commands.literal("recessive")
						.executes(ctx -> ModifyGenomeCommand.execute(type, ctx, false, true)))));
	}

	private static int execute(ISpeciesType<?, ?> type, CommandContext<CommandSourceStack> ctx, boolean active, boolean inactive) throws CommandSyntaxException {
		IKaryotype karyotype = type.getKaryotype();
		IChromosome<?> chromosome = ctx.getArgument("chromosome", IChromosome.class);

		if (karyotype.contains(chromosome)) {
			Allele<?> allele = ctx.getArgument("allele", Allele.class);

			CommandSourceStack source = ctx.getSource();
			ServerPlayer player = source.getPlayerOrException();
			ItemStack stack = player.getMainHandItem();
			IIndividual individual = IIndividualHandlerItem.getIndividual(stack);
			ILifeStage stage = IIndividualHandlerItem.getLifeStage(stack);

			if (individual != null && stage != null) {
				if (individual.getType() != type) {
					throw LifeStageArgument.INVALID_VALUE.create(individual.getClass().getSimpleName());
				}

				IGenome oldGenome = individual.getGenome();
				IGenomeBuilder builder = karyotype.createGenomeBuilder();

				for (Map.Entry<IChromosome<?>, AllelePair<?>> entry : oldGenome.getChromosomes().entrySet()) {
					IChromosome<?> key = entry.getKey();
					AllelePair<?> pair = entry.getValue();
					if (key == chromosome) {
						pair = replaceAllele(key, pair, allele, active, inactive);
					}

					builder.setUnchecked(key, pair);
				}

				IGenome newGenome = builder.build();
				IIndividual newIndividual = individual.copyWithGenome(newGenome);
				newIndividual.analyze();
				ItemStack newStack = newIndividual.createStack(stage);
				newStack.setCount(stack.getCount());
				player.setItemInHand(InteractionHand.MAIN_HAND, newStack);
				source.sendSuccess(() -> Component.literal("Modified genome of ").append(newStack.getDisplayName()), true);

				return 1;
			} else {
				throw ERROR_NO_GENETICS.create(stack.getDisplayName().getString());
			}
		} else {
			throw LifeStageArgument.INVALID_VALUE.create(chromosome.id().toString());
		}
	}

	// Replaces the active and/or inactive allele of a chromosome's pair. For reference chromosomes the parsed allele
	// only carries the referenced id (see AlleleArgument), so its intrinsic dominance is resolved here, mirroring
	// IGenome#copyWith.
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static AllelePair<?> replaceAllele(IChromosome<?> chromosome, AllelePair<?> pair, Allele<?> allele, boolean active, boolean inactive) {
		Allele replacement = allele;
		IChromosome.IReferenceResolver<?> resolver = chromosome.resolver();
		if (resolver != null) {
			ResourceLocation id = (ResourceLocation) allele.value();
			replacement = new Allele<>(id, resolver.isDominant(id));
		}

		Allele newActive = active ? replacement : pair.active();
		Allele newInactive = inactive ? replacement : pair.inactive();
		return new AllelePair(newActive, newInactive);
	}

	private static CompletableFuture<Suggestions> suggestAlleles(ISpeciesType<?, ?> type, CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		IChromosome<?> chromosome = ctx.getArgument("chromosome", IChromosome.class);
		// Alleles have no id anymore; only reference chromosomes (value = ResourceLocation) can be suggested as resources.
		return SharedSuggestionProvider.suggestResource(GeneticsUtil.getKnownAlleles(chromosome).stream()
			.map(Allele::value)
			.filter(value -> value instanceof ResourceLocation)
			.map(value -> (ResourceLocation) value), builder);
	}
}
