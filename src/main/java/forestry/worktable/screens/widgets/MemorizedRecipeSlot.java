package forestry.worktable.screens.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import forestry.api.client.ForestrySprites;
import forestry.api.client.IForestryClientApi;
import forestry.core.platform.gui.widgets.ItemStackWidgetBase;
import forestry.core.platform.gui.widgets.WidgetManager;
import forestry.core.platform.util.SoundUtil;
import forestry.worktable.recipes.RecipeMemory;
import forestry.worktable.screens.WorktableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemStack;

public class MemorizedRecipeSlot extends ItemStackWidgetBase {
	private final RecipeMemory memory;
	private final int slotIndex;

	public MemorizedRecipeSlot(WidgetManager manager, int xPos, int yPos, RecipeMemory memory, int slotIndex) {
		super(manager, xPos, yPos);
		this.memory = memory;
		this.slotIndex = slotIndex;
	}

	@Override
	protected ItemStack getItemStack() {
		return this.memory.getRecipeDisplayOutput(Minecraft.getInstance().level, this.slotIndex);
	}

	@Override
	public void draw(GuiGraphics graphics, int startX, int startY) {
		super.draw(graphics, startX, startY);

		RenderSystem.disableDepthTest();

		if (this.memory.isLocked(this.slotIndex)) {
			RenderSystem.setShaderTexture(0, ForestrySprites.TEXTURE_ATLAS);
			TextureAtlasSprite lockedSprite = IForestryClientApi.INSTANCE.getTextureManager().getSprite(ForestrySprites.SLOT_LOCKED);
			graphics.blit(startX + this.xPos, startY + this.yPos, 0, 16, 16, lockedSprite);
		}

		RenderSystem.enableDepthTest();
	}

	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton) {
		if (!getItemStack().isEmpty()) {
			WorktableMenu.sendRecipeClick(mouseButton, this.slotIndex);
			SoundUtil.playButtonClick();
		}
	}
}
