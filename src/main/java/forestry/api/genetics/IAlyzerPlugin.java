package forestry.api.genetics;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Used to display information in the Portable Analyzer.
 */
public interface IAlyzerPlugin {
	void drawAnalyticsPage1(GuiGraphics graphics, Screen gui, ItemStack stack);

	void drawAnalyticsPage2(GuiGraphics graphics, Screen gui, ItemStack stack);

	void drawAnalyticsPage3(GuiGraphics graphics, Screen gui, ItemStack stack);

	/**
	 * The hints that will be shown in the alyzer gui.
	 */
	List<String> getHints();

	/**
	 * @return Icon stacks used by this plugin for rendering.
	 */
	Map<ISpecies<?>, ItemStack> getIconStacks();
}
