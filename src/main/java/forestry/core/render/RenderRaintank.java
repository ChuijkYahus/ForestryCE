package forestry.core.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import forestry.Forestry;
import forestry.core.tiles.TileEscritoire;
import forestry.factory.tiles.TileRaintank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

import java.util.ArrayList;
import java.util.List;

public class RenderRaintank implements BlockEntityRenderer<TileRaintank> {
	private final ModelPart root;
	public RenderRaintank(BlockEntityRendererProvider.Context ctx) {
		//I don't actually know if this needs to do anything?
		this.root = ctx.bakeLayer(ForestryModelLayers.RAINTANK_LAYER);
	}

	public static LayerDefinition createBodyLayer() {
		return LayerDefinition.create(new MeshDefinition(), 16, 16);
	}
	@Override
	public void render(TileRaintank tileRaintank, float v, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int i1) {

		FluidStack fluid = tileRaintank.getTankManager().getFluid(0);

		//Render a layer of water so long as there is fluid in the tank
		if (fluid != null && !fluid.isEmpty()) {
			int maxAmount = tileRaintank.getTankManager().getTankCapacity(0);
			int fillAmount = fluid.getAmount();

			float fillRatio = (float)fillAmount/maxAmount; //The percentage of how 'full' the container is
			fillRatio = Mth.clamp(fillRatio, 0f, 1f);
			float top = 15f/16f; //The pixel where the container is at its fullest.
			float bottom = 2f/16f; //The pixel where the container is empty.
			float height = ((top-bottom)*fillRatio)+bottom; //The height to draw the water level at

			IClientFluidTypeExtensions fluidAttributes = IClientFluidTypeExtensions.of(fluid.getFluid());
			ResourceLocation fluidStill = fluidAttributes.getStillTexture(fluid);

			if (fluidStill != null) {
				TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidStill);
				VertexConsumer vc = multiBufferSource.getBuffer(RenderType.translucent());

				poseStack.pushPose();
				PoseStack.Pose pose = poseStack.last();

				int light = LevelRenderer.getLightColor(tileRaintank.getLevel(), tileRaintank.getBlockPos().above());
				int waterColour = tileRaintank.getLevel().getBiome(tileRaintank.getBlockPos()).get().getWaterColor();

				float red = (waterColour >> 16 & 0xFF) / 255f;
				float green = (waterColour >> 8 & 0xFF) / 255f;
				float blue = (waterColour & 0xFF) / 255f;
				float alpha = 180 / 255f;

				//These values determine the x and z the water renders from.
				float minCorner = 2f/16f;
				float maxCorner = 14f/16f;

				vc.vertex(pose.pose(), minCorner, height, minCorner)
					.color(red, green, blue, alpha)
					.uv(sprite.getU0(), sprite.getV0())
					.uv2(light)
					.normal(pose.normal(), 0, 1, 0)
					.endVertex();

				vc.vertex(pose.pose(), minCorner, height, maxCorner)
					.color(red, green, blue, alpha)
					.uv(sprite.getU0(), sprite.getV1())
					.uv2(light)
					.normal(pose.normal(), 0, 1, 0)
					.endVertex();

				vc.vertex(pose.pose(), maxCorner, height, maxCorner)
					.color(red, green, blue, alpha)
					.uv(sprite.getU1(), sprite.getV1())
					.uv2(light)
					.normal(pose.normal(), 0, 1, 0)
					.endVertex();

				vc.vertex(pose.pose(), maxCorner, height, minCorner)
					.color(red, green, blue, alpha)
					.uv(sprite.getU1(), sprite.getV0())
					.uv2(light)
					.normal(pose.normal(), 0, 1, 0)
					.endVertex();

				poseStack.popPose();
			}
		}
	}
}
