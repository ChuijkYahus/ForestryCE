package forestry.core.platform.render;

import com.mojang.blaze3d.vertex.PoseStack;
import forestry.core.content.escritoire.TileEscritoire;
import forestry.core.platform.util.RenderUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

public class RenderEscritoire implements BlockEntityRenderer<TileEscritoire> {

	private final ItemRenderer itemRenderer;

	public RenderEscritoire(BlockEntityRendererProvider.Context ctx) {
		this.itemRenderer = ctx.getItemRenderer();
	}

	@Override
	public void render(TileEscritoire escritoire, float partialTick, PoseStack stack, MultiBufferSource buffers, int light, int overlay) {
		ItemStack displayStack = escritoire.getIndividualOnDisplay();
		if (!displayStack.isEmpty()) {
			stack.pushPose();
			stack.translate(0.5, 0.9, 0.5);
			stack.scale(0.75f, 0.75f, 0.75f);
			RenderUtil.renderDisplayStack(stack, this.itemRenderer, displayStack, escritoire.getLevel(), partialTick, buffers, light);
			stack.popPose();
		}
	}
}
