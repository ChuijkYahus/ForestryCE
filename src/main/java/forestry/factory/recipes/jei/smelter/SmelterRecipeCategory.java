package forestry.factory.recipes.jei.smelter;

import forestry.api.ForestryConstants;
import forestry.api.recipes.ISmelterRecipe;
import forestry.api.recipes.ISqueezerRecipe;
import forestry.core.config.Constants;
import forestry.core.recipes.IngredientStack;
import forestry.core.recipes.jei.ChanceTooltipCallback;
import forestry.core.recipes.jei.ForestryRecipeCategory;
import forestry.core.recipes.jei.ForestryRecipeType;
import forestry.core.utils.JeiUtil;
import forestry.factory.blocks.BlockTypeFactoryPlain;
import forestry.factory.features.FactoryBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

//TODO: All of this
public class SmelterRecipeCategory extends ForestryRecipeCategory<ISmelterRecipe> {
	private static final ResourceLocation TEXTURE = ForestryConstants.forestry(Constants.TEXTURE_PATH_GUI + "/smelter.png");

	private final IDrawableAnimated arrow;
	private final IDrawable icon;
	private final IDrawable fire;

	public SmelterRecipeCategory(IGuiHelper guiHelper) {
		super(guiHelper.createDrawable(TEXTURE, 9, 16, 158, 62), "block.forestry.smelter");

		IDrawableStatic arrowDrawable = guiHelper.createDrawable(TEXTURE, 176, 52, 50, 16);
		this.arrow = guiHelper.createAnimatedDrawable(arrowDrawable, 200, IDrawableAnimated.StartDirection.LEFT, false);

		this.fire = guiHelper.createDrawable(TEXTURE, 176, 68, 14, 14);

		ItemStack smelter = new ItemStack(FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.SMELTER).block());
		this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, smelter);

	}

	@Override
	public RecipeType<ISmelterRecipe> getRecipeType() {
		return ForestryRecipeType.SMELTER;
	}

	@Override
	public IDrawable getIcon() {
		return this.icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ISmelterRecipe recipe, IFocusGroup focuses) {

		int cellX = 0;
		int cellY = 0;

		int oX = 12;
		int oY = 5;

		for (IngredientStack s: recipe.getInputs()){

			builder.addSlot(RecipeIngredientRole.INPUT, oX + cellX, oY + cellY).addItemStacks(
				Arrays.stream(s.getIngredient().getItems()).map(i -> i.copyWithCount(s.getCount())).toList()
			);

			cellX += 18;
			if (cellX >= 54){
				cellX = 0;
				cellY += 18;
			}

		}

		builder.addSlot(RecipeIngredientRole.OUTPUT, 130, 23)
			.addItemStack(recipe.getOutput());

	}

	@Override
	public void draw(ISmelterRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
		this.arrow.draw(graphics, 72, 23);
		this.fire.draw(graphics, 87, 42);
	}
}
