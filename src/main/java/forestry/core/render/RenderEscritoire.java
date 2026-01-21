package forestry.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import forestry.api.ForestryConstants;
import forestry.core.blocks.BlockBase;
import forestry.core.config.Constants;
import forestry.core.tiles.TileEscritoire;
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
import net.minecraft.world.item.ItemStack;

public class RenderEscritoire implements BlockEntityRenderer<TileEscritoire> {
	private static final ResourceLocation TEXTURE = ForestryConstants.forestry(Constants.TEXTURE_PATH_BLOCK + "/escritoire.png");

	private final ItemRenderer itemRenderer;
	private final ModelPart root;

	public RenderEscritoire(BlockEntityRendererProvider.Context ctx) {
		this.itemRenderer = ctx.getItemRenderer();
		this.root = ctx.bakeLayer(ForestryModelLayers.ESCRITOIRE_LAYER);
	}

	//TODO: Figure out if I need this? I'm sorta in a world I don't understand with this.
	public static LayerDefinition createBodyLayer() {
		return LayerDefinition.create(new MeshDefinition(), 64, 64);
	}

	@Override
	public void render(TileEscritoire escritoire, float partialTick, PoseStack stack, MultiBufferSource buffers, int light, int overlay) {

		stack.pushPose();
		//Direction facing = escritoire.getBlockState().getValue(BlockBase.FACING);
		//RenderUtil.rotateByHorizontalDirection(stack, facing);
		//VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));

		//this.root.render(stack, buffer, light, overlay);

		ItemStack displayStack = escritoire.getIndividualOnDisplay();
		if (!displayStack.isEmpty()) {
			stack.pushPose();
			stack.translate(0.5, 0.9, 0.5);
			stack.scale(0.75f, 0.75f, 0.75f);
			RenderUtil.renderDisplayStack(stack, this.itemRenderer, displayStack, escritoire.getLevel(), partialTick, buffers, light);
			stack.popPose();
		}
		stack.popPose();
	}
}
