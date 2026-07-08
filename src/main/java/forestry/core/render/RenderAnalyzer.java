package forestry.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import forestry.api.ForestryConstants;
import forestry.core.blocks.BlockBase;
import forestry.core.config.Constants;
import forestry.core.tiles.TileAnalyzer;
import forestry.core.utils.RenderUtil;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

// todo replace with static block model and just render the item
public class RenderAnalyzer implements BlockEntityRenderer<TileAnalyzer> {

	private final ItemRenderer itemRenderer;

	public RenderAnalyzer(BlockEntityRendererProvider.Context ctx) {
		this.itemRenderer = ctx.getItemRenderer();
	}

	@Override
	public void render(TileAnalyzer analyzer, float partialTick, PoseStack stack, MultiBufferSource buffers, int light, int overlay) {
		ItemStack displayStack = analyzer.getIndividualOnDisplay();
		if (!displayStack.isEmpty()) {
			stack.pushPose();
			stack.translate(0.5f, 0.2f, 0.5f);

			RenderUtil.renderDisplayStack(stack, this.itemRenderer, displayStack, analyzer.getLevel(), partialTick, buffers, light);

			stack.popPose();
		}
	}
}
