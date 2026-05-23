package forestry.core.recipes.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;

public abstract class ForestryRecipeCategory<T> implements IRecipeCategory<T> {
	protected final IDrawable background;
	private final String localizedName;

	public ForestryRecipeCategory(IDrawable background, String unlocalizedName) {
		this.background = background;
		this.localizedName = Component.translatable(unlocalizedName).getString();
	}

	@Override
	public Component getTitle() {
		return Component.translatable(localizedName);
	}

	@Override
	public int getWidth() {
		return this.background.getWidth();
	}

	@Override
	public int getHeight() {
		return this.background.getHeight();
	}

	// Replaces the deprecated getBackground() auto-render path: registering the
	// background as a recipe-extras drawable lets JEI 19 paint it for us before
	// each subclass's own draw() runs (so existing draw() overrides still work).
	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, T recipe, IFocusGroup focuses) {
		builder.addDrawable(this.background, 0, 0);
	}

	@Override
	abstract public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses);
}
