package forestry.core.genetics;

import forestry.Forestry;
import forestry.api.ForestryCapabilities;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.ILifeStage;
import forestry.api.genetics.ISpecies;
import forestry.api.genetics.ISpeciesType;
import forestry.api.genetics.capability.IIndividualHandlerItem;
import forestry.core.config.ForestryConfig;
import forestry.core.features.CoreDataComponents;
import forestry.core.genetics.capability.SerializableIndividualHandlerItem;
import forestry.core.items.ItemForestry;
import forestry.core.utils.GeneticsUtil;
import forestry.core.utils.SpeciesUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.mutable.MutableBoolean;

import javax.annotation.Nullable;
import java.util.List;

public abstract class ItemGE extends ItemForestry {
	protected final ILifeStage stage;

	protected ItemGE(Item.Properties properties, ILifeStage stage) {
		super(properties.setNoRepair());

		this.stage = stage;
	}

	protected abstract ISpecies<?> getSpecies(ItemStack stack);

	protected abstract ISpeciesType<?, ?> getType();

	public IIndividualHandlerItem createIndividualHandler(ItemStack stack) {
		Tag parent = readIndividualTag(stack);

		if (parent == null) {
			return new SerializableIndividualHandlerItem(getType(), stack, getType().getDefaultSpecies().createIndividual(), this.stage);
		}

		return new SerializableIndividualHandlerItem(getType(), stack, SpeciesUtil.deserializeIndividual(getType(), parent), this.stage);
	}

	/**
	 * Reads the serialized individual NBT from a stack, preferring the modern
	 * {@link CoreDataComponents#INDIVIDUAL} component and falling back to the
	 * legacy {@code CUSTOM_DATA -> "ForgeCaps" -> "Parent"} path used by
	 * pre-1.21 worlds and items still in flight from older saves.
	 */
	@Nullable
	public static Tag readIndividualTag(ItemStack stack) {
		CompoundTag individual = stack.get(CoreDataComponents.INDIVIDUAL);
		if (individual != null) {
			return individual;
		}

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null && customData.contains("ForgeCaps")) {
			CompoundTag forgeCaps = customData.copyTag().getCompound("ForgeCaps");
			return forgeCaps.contains("Parent") ? forgeCaps.get("Parent") : null;
		}
		return null;
	}

	public static boolean hasIndividual(ItemStack stack) {
		return stack.has(CoreDataComponents.INDIVIDUAL) || readIndividualTag(stack) != null;
	}

	@Override
	public Component getName(ItemStack stack) {
		IIndividualHandlerItem handler = stack.getCapability(ForestryCapabilities.INDIVIDUAL_HANDLER_ITEM);
		return handler != null ? GeneticsUtil.getItemName(handler.getStage(), handler.getIndividual().getSpecies()) : super.getName(stack);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		if (!hasIndividual(stack)) { // villager trade wildcard bees
			return false;
		}
		ISpecies<?> species = getSpecies(stack);
		return species.hasGlint() && ForestryConfig.CLIENT.enableGlints.get();
	}

	public static void appendGeneticsTooltip(ItemStack stack, List<Component> tooltip) {
		if (!hasIndividual(stack)) {
			return;
		}

		MutableBoolean analyzed = new MutableBoolean();
		IIndividualHandlerItem.ifPresent(stack, individual -> {
			if (individual.isAnalyzed()) {
				if (Screen.hasShiftDown()) {
					((ISpecies<IIndividual>) individual.getSpecies()).addTooltip(individual, tooltip);
				} else {
					tooltip.add(Component.translatable("for.gui.tooltip.tmi", "< %s >").withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));
				}

				analyzed.setTrue();
			}
		});
		if (analyzed.isFalse()) {
			tooltip.add(Component.translatable("for.gui.unknown", "< %s >").withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		appendGeneticsTooltip(stack, tooltip);
	}

	@Override
	public String getCreatorModId(ItemStack stack) {
		ISpecies<?> species = getSpecies(stack);
		return species.id().getNamespace();
	}

	public static <S extends ISpecies<I>, I extends IIndividual> void addCreativeItems(ILifeStage stage, List<ItemStack> subItems, boolean hideSecrets, ISpeciesType<S, I> type) {
		for (S species : type.getAllSpecies()) {
			// Don't show secrets unless ordered to.
			if (hideSecrets && species.isSecret() && !Forestry.DEBUG) {
				continue;
			}

			subItems.add(species.createStack(species.createIndividual(), stage));
		}
	}
}
